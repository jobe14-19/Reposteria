package com.example.demo;

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

public class DashboardClienteController {

    private static final Logger LOGGER = Logger.getLogger(DashboardClienteController.class.getName());

    // Constantes SQL
    private static final String SQL_KPI_CLIENTE =
            "SELECT COUNT(*) as total_pedidos, COALESCE(SUM(total), 0) as total_gastado FROM pedidos WHERE username = ?";
    private static final String SQL_PROXIMO_PEDIDO =
            "SELECT fecha_entrega FROM pedidos WHERE username = ? AND estado = 'PROGRAMADO' ORDER BY fecha_entrega ASC LIMIT 1";
    private static final String SQL_PEDIDOS_RECIENTES =
            "SELECT id_pedido, producto, libras, fecha_entrega, total, estado FROM pedidos WHERE username = ? ORDER BY fecha_entrega DESC LIMIT 5";
    private static final String SQL_PRODUCTO_FAVORITO =
            "SELECT producto, COUNT(*) as frecuencia FROM pedidos WHERE username = ? GROUP BY producto ORDER BY frecuencia DESC LIMIT 1";

    // Constantes
    private static final int REFRESH_INTERVAL_MS = 30000;
    private static final int PUNTOS_POR_GASTO = 10;
    private static final String PRODUCTO_POR_DEFECTO = "Pastel de Chocolate";
    private static final String MENSAJE_PROMO1 = "Código: WELCOME10";
    private static final String MENSAJE_PROMO3 = "Válido este mes";

