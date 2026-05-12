package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Optional;

public class PlanificacionController {

    // UI Components
    @FXML
    private Button pestana1Button;
    @FXML
    private Button pestana2Button;
    @FXML
    private Label usuarioLabel;
    @FXML
    private Button cerrarSesionButton;
    @FXML
    private VBox planificacionView;
    @FXML
    private VBox seguimientoView;
    @FXML
    private GridPane semanaGridPane;
    @FXML
    private Label totalPedidosLabel;
    @FXML
    private Label enProduccionLabel;
    @FXML
    private Label listosEntregarLabel;
    @FXML
    private ListView<String> alertasListView;
    @FXML
    private TextField buscarPedidoField;
    @FXML
    private Button verDetallesButton;
    @FXML
    private Label pedidoIdLabel;
    @FXML
    private Label clienteLabel;
    @FXML
    private Label estadoActualLabel;
    @FXML
    private ListView<String> timelineListView;
    @FXML
    private GridPane pasosGridPane;
    @FXML
    private Button actualizarButton;
    @FXML
    private Button marcarListoButton;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Pedido pedidoSeleccionado;
    private ObservableList<Pedido> pedidosList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

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

        verDetallesButton.setOnAction(event -> verDetallesPedido());
        actualizarButton.setOnAction(event -> actualizarEstadoPedido());
        marcarListoButton.setOnAction(event -> marcarComoListo());
    }

    private void mostrarPlanificacion() {
        planificacionView.setVisible(true);
        seguimientoView.setVisible(false);
        pestana1Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");
        pestana2Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");

        // Refresh planning data
        cargarDatosPlanificacion();
    }

    private void mostrarSeguimiento() {
        planificacionView.setVisible(false);
        seguimientoView.setVisible(true);
        pestana1Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
        pestana2Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");

        // Load orders for tracking
        cargarPedidosConfirmados();
    }

    private void cargarDatosPlanificacion() {
        try {
            Connection conn = dbConnection.getConnection();

            // Load weekly orders
            String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                       "pr.nombre as producto, p.libras, " +
                       "DATEPART(WEEKDAY, p.fecha_entrega) as dia_semana, " +
                       "p.fecha_entrega, p.estado " +
                       "FROM pedidos p " +
                       "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                       "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                       "WHERE p.estado IN ('Confirmado', 'En producción') " +
                       "AND p.fecha_entrega >= DATEADD(DAY, -DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
                       "AND p.fecha_entrega <= DATEADD(DAY, 6 - DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
                       "ORDER BY p.fecha_entrega";

            pedidosList = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Pedido pedido = new Pedido(
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("producto"),
                        rs.getDouble("libras"),
                        rs.getString("dia_semana"),
                        rs.getString("fecha_entrega"),
                        rs.getString("estado")
                    );
                    pedidosList.add(pedido);
                }
            }

            // Build weekly grid
            construirSemanalGrid();

            // Load statistics
            cargarEstadisticas();

            // Load alerts
            cargarAlertas();

        } catch (SQLException e) {
            System.err.println("Error al cargar datos de planificación: " + e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los datos de planificación: " + e.getMessage());
        }
    }

    private void construirSemanalGrid() {
        // Clear existing content
        semanaGridPane.getChildren().clear();

        // Add day headers
        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        for (int i = 0; i < dias.length; i++) {
            Label diaLabel = new Label(dias[i]);
            diaLabel.getStyleClass().add("day-header");
            if (i >= 5) { // Weekend
                diaLabel.getStyleClass().add("day-cell-weekend");
            }
            semanaGridPane.add(diaLabel, 0, i + 1);

            // Add capacity labels
            Label capacidadLabel = new Label("Capacidad (24h)");
            capacidadLabel.getStyleClass().add("capacity-label");
            semanaGridPane.add(capacidadLabel, 1, i + 1);

            Label ocupadoLabel = new Label("Ocupado");
            ocupadoLabel.getStyleClass().add("capacity-label");
            semanaGridPane.add(ocupadoLabel, 2, i + 1);

            Label disponibleLabel = new Label("Disponible");
            disponibleLabel.getStyleClass().add("capacity-label");
            semanaGridPane.add(disponibleLabel, 3, i + 1);

            // Add capacity bars
            ProgressBar capacidadBar = new ProgressBar(0.0);
            capacidadBar.getStyleClass().add("capacity-bar");
            semanaGridPane.add(capacidadBar, 1, i + 1);
        }

        // Add orders to appropriate days
        for (Pedido pedido : pedidosList) {
            int diaSemana = obtenerDiaSemana(pedido.getDiaSemana());
            if (diaSemana >= 1 && diaSemana <= 7) {
                VBox pedidoCard = crearTarjetaPedido(pedido);
                semanaGridPane.add(pedidoCard, 4, diaSemana + 1);
            }
        }

        // Update capacity bars
        actualizarBarrasCapacidad();
    }

    private VBox crearTarjetaPedido(Pedido pedido) {
        VBox card = new VBox(5);
        card.getStyleClass().add("day-cell");

        Label clienteLabel = new Label(pedido.getNombreCliente());
        clienteLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #333333;");

        Label productoLabel = new Label(pedido.getProducto());
        productoLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

        Label librasLabel = new Label(pedido.getLibras() + " lbs");
        librasLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

        Label tiempoLabel = new Label(calcularTiempoEstimado(pedido.getLibras()));
        tiempoLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

        card.getChildren().addAll(clienteLabel, productoLabel, librasLabel, tiempoLabel);

        return card;
    }

    private String calcularTiempoEstimado(double libras) {
        // Simple estimation: 30 minutes per pound
        int minutos = (int) (libras * 30);
        int horas = minutos / 60;
        int minutosRestantes = minutos % 60;

        return String.format("%dh %dm", horas, minutosRestantes);
    }

    private void actualizarBarrasCapacidad() {
        // Calculate capacity for each day
        double[] capacidadPorDia = new double[7]; // Monday to Friday only

        for (int i = 0; i < 7; i++) {
            capacidadPorDia[i] = 24.0; // 24 hours per day
        }

        // Calculate occupied time
        for (Pedido pedido : pedidosList) {
            int diaSemana = obtenerDiaSemana(pedido.getDiaSemana());
            if (diaSemana >= 1 && diaSemana <= 7) {
                double tiempoProduccion = calcularTiempoProduccion(pedido.getLibras());
                capacidadPorDia[diaSemana - 1] -= tiempoProduccion;
            }
        }

        // Update progress bars
        for (int i = 0; i < 7; i++) {
            double ocupado = 24.0 - capacidadPorDia[i];
            double porcentaje = ocupado / 24.0;

            ProgressBar bar = (ProgressBar) semanaGridPane.getChildren().get((i * 4) + 1); // Capacity bar for day i
            if (bar != null) {
                bar.setProgress(porcentaje);

                // Update availability labels
                Label ocupadoLabel = (Label) semanaGridPane.getChildren().get((i * 4) + 2);
                Label disponibleLabel = (Label) semanaGridPane.getChildren().get((i * 4) + 3);

                if (ocupadoLabel != null && disponibleLabel != null) {
                    ocupadoLabel.setText(String.format("%.1f h", ocupado));
                    disponibleLabel.setText(String.format("%.1f h", capacidadPorDia[i]));
                }
            }
        }
    }

    private double calcularTiempoProduccion(double libras) {
        // Production time estimation: 15 minutes per pound
        return libras * 0.25; // 0.25 hours per pound
    }

    private int obtenerDiaSemana(String dia) {
        switch (dia.toLowerCase()) {
            case "lunes": return 1;
            case "martes": return 2;
            case "miércoles": return 3;
            case "jueves": return 4;
            case "viernes": return 5;
            case "sábado": return 6;
            case "domingo": return 7;
            default: return 0;
        }
    }

    private void cargarEstadisticas() {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT " +
                       "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Confirmado') as total_pedidos, " +
                       "(SELECT COUNT(*) FROM pedidos WHERE estado = 'En producción') as en_produccion, " +
                       "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Listo') as listos_entregar";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    totalPedidosLabel.setText(String.valueOf(rs.getInt("total_pedidos")));
                    enProduccionLabel.setText(String.valueOf(rs.getInt("en_produccion")));
                    listosEntregarLabel.setText(String.valueOf(rs.getInt("listos_entregar")));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar estadísticas: " + e.getMessage());
        }
    }

    private void cargarAlertas() {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT TOP 10 " +
                       "'Pedido #' + CAST(p.id_pedido AS VARCHAR) + ' - ' + c.nombre + ' - ' + p.producto + ' - ' + p.estado' as alerta " +
                       "FROM pedidos p " +
                       "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                       "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                       "WHERE p.estado IN ('En producción', 'Listo') " +
                       "AND p.fecha_entrega <= DATEADD(HOUR, 2, GETDATE()) " +
                       "ORDER BY p.fecha_entrega";

            ObservableList<String> alertas = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    alertas.add(rs.getString("alerta"));
                }
            }

            alertasListView.setItems(alertas);

        } catch (SQLException e) {
            System.err.println("Error al cargar alertas: " + e.getMessage());
        }
    }

    private void cargarPedidosConfirmados() {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                       "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
                       "FROM pedidos p " +
                       "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                       "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                       "WHERE p.estado IN ('Confirmado', 'En producción', 'Listo') " +
                       "ORDER BY p.fecha_entrega";

            pedidosList = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Pedido pedido = new Pedido(
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("producto"),
                        rs.getDouble("libras"),
                        null, // No day needed for tracking
                        rs.getString("fecha_entrega"),
                        rs.getString("estado")
                    );
                    pedidosList.add(pedido);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar pedidos confirmados: " + e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los pedidos confirmados: " + e.getMessage());
        }
    }

    private void buscarPedido(String textoBusqueda) {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
                       "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
                       "FROM pedidos p " +
                       "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                       "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
                       "WHERE (CAST(p.id_pedido AS VARCHAR) LIKE ? OR " +
                       "c.nombre LIKE ? OR c.apellido LIKE ?) " +
                       "AND p.estado IN ('Confirmado', 'En producción', 'Listo') " +
                       "ORDER BY p.fecha_entrega";

            ObservableList<Pedido> resultados = FXCollections.observableArrayList();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String busqueda = "%" + textoBusqueda + "%";
                stmt.setString(1, busqueda);
                stmt.setString(2, busqueda);
                stmt.setString(3, busqueda);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Pedido pedido = new Pedido(
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("producto"),
                        rs.getDouble("libras"),
                        null, // No day needed for tracking
                        rs.getString("fecha_entrega"),
                        rs.getString("estado")
                    );
                    resultados.add(pedido);
                }
            }

            // Update order selection combo
            // This would require a ComboBox in the FXML
            // For now, just show first result
            if (!resultados.isEmpty()) {
                pedidoSeleccionado = resultados.get(0);
                mostrarDetallesPedido(pedidoSeleccionado);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar pedido: " + e.getMessage());
            mostrarError("Error de Búsqueda", "No se pudo realizar la búsqueda: " + e.getMessage());
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
        // Update order details display
        pedidoIdLabel.setText(String.valueOf(pedido.getId()));
        clienteLabel.setText(pedido.getNombreCliente());
        estadoActualLabel.setText(pedido.getEstado());

        // Build production timeline
        construirTimeline(pedido);
    }

    private void construirTimeline(Pedido pedido) {
        // Clear existing timeline
        pasasGridPane.getChildren().clear();
        timelineListView.getItems().clear();

        // Production steps
        String[] pasos = {
            "1. Preparación de masas",
            "2. Horneado (¡NO abrir horno!)",
            "3. Enfriado controlado",
            "4. Preparación de rellenos",
            "5. Decoración",
            "6. Empaque y control de calidad"
        };

        for (int i = 0; i < pasos.length; i++) {
            CheckBox pasoCheckBox = new CheckBox(pasos[i]);
            pasoCheckBox.setDisable(true); // Initially disabled until order is selected

            Label pasoLabel = new Label(pasos[i]);
            pasoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #8B5E3C;");

            pasasGridPane.add(pasoCheckBox, 0, i);
            pasasGridPane.add(pasoLabel, 1, i);

            timelineListView.getItems().add(pasos[i]);
        }

        // Enable checkboxes for selected order
        if (pedido != null) {
            // Load current progress from database
            cargarProgresoPedido(pedido);
        }
    }

    private void cargarProgresoPedido(Pedido pedido) {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT paso, timestamp FROM pasos_produccion WHERE id_pedido = ? ORDER BY paso";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, pedido.getId());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int paso = rs.getInt("paso");
                    String timestamp = rs.getString("timestamp");

                    // Update checkbox
                    if (paso - 1 < pasasGridPane.getChildren().size() / 2) {
                        CheckBox checkBox = (CheckBox) pasasGridPane.getChildren().get((paso - 1) * 2);
                        if (checkBox != null) {
                            checkBox.setSelected(true);

                            // Update timeline item
                            String pasoText = checkBox.getText() + " - " + timestamp;
                            if (paso - 1 < timelineListView.getItems().size()) {
                                timelineListView.getItems().set(paso - 1, pasoText);
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar progreso del pedido: " + e.getMessage());
        }
    }

    @FXML
    private void actualizarEstadoPedido() {
        if (pedidoSeleccionado != null) {
            // This would open a modal to update order status
            mostrarMensaje("Actualizar Estado", "Función de actualización de estado en desarrollo.");
        } else {
            mostrarMensaje("Sin Selección", "Por favor seleccione un pedido para actualizar su estado.");
        }
    }

    @FXML
    private void marcarComoListo() {
        if (pedidoSeleccionado != null) {
            if (validarPasosCompletados()) {
                try {
                    Connection conn = dbConnection.getConnection();

                    String sql = "UPDATE pedidos SET estado = 'Listo para entregar', " +
                               "fecha_modificacion = GETDATE() " +
                               "WHERE id_pedido = ?";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, pedidoSeleccionado.getId());
                        int filasAfectadas = stmt.executeUpdate();

                        if (filasAfectadas > 0) {
                            // Log activity
                            registrarActividad("MARCAR COMO LISTO", "Pedido #" + pedidoSeleccionado.getId() + " marcado como listo para entregar");

                            mostrarMensaje("Pedido Actualizado", "El pedido ha sido marcado como listo para entregar.");

                            // Refresh data
                            if (seguimientoView.isVisible()) {
                                cargarPedidosConfirmados();
                            }
                        } else {
                            mostrarError("Error al Actualizar", "No se pudo actualizar el estado del pedido.");
                        }
                    }

                } catch (SQLException e) {
                    System.err.println("Error al marcar pedido como listo: " + e.getMessage());
                    mostrarError("Error de Base de Datos", "No se pudo actualizar el pedido: " + e.getMessage());
                }
            } else {
                mostrarError("Pasos Incompletos", "Debe completar todos los pasos de producción antes de marcar como listo.");
            }
        } else {
            mostrarMensaje("Sin Selección", "Por favor seleccione un pedido para marcar como listo.");
        }
    }

    private boolean validarPasosCompletados() {
        // Check if all production steps are completed
        for (int i = 0; i < pasasGridPane.getChildren().size() / 2; i++) {
            CheckBox checkBox = (CheckBox) pasasGridPane.getChildren().get(i * 2);
            if (checkBox != null && !checkBox.isSelected()) {
                return false;
            }
        }
        return true;
    }

    private void limpiarSeguimiento() {
        pedidoSeleccionado = null;
        pedidoIdLabel.setText("-");
        clienteLabel.setText("-");
        estadoActualLabel.setText("-");
        timelineListView.getItems().clear();
        pasasGridPane.getChildren().clear();
    }

    private void actualizarInfoUsuario() {
        if (sessionManager.isLoggedIn()) {
            String nombre = sessionManager.getUsuarioActual();
            String perfil = sessionManager.getPerfilActual();
            usuarioLabel.setText("👤 " + nombre + " (" + perfil + ")");
        }
    }

    private void registrarActividad(String accion, String detalle) {
        try {
            Connection conn = dbConnection.getConnection();

            String sql = "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionManager.getUsuarioActual());
                stmt.setString(2, accion);
                stmt.setString(3, detalle);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar actividad: " + e.getMessage());
        }
    }

    private void cerrarSesion() {
        sessionManager.cerrarSesion();

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
        private String diaSemana;
        private String fechaEntrega;
        private String estado;

        public Pedido(int id, String nombreCliente, String producto, double libras, String diaSemana, String fechaEntrega, String estado) {
            this.id = id;
            this.nombreCliente = nombreCliente;
            this.producto = producto;
            this.libras = libras;
            this.diaSemana = diaSemana;
            this.fechaEntrega = fechaEntrega;
            this.estado = estado;
        }

        // Getters
        public int getId() { return id; }
        public String getNombreCliente() { return nombreCliente; }
        public String getProducto() { return producto; }
        public Double getLibras() { return libras; }
        public String getDiaSemana() { return diaSemana; }
        public String getFechaEntrega() { return fechaEntrega; }
        public String getEstado() { return estado; }
    }
}
