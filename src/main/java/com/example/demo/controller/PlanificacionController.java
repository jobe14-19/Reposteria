package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlanificacionController {

    private static final Logger LOGGER = Logger.getLogger(PlanificacionController.class.getName());

    // UI Components
    @FXML private Button pestana1Button;
    @FXML private Button pestana2Button;
    @FXML private Label usuarioLabel;
    @FXML private Button cerrarSesionButton;
    @FXML private VBox planificacionView;
    @FXML private VBox seguimientoView;
    @FXML private GridPane semanaGridPane;
    @FXML private Label totalPedidosLabel;
    @FXML private Label enProduccionLabel;
    @FXML private Label listosEntregarLabel;
    @FXML private ListView<String> alertasListView;
    @FXML private TextField buscarPedidoField;
    @FXML private Button buscarPedidoButton;
    @FXML private Button verDetallesButton;
    @FXML private Label pedidoIdLabel;
    @FXML private Label clienteLabel;
    @FXML private Label estadoActualLabel;
    @FXML private ListView<String> timelineListView;
    @FXML private GridPane pasosGridPane;
    @FXML private Button actualizarButton;
    @FXML private Button marcarListoButton;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Pedido pedidoSeleccionado;
    private ObservableList<Pedido> pedidosList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        if (!sessionManager.isAdmin() && !sessionManager.isAreaProduccion()) {
            mostrarError("Acceso Denegado", "Solo administradores y personal de producción pueden acceder a la planificación.");
            return;
        }

        // Initialize views
        planificacionView.setVisible(true);
        seguimientoView.setVisible(false);

        // Load initial data
        cargarDatosPlanificacion();

        // Setup event handlers
        setupEventHandlers();

        // Update user info
        actualizarInfoUsuario();
    }

    private void setupEventHandlers() {
        pestana1Button.setOnAction(event -> mostrarPlanificacion());
        pestana2Button.setOnAction(event -> mostrarSeguimiento());
        cerrarSesionButton.setOnAction(event -> cerrarSesion());

        buscarPedidoField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                limpiarSeguimiento();
            } else {
                buscarPedido(newVal);
            }
        });

        buscarPedidoButton.setOnAction(event -> buscarPedido(buscarPedidoField.getText()));
        verDetallesButton.setOnAction(event -> verDetallesPedido());
        actualizarButton.setOnAction(event -> actualizarEstadoPedido());
        marcarListoButton.setOnAction(event -> marcarComoListo());
    }

    private void mostrarPlanificacion() {
        planificacionView.setVisible(true);
        seguimientoView.setVisible(false);
        pestana1Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");
        pestana2Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");

        cargarDatosPlanificacion();
    }

    private void mostrarSeguimiento() {
        planificacionView.setVisible(false);
        seguimientoView.setVisible(true);
        pestana1Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
        pestana2Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");

        cargarPedidosConfirmados();
    }

    private void cargarDatosPlanificacion() {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                    "pr.nombre as producto, p.libras, " +
                    "DATEPART(WEEKDAY, p.fecha_entrega) as dia_semana, " +
                    "p.fecha_entrega, p.estado " +
                    "FROM pedidos p " +
                    "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                    "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                    "WHERE p.estado IN ('Confirmado', 'En producción', 'Listo') " +
                    "AND p.fecha_entrega >= DATEADD(DAY, -DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
                    "AND p.fecha_entrega <= DATEADD(DAY, 6 - DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
                    "ORDER BY p.fecha_entrega";

            pedidosList = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pedido pedido = new Pedido(
                            rs.getInt("id_pedido"),
                            rs.getString("nombre_cliente"),
                            rs.getString("producto"),
                            rs.getDouble("libras"),
                            rs.getInt("dia_semana"),
                            rs.getString("fecha_entrega"),
                            rs.getString("estado")
                    );
                    pedidosList.add(pedido);
                }
            }

            construirSemanalGrid();
            cargarEstadisticas();
            cargarAlertas();

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: usando datos de demostración en planificación");
            pedidosList = FXCollections.observableArrayList();
            construirSemanalGrid();
            totalPedidosLabel.setText("--");
            enProduccionLabel.setText("--");
            listosEntregarLabel.setText("--");
            alertasListView.setItems(FXCollections.observableArrayList());
        }
    }

    private void construirSemanalGrid() {
        semanaGridPane.getChildren().clear();

        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

        for (int i = 0; i < dias.length; i++) {
            Label diaLabel = new Label(dias[i]);
            semanaGridPane.add(diaLabel, 0, i);
        }

        for (Pedido pedido : pedidosList) {
            int diaSemana = pedido.getDiaSemana();
            if (diaSemana >= 1 && diaSemana <= 7) {
                VBox pedidoCard = crearTarjetaPedido(pedido);
                semanaGridPane.add(pedidoCard, 1, diaSemana - 1);
            }
        }
    }

    private VBox crearTarjetaPedido(Pedido pedido) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label clienteLabel = new Label(pedido.getNombreCliente());
        clienteLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label productoLabel = new Label(pedido.getProducto());
        productoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        Label librasLabel = new Label(pedido.getLibras() + " lbs");
        librasLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        card.getChildren().addAll(clienteLabel, productoLabel, librasLabel);

        return card;
    }

    private void cargarEstadisticas() {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT " +
                    "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Confirmado') as total_pedidos, " +
                    "(SELECT COUNT(*) FROM pedidos WHERE estado = 'En producción') as en_produccion, " +
                    "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Listo') as listos_entregar";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalPedidosLabel.setText(String.valueOf(rs.getInt("total_pedidos")));
                    enProduccionLabel.setText(String.valueOf(rs.getInt("en_produccion")));
                    listosEntregarLabel.setText(String.valueOf(rs.getInt("listos_entregar")));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: estadísticas no disponibles");
        }
    }

    private void cargarAlertas() {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT TOP 10 " +
                    "'Pedido #' + CAST(p.id_pedido AS VARCHAR) + ' - ' + c.nombre + ' - ' + pr.nombre + ' - ' + p.estado as alerta " +
                    "FROM pedidos p " +
                    "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                    "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                    "WHERE p.estado IN ('En producción', 'Listo') " +
                    "ORDER BY p.fecha_entrega";

            ObservableList<String> alertas = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alertas.add(rs.getString("alerta"));
                }
            }

            alertasListView.setItems(alertas);

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: alertas no disponibles");
            alertasListView.setItems(FXCollections.observableArrayList());
        }
    }

    private void cargarPedidosConfirmados() {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                    "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
                    "FROM pedidos p " +
                    "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                    "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                    "WHERE p.estado IN ('Confirmado', 'En producción', 'Listo') " +
                    "ORDER BY p.fecha_entrega";

            pedidosList = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pedido pedido = new Pedido(
                            rs.getInt("id_pedido"),
                            rs.getString("nombre_cliente"),
                            rs.getString("producto"),
                            rs.getDouble("libras"),
                            0,
                            rs.getString("fecha_entrega"),
                            rs.getString("estado")
                    );
                    pedidosList.add(pedido);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: pedidos confirmados no disponibles");
            pedidosList = FXCollections.observableArrayList();
        }
    }

    private void buscarPedido(String textoBusqueda) {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT TOP 1 p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                    "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
                    "FROM pedidos p " +
                    "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                    "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                    "WHERE (CAST(p.id_pedido AS VARCHAR) LIKE ? OR " +
                    "c.nombre LIKE ? OR c.apellido LIKE ?) " +
                    "AND p.estado IN ('Confirmado', 'En producción', 'Listo')";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String busqueda = "%" + textoBusqueda + "%";
                stmt.setString(1, busqueda);
                stmt.setString(2, busqueda);
                stmt.setString(3, busqueda);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        pedidoSeleccionado = new Pedido(
                                rs.getInt("id_pedido"),
                                rs.getString("nombre_cliente"),
                                rs.getString("producto"),
                                rs.getDouble("libras"),
                                0,
                                rs.getString("fecha_entrega"),
                                rs.getString("estado")
                        );
                        mostrarDetallesPedido(pedidoSeleccionado);
                    } else {
                        limpiarSeguimiento();
                        mostrarMensaje("No Encontrado", "No se encontró ningún pedido con ese criterio.");
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Modo offline: búsqueda de pedidos no disponible");
            mostrarMensaje("Búsqueda no disponible", "La búsqueda de pedidos no está disponible en modo offline.");
        }
    }

    private void verDetallesPedido() {
        if (pedidoSeleccionado != null) {
            mostrarDetallesPedido(pedidoSeleccionado);
        } else {
            mostrarMensaje("Sin Selección", "Por favor seleccione un pedido para ver sus detalles.");
        }
    }

    private void mostrarDetallesPedido(Pedido pedido) {
        pedidoIdLabel.setText(String.valueOf(pedido.getId()));
        clienteLabel.setText(pedido.getNombreCliente());
        estadoActualLabel.setText(pedido.getEstado());

        construirTimeline(pedido);
    }

    private void construirTimeline(Pedido pedido) {
        timelineListView.getItems().clear();

        String[] pasos = {
                "1. Preparación de masas",
                "2. Horneado",
                "3. Enfriado controlado",
                "4. Preparación de rellenos",
                "5. Decoración",
                "6. Empaque y control de calidad"
        };

        for (String paso : pasos) {
            timelineListView.getItems().add(paso);
        }
    }

    @FXML
    private void actualizarEstadoPedido() {
        if (pedidoSeleccionado != null) {
            mostrarMensaje("Actualizar Estado", "Función de actualización de estado en desarrollo.");
        } else {
            mostrarMensaje("Sin Selección", "Por favor seleccione un pedido para actualizar su estado.");
        }
    }

    @FXML
    private void marcarComoListo() {
        if (pedidoSeleccionado != null) {
            try (Connection conn = dbConnection.getConnection()) {
                String sql = "UPDATE pedidos SET estado = 'Listo' " +
                        "WHERE id_pedido = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, pedidoSeleccionado.getId());
                    int filasAfectadas = stmt.executeUpdate();

                    if (filasAfectadas > 0) {
                        mostrarMensaje("Pedido Actualizado", "El pedido ha sido marcado como listo para entregar. Consulte la planificación semanal para ver el estado actualizado.");
                        cargarPedidosConfirmados();
                    } else {
                        mostrarError("Error al Actualizar", "No se pudo actualizar el estado del pedido.");
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.INFO, "Modo offline: no se puede actualizar el estado del pedido");
                mostrarMensaje("Modo offline", "No se puede actualizar el estado del pedido en modo offline.");
            }
        } else {
            mostrarMensaje("Sin Selección", "Por favor seleccione un pedido para marcar como listo.");
        }
    }

    private void limpiarSeguimiento() {
        pedidoSeleccionado = null;
        pedidoIdLabel.setText("-");
        clienteLabel.setText("-");
        estadoActualLabel.setText("-");
        timelineListView.getItems().clear();
    }

    private void actualizarInfoUsuario() {
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            String nombre = sessionManager.getUsuarioActual();
            String perfil = sessionManager.getPerfilActual();
            usuarioLabel.setText("👤 " + nombre + " (" + perfil + ")");
        } else {
            usuarioLabel.setText("👤 Usuario");
        }
    }

    private void cerrarSesion() {
        if (sessionManager != null) {
            sessionManager.cerrarSesion();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) cerrarSesionButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Sistema de Gestión");
            stage.show();

        } catch (Exception e) {
            System.err.println("Error al regresar al login: " + e.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Data model for orders
    public static class Pedido {
        private int id;
        private String nombreCliente;
        private String producto;
        private double libras;
        private int diaSemana;
        private String fechaEntrega;
        private String estado;

        public Pedido(int id, String nombreCliente, String producto, double libras, int diaSemana, String fechaEntrega, String estado) {
            this.id = id;
            this.nombreCliente = nombreCliente;
            this.producto = producto;
            this.libras = libras;
            this.diaSemana = diaSemana;
            this.fechaEntrega = fechaEntrega;
            this.estado = estado;
        }

        public int getId() { return id; }
        public String getNombreCliente() { return nombreCliente; }
        public String getProducto() { return producto; }
        public double getLibras() { return libras; }
        public int getDiaSemana() { return diaSemana; }
        public String getFechaEntrega() { return fechaEntrega; }
        public String getEstado() { return estado; }
    }
}