    // UI Components
    @FXML private Label userLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label totalPedidosLabel;
    @FXML private Label proxPedidoLabel;
    @FXML private Label puntosLabel;
    @FXML private Label totalGastadoLabel;
    @FXML private TableView<Pedido> pedidosTable;
    @FXML private TableColumn<Pedido, Integer> idColumn;
    @FXML private TableColumn<Pedido, String> productoColumn;
    @FXML private TableColumn<Pedido, String> librasColumn;
    @FXML private TableColumn<Pedido, String> fechaColumn;
    @FXML private TableColumn<Pedido, String> totalColumn;
    @FXML private TableColumn<Pedido, String> estadoColumn;
    @FXML private Label sugerenciaProductoLabel;
    @FXML private Label sugerenciaDescLabel;
    @FXML private Label promo1Label;
    @FXML private Label promo3Label;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Timer refreshTimer;
    private DateTimeFormatter timeFormatter;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        configurarTablaPedidos();
        cargarDatosCliente();
        iniciarAutoRefresh();
        actualizarInfoUsuario();
        actualizarTimestamp();
    }

    private void configurarTablaPedidos() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void cargarDatosCliente() {
        if (!sessionManager.isLoggedIn()) {
            LOGGER.log(Level.WARNING, "Intento de cargar datos sin sesión activa");
            return;
        }

        String username = sessionManager.getUsuarioActual();

        try (Connection conn = dbConnection.getConnection()) {
            cargarKPIs(conn, username);
            cargarPedidosRecientes(conn, username);
            cargarSugerencias(conn, username);
            cargarPromociones();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del cliente: {0}", e.getMessage());
            mostrarError("Error de conexión", "No se pudieron cargar tus datos. Intenta nuevamente.");
        }
    }

    private void cargarKPIs(Connection conn, String username) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_KPI_CLIENTE)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalPedidos = rs.getInt("total_pedidos");
                    double totalGastado = rs.getDouble("total_gastado");
                    totalPedidosLabel.setText(String.valueOf(totalPedidos));
                    totalGastadoLabel.setText(String.format("$%.2f", totalGastado));
                    int puntos = (int) (totalGastado / PUNTOS_POR_GASTO);
                    puntosLabel.setText(String.valueOf(puntos));
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_PROXIMO_PEDIDO)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    proxPedidoLabel.setText(rs.getString("fecha_entrega"));
                } else {
                    proxPedidoLabel.setText("No programado");
                }
            }
        }
    }

    private void cargarPedidosRecientes(Connection conn, String username) throws SQLException {
        ObservableList<Pedido> pedidos = FXCollections.observableArrayList();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_RECIENTES)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(new Pedido(
                            rs.getInt("id_pedido"),
                            rs.getString("producto"),
                            rs.getString("libras"),
                            rs.getString("fecha_entrega"),
                            String.format("$%.2f", rs.getDouble("total")),
                            rs.getString("estado")
                    ));
                }
            }
        }
        pedidosTable.setItems(pedidos);
    }

    private void cargarSugerencias(Connection conn, String username) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTO_FAVORITO)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String productoFavorito = rs.getString("producto");
                    sugerenciaProductoLabel.setText(productoFavorito);
                    sugerenciaDescLabel.setText("Basado en tu historial, te recomendamos: " + productoFavorito);
                } else {
                    setSugerenciasPorDefecto();
                }
            }
        }
    }

    private void setSugerenciasPorDefecto() {
        sugerenciaProductoLabel.setText(PRODUCTO_POR_DEFECTO);
        sugerenciaDescLabel.setText("Nuestro producto más popular");
    }

    private void cargarPromociones() {
        promo1Label.setText(MENSAJE_PROMO1);
        promo3Label.setText(MENSAJE_PROMO3);
    }

    private void iniciarAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        cargarDatosCliente();
                        actualizarTimestamp();
                    }
                });
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
    }

    private void actualizarInfoUsuario() {
        if (sessionManager.isLoggedIn()) {
            String nombre = sessionManager.getUsuarioActual();
            userLabel.setText("👤 " + nombre + " (CLIENTE)");
            welcomeLabel.setText("¡Bienvenido de vuelta, " + nombre + "!");
        } else {
            userLabel.setText("👤 Invitado");
            welcomeLabel.setText("¡Bienvenido!");
        }
    }

    private void actualizarTimestamp() {
        String now = LocalDateTime.now().format(timeFormatter);
        lastUpdateLabel.setText("Última actualización: " + now);
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        sessionManager.cerrarSesion();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Sistema de Gestión");
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al regresar al login: {0}", e.getMessage());
        }
    }

    @FXML
    private void mostrarDashboard(ActionEvent event) {
        cargarDatosCliente();
        actualizarTimestamp();
    }

    @FXML
    private void verMisPedidos(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MisPedidos.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root, 1000, 600);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Mis Pedidos");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir Mis Pedidos: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de pedidos");
        }
    }

    @FXML
    private void verMiPerfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MiPerfil.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Mi Perfil");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir Mi Perfil: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de perfil");
        }
    }

    @FXML
    private void verChefsBox(ActionEvent event) {
        mostrarMensaje("Chef's Box", "Suscripción mensual Chef's Box en desarrollo");
    }

    @FXML
    private void pedirSugerencia(ActionEvent event) {
        String producto = sugerenciaProductoLabel.getText();
        mostrarMensaje("Pedido Sugerido", "Procesando tu pedido sugerido: " + producto);
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.ERROR, titulo, mensaje);
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * CLASE TRADICIONAL para Pedido (NO Record)
     */
    public static class Pedido {
        private int id;
        private String producto;
        private String libras;
        private String fechaEntrega;
        private String total;
        private String estado;

        public Pedido(int id, String producto, String libras, String fechaEntrega, String total, String estado) {
            this.id = id;
            this.producto = producto;
            this.libras = libras;
            this.fechaEntrega = fechaEntrega;
            this.total = total;
            this.estado = estado;
        }

        public int getId() { return id; }
        public String getProducto() { return producto; }
        public String getLibras() { return libras; }
        public String getFechaEntrega() { return fechaEntrega; }
        public String getTotal() { return total; }
        public String getEstado() { return estado; }

        public boolean isEntregado() { return "ENTREGADO".equalsIgnoreCase(estado); }
        public boolean isProgramado() { return "PROGRAMADO".equalsIgnoreCase(estado); }
        public boolean isCancelado() { return "CANCELADO".equalsIgnoreCase(estado); }
    }
}