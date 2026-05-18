package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardAdminController {

    private static final Logger LOGGER = Logger.getLogger(DashboardAdminController.class.getName());

    private static final String SQL_PEDIDOS_HOY =
            "SELECT COUNT(*) as pedidos_hoy FROM pedidos WHERE CAST(fecha_creacion AS DATE) = CAST(GETDATE() AS DATE)";
    private static final String SQL_INGRESOS_MES =
            "SELECT COALESCE(SUM(total), 0) as ingresos_mes FROM pedidos WHERE MONTH(fecha_creacion) = MONTH(GETDATE()) AND YEAR(fecha_creacion) = YEAR(GETDATE())";
    private static final String SQL_SALDO_PENDIENTE =
            "SELECT COALESCE(SUM(CASE WHEN estado = 'Pendiente' THEN total ELSE 0 END), 0) as saldo_pendiente FROM pedidos";
    private static final String SQL_MAQUINAS_MANTENIMIENTO =
            "SELECT COUNT(*) as maquinas_mantenimiento FROM equipos WHERE estado = 'Mantenimiento'";
    private static final String SQL_VENTAS_ULTIMOS_7_DIAS =
            "SELECT CONVERT(VARCHAR, CAST(fecha_creacion AS DATE), 103) as dia, SUM(total) as ventas_dia FROM pedidos WHERE fecha_creacion >= DATEADD(day, -6, GETDATE()) GROUP BY CAST(fecha_creacion AS DATE) ORDER BY CAST(fecha_creacion AS DATE)";
    private static final String SQL_PRODUCTOS_MAS_VENDIDOS =
            "SELECT TOP 10 p.nombre as producto, COUNT(*) as unidades_vendidas FROM detalles_pedido dp INNER JOIN productos p ON dp.id_producto = p.id_producto INNER JOIN pedidos pe ON dp.id_pedido = pe.id_pedido WHERE pe.fecha_creacion >= DATEADD(month, -1, GETDATE()) GROUP BY p.nombre ORDER BY unidades_vendidas DESC";
    private static final String SQL_ALERTAS =
            "SELECT TOP 10 'Inventario Crítico' as tipo, i.ingrediente + ' - Stock: ' + CAST(i.stock_actual AS VARCHAR) + ' (Mínimo: ' + CAST(i.stock_minimo AS VARCHAR) + ')' as descripcion, FORMAT(GETDATE(), 'yyyy-MM-dd HH:mm') as fecha_hora, CASE WHEN i.stock_actual < i.stock_minimo THEN 'ACTIVO' ELSE 'RESUELTO' END as estado FROM inventario i WHERE i.stock_actual < i.stock_minimo * 1.2 UNION ALL SELECT TOP 5 'Entrega Atrasada' as tipo, 'Pedido ' + CAST(pe.id_pedido AS VARCHAR) + ' para ' + c.nombre + ' - ' + FORMAT(pe.fecha_entrega, 'HH:mm') + 'hrs' as descripcion, FORMAT(pe.fecha_entrega, 'yyyy-MM-dd HH:mm') as fecha_hora, 'ATRASADO' as estado FROM pedidos pe INNER JOIN clientes c ON pe.id_cliente = c.id_cliente INNER JOIN productos p ON pe.id_producto = p.id_producto WHERE pe.estado = 'Atrasado' AND pe.fecha_entrega < GETDATE() ORDER BY fecha_hora DESC";
    private static final String SQL_ACTIVIDADES_RECIENTES =
            "SELECT TOP 20 FORMAT(a.fecha_hora, 'yyyy-MM-dd HH:mm') as fecha_hora, a.usuario as usuario, a.accion as accion, a.detalle as detalle FROM actividad a ORDER BY a.fecha_hora DESC";

    private static final int REFRESH_INTERVAL_MS = 30000;

    @FXML private Label userLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label pedidosHoyLabel;
    @FXML private Label ingresosMesLabel;
    @FXML private Label saldoPendienteLabel;
    @FXML private Label mantenimientoLabel;
    @FXML private LineChart<String, Number> salesChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private BarChart<String, Number> productsChart;
    @FXML private CategoryAxis productsXAxis;
    @FXML private NumberAxis productsYAxis;
    @FXML private TableView<Alerta> alertasTable;
    @FXML private TableColumn<Alerta, String> alertTipoColumn;
    @FXML private TableColumn<Alerta, String> alertDescripcionColumn;
    @FXML private TableColumn<Alerta, String> alertFechaColumn;
    @FXML private TableColumn<Alerta, String> alertEstadoColumn;
    @FXML private TableView<Actividad> actividadesTable;
    @FXML private TableColumn<Actividad, String> actFechaColumn;
    @FXML private TableColumn<Actividad, String> actUsuarioColumn;
    @FXML private TableColumn<Actividad, String> actAccionColumn;
    @FXML private TableColumn<Actividad, String> actDetalleColumn;

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
        cargarDatosAdministrador();
        iniciarAutoRefresh();
        actualizarInfoUsuario();
        actualizarTimestamp();
    }

    private void configurarTablas() {
        alertTipoColumn.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        alertDescripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        alertFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        alertEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        actFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        actUsuarioColumn.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        actAccionColumn.setCellValueFactory(new PropertyValueFactory<>("accion"));
        actDetalleColumn.setCellValueFactory(new PropertyValueFactory<>("detalle"));
    }

    private void cargarDatosAdministrador() {
        if (!sessionManager.isLoggedIn()) return;

        try (Connection conn = dbConnection.getConnection()) {
            cargarKPIs(conn);
            cargarGraficoVentas(conn);
            cargarGraficoProductos(conn);
            cargarAlertas(conn);
            cargarActividades(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: usando datos de demostración");
            pedidosHoyLabel.setText("--");
            ingresosMesLabel.setText("--");
            saldoPendienteLabel.setText("--");
            mantenimientoLabel.setText("--");
            salesChart.setData(FXCollections.observableArrayList());
            productsChart.setData(FXCollections.observableArrayList());
            alertasTable.setItems(FXCollections.observableArrayList());
            actividadesTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void cargarKPIs(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_HOY); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) pedidosHoyLabel.setText(String.valueOf(rs.getInt("pedidos_hoy")));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage()); }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INGRESOS_MES); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) ingresosMesLabel.setText(String.format("$%.2f", rs.getDouble("ingresos_mes")));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage()); }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_SALDO_PENDIENTE); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) saldoPendienteLabel.setText(String.format("$%.2f", rs.getDouble("saldo_pendiente")));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage()); }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_MAQUINAS_MANTENIMIENTO); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) mantenimientoLabel.setText(String.valueOf(rs.getInt("maquinas_mantenimiento")));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage()); }
    }

    private void cargarGraficoVentas(Connection conn) {
        ObservableList<XYChart.Data<String, Number>> salesData = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_VENTAS_ULTIMOS_7_DIAS); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                salesData.add(new XYChart.Data<>(rs.getString("dia"), rs.getDouble("ventas_dia")));
            }
            XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
            salesSeries.setData(salesData);
            salesChart.setData(FXCollections.observableArrayList(salesSeries));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage());
            salesChart.setData(FXCollections.observableArrayList());
        }
    }

    private void cargarGraficoProductos(Connection conn) {
        ObservableList<XYChart.Data<String, Number>> productsData = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTOS_MAS_VENDIDOS); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int unidades = rs.getInt("unidades_vendidas");
                if (unidades > 0) {
                    productsData.add(new XYChart.Data<>(rs.getString("producto"), unidades));
                }
            }
            XYChart.Series<String, Number> productsSeries = new XYChart.Series<>();
            productsSeries.setData(productsData);
            productsChart.setData(FXCollections.observableArrayList(productsSeries));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage());
            productsChart.setData(FXCollections.observableArrayList());
        }
    }

    private void cargarAlertas(Connection conn) {
        ObservableList<Alerta> alertas = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ALERTAS); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                alertas.add(new Alerta(rs.getString("tipo"), rs.getString("descripcion"), rs.getString("fecha_hora"), rs.getString("estado")));
            }
            alertasTable.setItems(alertas);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage());
        }
    }

    private void cargarActividades(Connection conn) {
        ObservableList<Actividad> actividades = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTIVIDADES_RECIENTES); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                actividades.add(new Actividad(rs.getString("fecha_hora"), rs.getString("usuario"), rs.getString("accion"), rs.getString("detalle")));
            }
            actividadesTable.setItems(actividades);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error: {0}", e.getMessage());
        }
    }

    private void iniciarAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    cargarDatosAdministrador();
                    actualizarTimestamp();
                });
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
    }

    private void actualizarInfoUsuario() {
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("👤 " + sessionManager.getUsuarioActual() + " (ADMINISTRADOR)");
        }
    }

    private void actualizarTimestamp() {
        lastUpdateLabel.setText("Última actualización: " + LocalDateTime.now().format(timeFormatter));
    }

    @FXML private void cerrarSesion(ActionEvent event) {
        if (refreshTimer != null) refreshTimer.cancel();
        sessionManager.cerrarSesion();
        try {
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/example/demo/Login.fxml")), 1280, 720));
            stage.setTitle("🍰 Repostería Rosato - Sistema de Gestión");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: {0}", e.getMessage());
        }
    }

    @FXML private void mostrarDashboard(ActionEvent event) { cargarDatosAdministrador(); actualizarTimestamp(); }
    @FXML private void mostrarClientes(ActionEvent event) { navegarAVista("Clientes.fxml", "Gestión de Clientes"); }
    @FXML private void mostrarPedidos(ActionEvent event) { navegarAVista("Pedidos.fxml", "Gestión de Pedidos"); }
    @FXML private void mostrarProduccion(ActionEvent event) { navegarAVista("Planificacion.fxml", "Gestión de Producción"); }
    @FXML private void mostrarInventario(ActionEvent event) { navegarAVista("Inventario.fxml", "Gestión de Inventario"); }
    @FXML private void mostrarEntregas(ActionEvent event) { navegarAVista("Entregas.fxml", "Gestión de Entregas"); }
    @FXML private void mostrarMantenimiento(ActionEvent event) { navegarAVista("Mantenimiento.fxml", "Gestión de Mantenimiento"); }
    @FXML private void mostrarPersonal(ActionEvent event) { navegarAVista("Personal.fxml", "Gestión de Personal"); }
    @FXML private void mostrarHigiene(ActionEvent event) { navegarAVista("Limpieza.fxml", "Gestión de Higiene"); }
    @FXML private void mostrarProductos(ActionEvent event) { mostrarMensaje("Gestión de Productos", "Módulo en desarrollo"); }
    @FXML private void mostrarChefsBox(ActionEvent event) { mostrarMensaje("Chef's Box", "Módulo en desarrollo"); }
    @FXML private void mostrarReportes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Reportes.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Gestión de Reportes");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir reportes: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de reportes");
        }
    }
    @FXML private void mostrarConfiguracion(ActionEvent event) { mostrarMensaje("Configuración del Sistema", "Módulo en desarrollo"); }

    @FXML private void verMiPerfil(ActionEvent event) {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/example/demo/MiPerfil.fxml")), 800, 600));
            stage.setTitle("🍰 Repostería Rosato - Mi Perfil");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de perfil");
        }
    }

    @FXML private void mostrarAlertas(ActionEvent event) {
        cargarDatosAdministrador();
        mostrarMensaje("Alertas", "Las alertas se muestran en el panel del dashboard.");
    }

    @FXML private void mostrarLogs(ActionEvent event) {
        cargarDatosAdministrador();
        mostrarMensaje("Logs del Sistema", "Los logs se muestran en el panel del dashboard.");
    }

    private void navegarAVista(String fxmlName, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/" + fxmlName));
            Parent viewRoot = loader.load();

            BorderPane wrapper = new BorderPane();
            HBox navBar = new HBox(15);
            navBar.setPadding(new Insets(15, 30, 15, 30));
            navBar.setStyle("-fx-background-color: #8B5E3C; -fx-border-color: #7A4D2B; -fx-border-width: 0 0 2 0;");
            navBar.setAlignment(Pos.CENTER_LEFT);

            Button btnVolver = new Button("← Volver al Dashboard");
            btnVolver.setStyle("-fx-background-color: white; -fx-text-fill: #8B5E3C; -fx-font-weight: bold; -fx-background-radius: 10;");
            btnVolver.setOnAction(e -> {
                try {
                    Parent dashboardRoot = FXMLLoader.load(getClass().getResource("/com/example/demo/DashboardAdmin.fxml"));
                    Stage stage = (Stage) btnVolver.getScene().getWindow();
                    stage.getScene().setRoot(dashboardRoot);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error: {0}", ex.getMessage());
                }
            });

            Label title = new Label(titulo);
            title.setFont(Font.font("System", FontWeight.BOLD, 18));
            title.setStyle("-fx-text-fill: white;");

            navBar.getChildren().addAll(btnVolver, title);
            wrapper.setTop(navBar);
            wrapper.setCenter(viewRoot);

            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.getScene().setRoot(wrapper);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al navegar a {0}: {1}", new Object[]{fxmlName, e.getMessage()});
            mostrarError("Error", "No se pudo abrir " + titulo);
        }
    }

    private void mostrarError(String t, String m) { mostrarAlerta(Alert.AlertType.ERROR, t, m); }
    private void mostrarMensaje(String t, String m) { mostrarAlerta(Alert.AlertType.INFORMATION, t, m); }

    private void mostrarAlerta(Alert.AlertType tipo, String t, String m) {
        Alert a = new Alert(tipo);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m);
        a.showAndWait();
    }

    // CLASES TRADICIONALES (NO Records) - Compatible con Java 8/11/15
    public static class Alerta {
        private String tipo, descripcion, fechaHora, estado;
        public Alerta(String tipo, String descripcion, String fechaHora, String estado) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.fechaHora = fechaHora;
            this.estado = estado;
        }
        public String getTipo() { return tipo; }
        public String getDescripcion() { return descripcion; }
        public String getFechaHora() { return fechaHora; }
        public String getEstado() { return estado; }
    }

    public static class Actividad {
        private String fechaHora, usuario, accion, detalle;
        public Actividad(String fechaHora, String usuario, String accion, String detalle) {
            this.fechaHora = fechaHora;
            this.usuario = usuario;
            this.accion = accion;
            this.detalle = detalle;
        }
        public String getFechaHora() { return fechaHora; }
        public String getUsuario() { return usuario; }
        public String getAccion() { return accion; }
        public String getDetalle() { return detalle; }
    }
}
