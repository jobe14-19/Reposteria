package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardEmpleadoController {

    private static final Logger LOGGER = Logger.getLogger(DashboardEmpleadoController.class.getName());

    // Constantes SQL
    private static final String SQL_PENDIENTES_HOY =
            "SELECT COUNT(*) as pendientes FROM pedidos WHERE estado = 'Confirmado' AND CAST(fecha_entrega AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)";
    private static final String SQL_URGENTES_HOY =
            "SELECT COUNT(*) as urgententes FROM pedidos WHERE estado = 'Confirmado' AND prioridad = 'ALTA' AND CAST(fecha_entrega AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)";
    private static final String SQL_PARA_HOY =
            "SELECT COUNT(*) as para_hoy FROM pedidos WHERE CAST(fecha_entrega AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)";
    private static final String SQL_PRODUCCION_ACTIVA =
            "SELECT id_produccion, producto, cantidad, progreso, estado FROM produccion WHERE estado IN ('En Progreso', 'Pendiente') ORDER BY id_produccion DESC LIMIT 5";
    private static final String SQL_STOCK_CRITICO =
            "SELECT ingrediente, stock_actual, stock_minimo, (stock_minimo - stock_actual) as diferencia, CASE WHEN stock_actual < stock_minimo THEN 'CRÍTICO' WHEN stock_actual < stock_minimo * 1.2 THEN 'BAJO' ELSE 'OK' END as urgencia FROM inventario WHERE stock_actual < stock_minimo * 1.5 ORDER BY (stock_minimo - stock_actual) DESC";
    private static final String SQL_ENTREGAS_HOY =
            "SELECT FORMAT(hora_entrega, 'HH:mm') as hora, cliente, direccion, producto, estado FROM entregas WHERE CAST(fecha_entrega AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE) ORDER BY hora_entrega ASC LIMIT 10";

    // Constantes
    private static final int REFRESH_INTERVAL_MS = 30000;
    private static final String COLOR_PRIMARIO = "#8B5E3C";
    private static final String COLOR_FONDO_NORMAL = "#F5F5F5";
    private static final String COLOR_FONDO_FIN_SEMANA = "#FAFAFA";
    private static final String COLOR_TEXTO_NORMAL = "#666666";
    private static final String COLOR_TEXTO_CLARO = "#999999";

    // UI Components
    @FXML private Label userLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label pendientesHoyLabel;
    @FXML private Label urgentesHoyLabel;
    @FXML private Label paraHoyLabel;
    @FXML private ListView<String> pedidosRapidosList;
    @FXML private TableView<Produccion> produccionTable;
    @FXML private TableColumn<Produccion, Integer> prodIdColumn;
    @FXML private TableColumn<Produccion, String> prodProductoColumn;
    @FXML private TableColumn<Produccion, Integer> prodCantidadColumn;
    @FXML private TableColumn<Produccion, Double> prodProgresoColumn;
    @FXML private TableColumn<Produccion, String> prodEstadoColumn;
    @FXML private TableView<StockCritico> stockCriticoTable;
    @FXML private TableColumn<StockCritico, String> alertIngredienteColumn;
    @FXML private TableColumn<StockCritico, Double> alertActualColumn;
    @FXML private TableColumn<StockCritico, Double> alertMinimoColumn;
    @FXML private TableColumn<StockCritico, Double> alertDiferenciaColumn;
    @FXML private TableColumn<StockCritico, String> alertUrgenciaColumn;
    @FXML private TableView<Entrega> entregasTable;
    @FXML private TableColumn<Entrega, String> entHoraColumn;
    @FXML private TableColumn<Entrega, String> entClienteColumn;
    @FXML private TableColumn<Entrega, String> entDireccionColumn;
    @FXML private TableColumn<Entrega, String> entProductoColumn;
    @FXML private TableColumn<Entrega, String> entEstadoColumn;
    @FXML private GridPane calendarGrid;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Timer refreshTimer;
    private DateTimeFormatter timeFormatter;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        configurarTablas();
        cargarDatosEmpleado();
        iniciarAutoRefresh();
        actualizarInfoUsuario();
        actualizarTimestamp();
        inicializarCalendario();
    }

    private void configurarTablas() {
        prodIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        prodProductoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        prodCantidadColumn.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        prodProgresoColumn.setCellValueFactory(new PropertyValueFactory<>("progreso"));
        prodEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        alertIngredienteColumn.setCellValueFactory(new PropertyValueFactory<>("ingrediente"));
        alertActualColumn.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        alertMinimoColumn.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        alertDiferenciaColumn.setCellValueFactory(new PropertyValueFactory<>("diferencia"));
        alertUrgenciaColumn.setCellValueFactory(new PropertyValueFactory<>("urgencia"));
        entHoraColumn.setCellValueFactory(new PropertyValueFactory<>("hora"));
        entClienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        entDireccionColumn.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        entProductoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        entEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void cargarDatosEmpleado() {
        if (!sessionManager.isLoggedIn()) return;
        try (Connection conn = dbConnection.getConnection()) {
            cargarKPIs(conn);
            cargarProduccion(conn);
            cargarStockCritico(conn);
            cargarEntregasHoy(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error: {0}", e.getMessage());
            mostrarError("Error", "No se pudieron cargar los datos.");
        }
    }

    private void cargarKPIs(Connection conn) {
        ejecutarConsulta(conn, SQL_PENDIENTES_HOY, "pendientes", pendientesHoyLabel);
        ejecutarConsulta(conn, SQL_URGENTES_HOY, "urgententes", urgentesHoyLabel);
        ejecutarConsulta(conn, SQL_PARA_HOY, "para_hoy", paraHoyLabel);
    }

    private void ejecutarConsulta(Connection conn, String sql, String columna, Label label) {
        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) label.setText(String.valueOf(rs.getInt(columna)));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage());
        }
    }

    private void cargarProduccion(Connection conn) throws SQLException {
        ObservableList<Produccion> list = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCCION_ACTIVA); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Produccion(rs.getInt("id_produccion"), rs.getString("producto"), rs.getInt("cantidad"), rs.getDouble("progreso"), rs.getString("estado")));
            }
        }
        produccionTable.setItems(list);
    }

    private void cargarStockCritico(Connection conn) throws SQLException {
        ObservableList<StockCritico> list = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_STOCK_CRITICO); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new StockCritico(rs.getString("ingrediente"), rs.getDouble("stock_actual"), rs.getDouble("stock_minimo"), rs.getDouble("diferencia"), rs.getString("urgencia")));
            }
        }
        stockCriticoTable.setItems(list);
    }

    private void cargarEntregasHoy(Connection conn) throws SQLException {
        ObservableList<Entrega> list = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ENTREGAS_HOY); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Entrega(rs.getString("hora"), rs.getString("cliente"), rs.getString("direccion"), rs.getString("producto"), rs.getString("estado")));
            }
        }
        entregasTable.setItems(list);
    }

    private void inicializarCalendario() {
        calendarGrid.getChildren().clear();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        String[] dias = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
        for (int i = 0; i < dias.length; i++) {
            Label header = new Label(dias[i]);
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARIO + "; -fx-font-size: 12px;");
            calendarGrid.add(header, i, 0);
        }
        LocalDate firstDay = currentMonth.atDay(1);
        int startDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            int row = (startDayOfWeek + day - 1) / 7 + 1;
            int col = (startDayOfWeek + day - 1) % 7;
            VBox dayBox = crearDayBox(day, currentMonth, today);
            calendarGrid.add(dayBox, col, row);
        }
    }

    private VBox crearDayBox(int day, YearMonth currentMonth, LocalDate today) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(5));
        Label label = new Label(String.valueOf(day));
        label.setStyle("-fx-font-size: 12px;");
        LocalDate currentDay = currentMonth.atDay(day);
        if (currentDay.equals(today)) {
            box.setStyle("-fx-background-color: " + COLOR_PRIMARIO + "; -fx-background-radius: 8;");
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        } else if (currentDay.getDayOfWeek() == DayOfWeek.SATURDAY || currentDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            box.setStyle("-fx-background-color: " + COLOR_FONDO_FIN_SEMANA + ";");
            label.setStyle("-fx-text-fill: " + COLOR_TEXTO_CLARO + ";");
        } else {
            box.setStyle("-fx-background-color: " + COLOR_FONDO_NORMAL + ";");
            label.setStyle("-fx-text-fill: " + COLOR_TEXTO_NORMAL + ";");
        }
        box.getChildren().add(label);
        return box;
    }

    private void iniciarAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    cargarDatosEmpleado();
                    actualizarTimestamp();
                });
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
    }

    private void actualizarInfoUsuario() {
        if (sessionManager.isLoggedIn()) userLabel.setText("👤 " + sessionManager.getUsuarioActual() + " (EMPLEADO)");
    }

    private void actualizarTimestamp() {
        lastUpdateLabel.setText("Última actualización: " + LocalDateTime.now().format(timeFormatter));
    }

    @FXML private void cerrarSesion(ActionEvent e) {
        if (refreshTimer != null) refreshTimer.cancel();
        sessionManager.cerrarSesion();
        try {
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("Login.fxml")), 1280, 720));
            stage.setTitle("🍰 Pastelería Rosato - Sistema de Gestión");
        } catch (Exception ex) { LOGGER.log(Level.SEVERE, "Error: {0}", ex.getMessage()); }
    }

    @FXML private void mostrarDashboard(ActionEvent e) { cargarDatosEmpleado(); actualizarTimestamp(); }
    @FXML private void mostrarProduccion(ActionEvent e) { mostrarMensaje("Producción", "Módulo en desarrollo"); }
    @FXML private void mostrarEntregas(ActionEvent e) { mostrarMensaje("Entregas", "Módulo en desarrollo"); }
    @FXML private void mostrarInventario(ActionEvent e) { mostrarMensaje("Inventario", "Módulo en desarrollo"); }
    @FXML private void mostrarHigiene(ActionEvent e) { mostrarMensaje("Higiene", "Módulo en desarrollo"); }

    @FXML
    private void verMiPerfil(ActionEvent e) {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("MiPerfil.fxml")), 800, 600));
            stage.setTitle("🍰 Pastelería Rosato - Mi Perfil");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception ex) { mostrarError("Error", "No se pudo abrir perfil"); }
    }

    private void mostrarError(String t, String m) { mostrarAlerta(Alert.AlertType.ERROR, t, m); }
    private void mostrarMensaje(String t, String m) { mostrarAlerta(Alert.AlertType.INFORMATION, t, m); }
    private void mostrarAlerta(Alert.AlertType tipo, String t, String m) {
        Alert a = new Alert(tipo); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    public static class Produccion {
        private int id; private String producto; private int cantidad; private double progreso; private String estado;
        public Produccion(int id, String producto, int cantidad, double progreso, String estado) {
            this.id = id; this.producto = producto; this.cantidad = cantidad; this.progreso = progreso; this.estado = estado;
        }
        public int getId() { return id; }
        public String getProducto() { return producto; }
        public int getCantidad() { return cantidad; }
        public double getProgreso() { return progreso; }
        public String getEstado() { return estado; }
    }

    public static class StockCritico {
        private String ingrediente; private double stockActual; private double stockMinimo; private double diferencia; private String urgencia;
        public StockCritico(String i, double sa, double sm, double d, String u) {
            this.ingrediente = i; this.stockActual = sa; this.stockMinimo = sm; this.diferencia = d; this.urgencia = u;
        }
        public String getIngrediente() { return ingrediente; }
        public double getStockActual() { return stockActual; }
        public double getStockMinimo() { return stockMinimo; }
        public double getDiferencia() { return diferencia; }
        public String getUrgencia() { return urgencia; }
    }

    public static class Entrega {
        private String hora; private String cliente; private String direccion; private String producto; private String estado;
        public Entrega(String h, String c, String d, String p, String e) {
            this.hora = h; this.cliente = c; this.direccion = d; this.producto = p; this.estado = e;
        }
        public String getHora() { return hora; }
        public String getCliente() { return cliente; }
        public String getDireccion() { return direccion; }
        public String getProducto() { return producto; }
        public String getEstado() { return estado; }
    }
}