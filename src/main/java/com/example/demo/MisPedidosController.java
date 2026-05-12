package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MisPedidosController {

    private static final Logger LOGGER = Logger.getLogger(MisPedidosController.class.getName());

    // Constantes SQL - CORREGIDO para SQLite
    private static final String SQL_CLIENTE_NOMBRE = "SELECT nombre FROM clientes WHERE id_cliente = ?";

    private static final String SQL_PEDIDOS_CLIENTE = """
        SELECT p.id_pedido, p.producto, p.libras,
               CAST(p.fecha_entrega AS DATE) as fecha_entrega,
               p.total, p.adelanto, p.total - p.adelanto as saldo, p.estado
        FROM pedidos p
        WHERE p.id_cliente = ?
        ORDER BY p.fecha_pedido DESC
        """;

    // Constantes para estilos
    private static final String BADGE_STYLE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
    private static final String BADGE_PENDIENTE_STYLE = "-fx-background-color: #FF9800;";
    private static final String BADGE_CONFIRMADO_STYLE = "-fx-background-color: #007BFF;";
    private static final String BADGE_EN_PRODUCCION_STYLE = "-fx-background-color: #6F42C1;";
    private static final String BADGE_LISTO_ENTREGAR_STYLE = "-fx-background-color: #17A2B8;";
    private static final String BADGE_ENTREGADO_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_CANCELADO_STYLE = "-fx-background-color: #DC3545;";
    private static final String BADGE_DEFAULT_STYLE = "-fx-background-color: #666666;";

    private static final String BUTTON_SEGUIMIENTO_STYLE = "-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BUTTON_FACTURA_STYLE = "-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BUTTON_DETALLE_STYLE = "-fx-background-color: #8B5E3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

    // Constantes para estados
    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_CONFIRMADO = "Confirmado";
    private static final String ESTADO_EN_PRODUCCION = "En producción";
    private static final String ESTADO_LISTO_ENTREGAR = "Listo para entregar";
    private static final String ESTADO_ENTREGADO = "Entregado";
    private static final String ESTADO_CANCELADO = "Cancelado";

    // UI Components
    @FXML private Button nuevoPedidoButton;
    @FXML private Label clienteLabel;
    @FXML private Label totalPedidosLabel;
    @FXML private Label saldoPendienteLabel;
    @FXML private TableView<PedidoCliente> pedidosTable;
    @FXML private TableColumn<PedidoCliente, Integer> idColumn;
    @FXML private TableColumn<PedidoCliente, String> productoColumn;
    @FXML private TableColumn<PedidoCliente, Double> librasColumn;
    @FXML private TableColumn<PedidoCliente, String> fechaEntregaColumn;
    @FXML private TableColumn<PedidoCliente, Double> totalColumn;
    @FXML private TableColumn<PedidoCliente, Double> adelantoColumn;
    @FXML private TableColumn<PedidoCliente, Double> saldoColumn;
    @FXML private TableColumn<PedidoCliente, String> estadoColumn;
    @FXML private TableColumn<PedidoCliente, Void> accionesColumn;
    @FXML private Label totalLabel;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private ObservableList<PedidoCliente> pedidosList;
    private int idCliente;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        idCliente = sessionManager.getIdUsuarioActual();

        configurarTabla();
        cargarInfoCliente();
        cargarPedidos();
        actualizarEstadisticas();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        fechaEntregaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        adelantoColumn.setCellValueFactory(new PropertyValueFactory<>("adelanto"));
        saldoColumn.setCellValueFactory(new PropertyValueFactory<>("saldo"));

        // Status Column with badge - USANDO CLASE ANÓNIMA TRADICIONAL
        estadoColumn.setCellFactory(new javafx.util.Callback<TableColumn<PedidoCliente, String>, TableCell<PedidoCliente, String>>() {
            @Override
            public TableCell<PedidoCliente, String> call(TableColumn<PedidoCliente, String> param) {
                return new TableCell<PedidoCliente, String>() {
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
                };
            }
        });

        // Actions Column with buttons - USANDO CLASE ANÓNIMA TRADICIONAL
        accionesColumn.setCellFactory(new javafx.util.Callback<TableColumn<PedidoCliente, Void>, TableCell<PedidoCliente, Void>>() {
            @Override
            public TableCell<PedidoCliente, Void> call(TableColumn<PedidoCliente, Void> param) {
                return new TableCell<PedidoCliente, Void>() {
                    private final Button seguimientoButton = new Button("🔄");
                    private final Button facturaButton = new Button("📄");
                    private final Button detalleButton = new Button("👁️");
                    private final HBox hbox = new HBox(5);

                    {
                        seguimientoButton.setStyle(BUTTON_SEGUIMIENTO_STYLE);
                        facturaButton.setStyle(BUTTON_FACTURA_STYLE);
                        detalleButton.setStyle(BUTTON_DETALLE_STYLE);

                        hbox.getChildren().setAll(seguimientoButton, facturaButton, detalleButton);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            PedidoCliente pedido = getTableView().getItems().get(getIndex());

                            // Configurar acciones de los botones
                            seguimientoButton.setOnAction(e -> verSeguimiento(pedido));
                            facturaButton.setOnAction(e -> verFactura(pedido));
                            detalleButton.setOnAction(e -> verDetalle(pedido));

                            // Mostrar/ocultar botones según estado
                            hbox.getChildren().clear();
                            hbox.getChildren().add(detalleButton);

                            if (ESTADO_EN_PRODUCCION.equalsIgnoreCase(pedido.getEstado())) {
                                hbox.getChildren().add(seguimientoButton);
                            }

                            if (ESTADO_ENTREGADO.equalsIgnoreCase(pedido.getEstado())) {
                                hbox.getChildren().add(facturaButton);
                            }

                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
    }

    private String obtenerEstiloEstado(String estado) {
        if (estado == null) return BADGE_DEFAULT_STYLE;

        switch (estado.toLowerCase()) {
            case "pendiente": return BADGE_PENDIENTE_STYLE;
            case "confirmado": return BADGE_CONFIRMADO_STYLE;
            case "en producción": return BADGE_EN_PRODUCCION_STYLE;
            case "listo para entregar": return BADGE_LISTO_ENTREGAR_STYLE;
            case "entregado": return BADGE_ENTREGADO_STYLE;
            case "cancelado": return BADGE_CANCELADO_STYLE;
            default: return BADGE_DEFAULT_STYLE;
        }
    }

    private void cargarInfoCliente() {
        try (var conn = dbConnection.getConnection();
             var stmt = conn.prepareStatement(SQL_CLIENTE_NOMBRE)) {

            stmt.setInt(1, idCliente);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    clienteLabel.setText(rs.getString("nombre"));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar info del cliente: {0}", e.getMessage());
        }
    }

    private void cargarPedidos() {
        try (var conn = dbConnection.getConnection();
             var stmt = conn.prepareStatement(SQL_PEDIDOS_CLIENTE)) {

            stmt.setInt(1, idCliente);

            pedidosList = FXCollections.observableArrayList();

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PedidoCliente pedido = new PedidoCliente(
                            rs.getInt("id_pedido"),
                            rs.getString("producto"),
                            rs.getDouble("libras"),
                            rs.getString("fecha_entrega"),
                            rs.getDouble("total"),
                            rs.getDouble("adelanto"),
                            rs.getDouble("saldo"),
                            rs.getString("estado")
                    );
                    pedidosList.add(pedido);
                }
            }

            pedidosTable.setItems(pedidosList);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar pedidos: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los pedidos: " + e.getMessage());
        }
    }

    private void actualizarEstadisticas() {
        int totalPedidos = pedidosList.size();
        double saldoPendiente = 0;

        for (PedidoCliente p : pedidosList) {
            if (!ESTADO_ENTREGADO.equalsIgnoreCase(p.getEstado()) &&
                    !ESTADO_CANCELADO.equalsIgnoreCase(p.getEstado())) {
                saldoPendiente += p.getSaldo();
            }
        }

        totalPedidosLabel.setText(String.valueOf(totalPedidos));
        saldoPendienteLabel.setText(String.format("$%.2f", saldoPendiente));
        totalLabel.setText("Total: " + totalPedidos + " pedidos");
    }

    @FXML
    private void nuevoPedido(ActionEvent event) {
        try {
            var loader = new FXMLLoader(getClass().getResource("/com/example/demo/Pedidos.fxml"));
            var root = loader.<Parent>load();

            var stage = new Stage();
            var scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("➕ Nuevo Pedido");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarPedidos();
            actualizarEstadisticas();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir pedidos: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de pedidos: " + e.getMessage());
        }
    }

    private void verSeguimiento(PedidoCliente pedido) {
        String mensaje = "Abriendo seguimiento de producción para pedido #" + pedido.getId() +
                "\n\nEsta función abrirá la pantalla de planificación con el pedido seleccionado.";
        mostrarMensaje("Seguimiento de Pedido", mensaje);
    }

    private void verFactura(PedidoCliente pedido) {
        String mensaje = "Generando factura para pedido #" + pedido.getId() +
                "\n\nTotal: $" + String.format("%.2f", pedido.getTotal()) +
                "\nAdelanto: $" + String.format("%.2f", pedido.getAdelanto()) +
                "\nSaldo: $" + String.format("%.2f", pedido.getSaldo());
        mostrarMensaje("Factura de Pedido", mensaje);
    }

    private void verDetalle(PedidoCliente pedido) {
        abrirModalDetalle(pedido);
    }

    private void abrirModalDetalle(PedidoCliente pedido) {
        try {
            var loader = new FXMLLoader(getClass().getResource("/com/example/demo/PedidoDetalleModal.fxml"));
            var root = loader.<Parent>load();

            var controller = loader.<PedidoDetalleModalController>getController();
            controller.setPedido(pedido);

            var stage = new Stage();
            var scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.setTitle("📋 Detalle del Pedido #" + pedido.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de detalle: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir el detalle del pedido: " + e.getMessage());
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
     * CLASE TRADICIONAL para PedidoCliente (evita problemas con Records)
     * Esto es más compatible con PropertyValueFactory y otros componentes JavaFX
     */
    public static class PedidoCliente {
        private int id;
        private String producto;
        private double libras;
        private String fechaEntrega;
        private double total;
        private double adelanto;
        private double saldo;
        private String estado;

        public PedidoCliente(int id, String producto, double libras, String fechaEntrega,
                             double total, double adelanto, double saldo, String estado) {
            this.id = id;
            this.producto = producto;
            this.libras = libras;
            this.fechaEntrega = fechaEntrega;
            this.total = total;
            this.adelanto = adelanto;
            this.saldo = saldo;
            this.estado = estado;
        }

        // Getters
        public int getId() { return id; }
        public String getProducto() { return producto; }
        public double getLibras() { return libras; }
        public String getFechaEntrega() { return fechaEntrega; }
        public double getTotal() { return total; }
        public double getAdelanto() { return adelanto; }
        public double getSaldo() { return saldo; }
        public String getEstado() { return estado; }

        // Setters (opcionales)
        public void setId(int id) { this.id = id; }
        public void setProducto(String producto) { this.producto = producto; }
        public void setLibras(double libras) { this.libras = libras; }
        public void setFechaEntrega(String fechaEntrega) { this.fechaEntrega = fechaEntrega; }
        public void setTotal(double total) { this.total = total; }
        public void setAdelanto(double adelanto) { this.adelanto = adelanto; }
        public void setSaldo(double saldo) { this.saldo = saldo; }
        public void setEstado(String estado) { this.estado = estado; }

        // Métodos de conveniencia
        public boolean estaPendiente() { return ESTADO_PENDIENTE.equalsIgnoreCase(estado); }
        public boolean estaConfirmado() { return ESTADO_CONFIRMADO.equalsIgnoreCase(estado); }
        public boolean estaEnProduccion() { return ESTADO_EN_PRODUCCION.equalsIgnoreCase(estado); }
        public boolean estaListoParaEntregar() { return ESTADO_LISTO_ENTREGAR.equalsIgnoreCase(estado); }
        public boolean estaEntregado() { return ESTADO_ENTREGADO.equalsIgnoreCase(estado); }
        public boolean estaCancelado() { return ESTADO_CANCELADO.equalsIgnoreCase(estado); }
        public boolean tieneSaldoPendiente() { return saldo > 0; }
        public double getPorcentajePagado() { return total > 0 ? (adelanto / total) * 100 : 0; }

        public String getResumenPedido() {
            return String.format("Pedido #%d\nProducto: %s\nLibras: %.2f\nFecha de entrega: %s\nTotal: $%.2f\nAdelanto: $%.2f\nSaldo: $%.2f\nEstado: %s",
                    id, producto, libras, fechaEntrega, total, adelanto, saldo, estado);
        }
    }
}