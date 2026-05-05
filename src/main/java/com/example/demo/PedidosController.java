package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PedidosController {

    private static final Logger LOGGER = Logger.getLogger(PedidosController.class.getName());

    // Constantes SQL (CORREGIDO para SQL Server)
    private static final String SQL_CARGAR_PEDIDOS =
            "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, FORMAT(p.fecha_pedido, 'yyyy-MM-dd') as fecha_pedido, FORMAT(p.fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, pr.nombre as producto, p.libras, p.total, p.adelanto, p.estado FROM pedidos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente INNER JOIN productos pr ON p.id_producto = pr.id_producto ORDER BY p.fecha_pedido DESC";

    private static final String SQL_BUSCAR_PEDIDOS =
            "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, FORMAT(p.fecha_pedido, 'yyyy-MM-dd') as fecha_pedido, FORMAT(p.fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, pr.nombre as producto, p.libras, p.total, p.adelanto, p.estado FROM pedidos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente INNER JOIN productos pr ON p.id_producto = pr.id_producto WHERE CAST(p.id_pedido AS VARCHAR) LIKE ? OR c.nombre LIKE ? OR c.apellido LIKE ? OR pr.nombre LIKE ? ORDER BY p.fecha_pedido DESC";

    // Constantes para estilos
    private static final String BADGE_STYLE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
    private static final String BADGE_PENDIENTE_STYLE = "-fx-background-color: #F39C12;";
    private static final String BADGE_CONFIRMADO_STYLE = "-fx-background-color: #007BFF;";
    private static final String BADGE_EN_PRODUCCION_STYLE = "-fx-background-color: #FF9800;";
    private static final String BADGE_LISTO_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_ENTREGADO_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_CANCELADO_STYLE = "-fx-background-color: #DC3545;";
    private static final String BADGE_DEFAULT_STYLE = "-fx-background-color: #666666;";

    // Constantes para estados
    private static final String ESTADO_TODOS = "Todos";
    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_CONFIRMADO = "Confirmado";
    private static final String ESTADO_EN_PRODUCCION = "En producción";
    private static final String ESTADO_LISTO = "Listo";
    private static final String ESTADO_ENTREGADO = "Entregado";
    private static final String ESTADO_CANCELADO = "Cancelado";

    private static final java.util.List<String> ESTADOS = java.util.Arrays.asList(
            ESTADO_TODOS, ESTADO_PENDIENTE, ESTADO_CONFIRMADO, ESTADO_EN_PRODUCCION,
            ESTADO_LISTO, ESTADO_ENTREGADO, ESTADO_CANCELADO
    );

    // UI Components
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> clienteFilter;
    @FXML private ComboBox<String> estadoFilter;
    @FXML private DatePicker fechaDesdePicker;
    @FXML private DatePicker fechaHastaPicker;
    @FXML private TableView<Pedido> pedidosTable;
    @FXML private TableColumn<Pedido, Integer> idColumn;
    @FXML private TableColumn<Pedido, String> clienteColumn;
    @FXML private TableColumn<Pedido, String> fechaPedidoColumn;
    @FXML private TableColumn<Pedido, String> fechaEntregaColumn;
    @FXML private TableColumn<Pedido, String> productoColumn;
    @FXML private TableColumn<Pedido, Double> librasColumn;
    @FXML private TableColumn<Pedido, Double> totalColumn;
    @FXML private TableColumn<Pedido, Double> adelantoColumn;
    @FXML private TableColumn<Pedido, String> estadoColumn;
    @FXML private Label totalLabel;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private ObservableList<Pedido> pedidosList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        initializeFilters();
        configurarTabla();
        cargarPedidos();
        setupListeners();
    }

    private void initializeFilters() {
        for (String estado : ESTADOS) {
            estadoFilter.getItems().add(estado);
        }
        estadoFilter.getSelectionModel().selectFirst();

        cargarClientesEnFiltro();
    }

    private void cargarClientesEnFiltro() {
        clienteFilter.getItems().clear();
        clienteFilter.getItems().add(ESTADO_TODOS);

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT nombre + ' ' + apellido as nombre_completo FROM clientes ORDER BY nombre_completo");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clienteFilter.getItems().add(rs.getString("nombre_completo"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar clientes para filtro: {0}", e.getMessage());
        }

        clienteFilter.getSelectionModel().selectFirst();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clienteColumn.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        fechaPedidoColumn.setCellValueFactory(new PropertyValueFactory<>("fechaPedido"));
        fechaEntregaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        adelantoColumn.setCellValueFactory(new PropertyValueFactory<>("adelanto"));

        estadoColumn.setCellFactory(param -> new TableCell<Pedido, String>() {
            private final HBox hbox = new HBox(5);
            private final Label badge = new Label();

            {
                badge.setStyle(BADGE_STYLE_BASE);
                hbox.getChildren().setAll(badge);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                } else {
                    String estilo = obtenerEstiloEstado(estado);
                    badge.setStyle(BADGE_STYLE_BASE + estilo);
                    badge.setText(estado.toUpperCase());
                    setGraphic(hbox);
                }
            }
        });
    }

    private String obtenerEstiloEstado(String estado) {
        String estadoLower = estado.toLowerCase();
        if (estadoLower.equals("pendiente")) return BADGE_PENDIENTE_STYLE;
        if (estadoLower.equals("confirmado")) return BADGE_CONFIRMADO_STYLE;
        if (estadoLower.equals("en producción")) return BADGE_EN_PRODUCCION_STYLE;
        if (estadoLower.equals("listo")) return BADGE_LISTO_STYLE;
        if (estadoLower.equals("entregado")) return BADGE_ENTREGADO_STYLE;
        if (estadoLower.equals("cancelado")) return BADGE_CANCELADO_STYLE;
        return BADGE_DEFAULT_STYLE;
    }

    private void setupListeners() {
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                cargarPedidos();
            } else {
                buscarPedidos(newVal);
            }
        });

        clienteFilter.setOnAction(event -> aplicarFiltros());
        estadoFilter.setOnAction(event -> aplicarFiltros());
        fechaDesdePicker.setOnAction(event -> aplicarFiltros());
        fechaHastaPicker.setOnAction(event -> aplicarFiltros());
    }

    private void cargarPedidos() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_PEDIDOS);
             ResultSet rs = stmt.executeQuery()) {

            pedidosList = FXCollections.observableArrayList();

            while (rs.next()) {
                pedidosList.add(crearPedidoDesdeResultSet(rs));
            }

            pedidosTable.setItems(pedidosList);
            actualizarTotal(pedidosList.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar pedidos: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los pedidos: " + e.getMessage());
        }
    }

    private void buscarPedidos(String textoBusqueda) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_PEDIDOS)) {

            String busqueda = "%" + textoBusqueda + "%";
            stmt.setString(1, busqueda);
            stmt.setString(2, busqueda);
            stmt.setString(3, busqueda);
            stmt.setString(4, busqueda);

            ObservableList<Pedido> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(crearPedidoDesdeResultSet(rs));
                }
            }

            pedidosTable.setItems(resultados);
            actualizarTotal(resultados.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar pedidos: {0}", e.getMessage());
            mostrarError("Error de Búsqueda", "No se pudo realizar la búsqueda: " + e.getMessage());
        }
    }

    private void aplicarFiltros() {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, FORMAT(p.fecha_pedido, 'yyyy-MM-dd') as fecha_pedido, FORMAT(p.fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, pr.nombre as producto, p.libras, p.total, p.adelanto, p.estado FROM pedidos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente INNER JOIN productos pr ON p.id_producto = pr.id_producto WHERE 1=1 ");

        java.util.ArrayList<String> condiciones = new java.util.ArrayList<>();
        java.util.ArrayList<Object> parametros = new java.util.ArrayList<>();

        if (fechaDesdePicker.getValue() != null) {
            condiciones.add("p.fecha_pedido >= ?");
            parametros.add(java.sql.Date.valueOf(fechaDesdePicker.getValue()));
        }
        if (fechaHastaPicker.getValue() != null) {
            condiciones.add("p.fecha_pedido <= ?");
            parametros.add(java.sql.Date.valueOf(fechaHastaPicker.getValue()));
        }

        String clienteSeleccionado = clienteFilter.getValue();
        if (clienteSeleccionado != null && !ESTADO_TODOS.equals(clienteSeleccionado)) {
            condiciones.add("(c.nombre + ' ' + c.apellido) LIKE ?");
            parametros.add("%" + clienteSeleccionado + "%");
        }

        String estadoSeleccionado = estadoFilter.getValue();
        if (estadoSeleccionado != null && !ESTADO_TODOS.equals(estadoSeleccionado)) {
            condiciones.add("p.estado = ?");
            parametros.add(estadoSeleccionado);
        }

        if (!condiciones.isEmpty()) {
            sqlBuilder.append(" AND ").append(String.join(" AND ", condiciones));
        }

        sqlBuilder.append(" ORDER BY p.fecha_pedido DESC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            ObservableList<Pedido> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(crearPedidoDesdeResultSet(rs));
                }
            }

            pedidosTable.setItems(resultados);
            actualizarTotal(resultados.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
            mostrarError("Error de Filtros", "No se pudieron aplicar los filtros: " + e.getMessage());
        }
    }

    private Pedido crearPedidoDesdeResultSet(ResultSet rs) throws SQLException {
        return new Pedido(
                rs.getInt("id_pedido"),
                rs.getString("nombre_cliente"),
                rs.getString("fecha_pedido"),
                rs.getString("fecha_entrega"),
                rs.getString("producto"),
                rs.getDouble("libras"),
                rs.getDouble("total"),
                rs.getDouble("adelanto"),
                rs.getString("estado")
        );
    }

    @FXML
    private void nuevoPedido(ActionEvent event) {
        abrirModalPedido(null);
    }

    @FXML
    private void actualizarDatos(ActionEvent event) {
        cargarPedidos();
        mostrarMensaje("Datos Actualizados", "La lista de pedidos ha sido actualizada correctamente.");
    }

    private void actualizarTotal(int total) {
        totalLabel.setText("Total: " + total + " pedidos");
    }

    private void abrirModalPedido(Pedido pedido) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("PedidoModal.fxml"));
            Parent root = loader.load();

            PedidoModalController controller = loader.getController();

            PedidoModalController.Pedido pedidoModal = null;
            if (pedido != null) {
                pedidoModal = new PedidoModalController.Pedido(
                        pedido.getId(),
                        pedido.getNombreCliente(),
                        pedido.getFechaEntrega(),
                        pedido.getProducto(),
                        pedido.getLibras(),
                        "",
                        pedido.getTotal(),
                        pedido.getAdelanto(),
                        ""
                );
            }
            controller.setPedido(pedidoModal);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 700);
            stage.setScene(scene);
            stage.setTitle(pedido == null ? "📋 Nuevo Pedido" : "✏️ Editar Pedido");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarPedidos();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de pedido: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de pedido: " + e.getMessage());
        }
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
     * CLASE TRADICIONAL para Pedido (Java 8 compatible)
     */
    public static class Pedido {
        private int id;
        private String nombreCliente;
        private String fechaPedido;
        private String fechaEntrega;
        private String producto;
        private double libras;
        private double total;
        private double adelanto;
        private String estado;

        public Pedido(int id, String nombreCliente, String fechaPedido, String fechaEntrega,
                      String producto, double libras, double total, double adelanto, String estado) {
            this.id = id;
            this.nombreCliente = nombreCliente;
            this.fechaPedido = fechaPedido;
            this.fechaEntrega = fechaEntrega;
            this.producto = producto;
            this.libras = libras;
            this.total = total;
            this.adelanto = adelanto;
            this.estado = estado;
        }

        public int getId() { return id; }
        public String getNombreCliente() { return nombreCliente; }
        public String getFechaPedido() { return fechaPedido; }
        public String getFechaEntrega() { return fechaEntrega; }
        public String getProducto() { return producto; }
        public double getLibras() { return libras; }
        public double getTotal() { return total; }
        public double getAdelanto() { return adelanto; }
        public String getEstado() { return estado; }

        public boolean estaPendiente() { return ESTADO_PENDIENTE.equalsIgnoreCase(estado); }
        public boolean estaConfirmado() { return ESTADO_CONFIRMADO.equalsIgnoreCase(estado); }
        public boolean estaEnProduccion() { return ESTADO_EN_PRODUCCION.equalsIgnoreCase(estado); }
        public boolean estaListo() { return ESTADO_LISTO.equalsIgnoreCase(estado); }
        public boolean estaEntregado() { return ESTADO_ENTREGADO.equalsIgnoreCase(estado); }
        public boolean estaCancelado() { return ESTADO_CANCELADO.equalsIgnoreCase(estado); }

        public double getSaldo() { return total - adelanto; }
        public boolean tieneSaldoPendiente() { return getSaldo() > 0; }
        public double getPorcentajePagado() { return total > 0 ? (adelanto / total) * 100 : 0; }
    }
}