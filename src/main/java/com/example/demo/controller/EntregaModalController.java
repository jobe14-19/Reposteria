package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntregaModalController {

    private static final Logger LOGGER = Logger.getLogger(EntregaModalController.class.getName());

    // Constantes SQL usando Text Blocks (Java 15+)
    private static final String SQL_PEDIDOS_PENDIENTES = """
        SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente,
               c.direccion, p.total, p.adelanto,
               p.total - p.adelanto as saldo,
               CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo
        FROM pedidos p
        INNER JOIN clientes c ON p.id_cliente = c.id_cliente
        WHERE p.estado IN ('Confirmado', 'Pendiente', 'Listo para entregar')
          AND (p.total - p.adelanto) > 0
        ORDER BY p.fecha_entrega
        """;

    private static final String SQL_INSERTAR_PAGO = """
        INSERT INTO pagos (id_pedido, monto, fecha_pago, metodo_pago, referencia, estado)
        VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, 'Pagado')
        """;

    private static final String SQL_ACTUALIZAR_PEDIDO = """
        UPDATE pedidos SET estado = 'Entregado', fecha_entrega_real = CURRENT_TIMESTAMP,
        tipo_entrega = ?, direccion_entrega = ?, costo_delivery = ?
        WHERE id_pedido = ?
        """;

    private static final String SQL_REGISTRAR_ACTIVIDAD = """
        INSERT INTO actividad (fecha_hora, usuario, accion, detalle)
        VALUES (CURRENT_TIMESTAMP, ?, ?, ?)
        """;

    // Constantes
    private static final double BASE_DELIVERY_COST = 5.0;
    private static final double COST_PER_KM = 2.0;
    private static final String TIPO_ENTREGA_LOCAL = "L";
    private static final String TIPO_ENTREGA_DELIVERY = "D";
    private static final String PAGO_EFECTIVO = "Efectivo";
    private static final String REFERENCIA_DISABLED_STYLE = "-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-width: 1;";
    private static final String REFERENCIA_ENABLED_STYLE = "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1;";

    // UI Components
    @FXML private Label tituloLabel;
    @FXML private ComboBox<String> pedidoComboBox;
    @FXML private Label pedidoIdLabel;
    @FXML private Label clienteLabel;
    @FXML private Label totalLabel;
    @FXML private Label adelantoLabel;
    @FXML private Label saldoLabel;
    @FXML private RadioButton localRadioButton;
    @FXML private RadioButton deliveryRadioButton;
    @FXML private ToggleGroup tipoEntregaGroup;
    @FXML private VBox deliveryDetails;
    @FXML private TextField direccionField;
    @FXML private TextField distanciaField;
    @FXML private TextField costoDeliveryField;
    @FXML private TextField montoCobrarField;
    @FXML private ComboBox<String> metodoPagoComboBox;
    @FXML private TextField referenciaField;
    @FXML private Button limpiarButton;
    @FXML private Button cancelarButton;
    @FXML private Button guardarResultado;

    // Services and Managers
    private DatabaseConnection dbConnection;
    private SessionManager sessionManager;
    private EntregasController.PedidoPendiente pedidoActual;
    private double costoDelivery = 0.0;
    private Map<String, EntregasController.PedidoPendiente> pedidoMap = new HashMap<>();

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        initializeRadioButtons();
        initializePaymentCombo();
        setupEventHandlers();
        deliveryDetails.setVisible(false);
        cargarPedidosPendientes();
    }

    private void initializeRadioButtons() {
        localRadioButton.setToggleGroup(tipoEntregaGroup);
        deliveryRadioButton.setToggleGroup(tipoEntregaGroup);
        localRadioButton.setSelected(true);
    }

    private void initializePaymentCombo() {
        metodoPagoComboBox.getItems().addAll(PAGO_EFECTIVO, "Tarjeta", "Transferencia");
        metodoPagoComboBox.getSelectionModel().selectFirst();
        referenciaField.setDisable(true);
        referenciaField.setStyle(REFERENCIA_DISABLED_STYLE);
    }

    private void cargarPedidosPendientes() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_PENDIENTES);
             ResultSet rs = stmt.executeQuery()) {

            pedidoComboBox.getItems().clear();
            pedidoMap.clear();

            while (rs.next()) {
                int id = rs.getInt("id_pedido");
                String nombre = rs.getString("nombre_cliente");
                String direccion = rs.getString("direccion");
                double total = rs.getDouble("total");
                double adelanto = rs.getDouble("adelanto");
                double saldo = rs.getDouble("saldo");
                String tipo = rs.getString("tipo");

                EntregasController.PedidoPendiente pedido = new EntregasController.PedidoPendiente(id, nombre, direccion, total, adelanto, saldo, tipo);
                String display = "#" + id + " - " + nombre + " ($" + String.format("%.2f", saldo) + ")";
                pedidoComboBox.getItems().add(display);
                pedidoMap.put(display, pedido);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar pedidos pendientes: {0}", e.getMessage());
        }
    }

    private void seleccionarPedido() {
        String selected = pedidoComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            EntregasController.PedidoPendiente pedido = pedidoMap.get(selected);
            if (pedido != null) {
                setPedido(pedido);
                return;
            }
        }
        limpiarDetalles();
    }

    private void limpiarDetalles() {
        pedidoActual = null;
        pedidoIdLabel.setText("-");
        clienteLabel.setText("-");
        totalLabel.setText("-");
        adelantoLabel.setText("-");
        saldoLabel.setText("-");
        localRadioButton.setSelected(true);
        direccionField.clear();
        distanciaField.clear();
        costoDeliveryField.clear();
        montoCobrarField.clear();
        metodoPagoComboBox.getSelectionModel().selectFirst();
        referenciaField.clear();
    }

    private void setupEventHandlers() {
        // Pedido selection handler
        pedidoComboBox.setOnAction(event -> seleccionarPedido());

        // Delivery type change handler
        tipoEntregaGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDelivery = newVal == deliveryRadioButton;
            deliveryDetails.setVisible(isDelivery);
            costoDelivery = isDelivery ? calcularCostoDeliveryDesdeDistancia() : 0.0;
            actualizarCostoDeliveryField();
            actualizarMontoCobrar();
        });

        // Distance change handler
        distanciaField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                costoDelivery = calcularCostoDeliveryDesdeDistancia();
                actualizarCostoDeliveryField();
                actualizarMontoCobrar();
            }
        });

        // Payment method change handler
        metodoPagoComboBox.setOnAction(event -> actualizarReferencia());
    }

    public void setPedido(EntregasController.PedidoPendiente pedido) {
        this.pedidoActual = pedido;

        if (pedido != null) {
            pedidoIdLabel.setText(String.valueOf(pedido.getId()));
            clienteLabel.setText(pedido.getNombreCliente());
            totalLabel.setText(String.format("%.2f", pedido.getTotal()));
            adelantoLabel.setText(String.format("%.2f", pedido.getAdelanto()));
            saldoLabel.setText(String.format("%.2f", pedido.getSaldo()));
            montoCobrarField.setText(String.format("%.2f", pedido.getSaldo()));
            direccionField.setText(pedido.getDireccion());
        }
    }

    private double calcularCostoDeliveryDesdeDistancia() {
        try {
            String distanciaTexto = distanciaField.getText();
            if (distanciaTexto == null || distanciaTexto.isBlank()) {
                return 0.0;
            }
            double distancia = Double.parseDouble(distanciaTexto);
            return BASE_DELIVERY_COST + (distancia * COST_PER_KM);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error en formato de distancia: {0}", e.getMessage());
            return 0.0;
        }
    }

    private void actualizarCostoDeliveryField() {
        costoDeliveryField.setText(String.format("%.2f", costoDelivery));
    }

    private void actualizarMontoCobrar() {
        if (pedidoActual != null) {
            double monto = pedidoActual.getSaldo();
            if (deliveryRadioButton.isSelected()) {
                monto += costoDelivery;
            }
            montoCobrarField.setText(String.format("%.2f", monto));
        }
    }

    private void actualizarReferencia() {
        String metodo = metodoPagoComboBox.getSelectionModel().getSelectedItem();
        boolean esEfectivo = PAGO_EFECTIVO.equals(metodo);

        referenciaField.setDisable(esEfectivo);
        if (esEfectivo) {
            referenciaField.clear();
            referenciaField.setStyle(REFERENCIA_DISABLED_STYLE);
        } else {
            referenciaField.setStyle(REFERENCIA_ENABLED_STYLE);
        }
    }

    @FXML
    private void guardarResultado(ActionEvent event) {
        if (!sonCamposValidos()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
            return;
        }

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            registrarPago(conn);
            actualizarPedido(conn);
            registrarActividad("REGISTRAR ENTREGA",
                    "Entrega registrada para pedido #" + pedidoActual.getId() + " - Monto: $" + montoCobrarField.getText());

            conn.commit();

            generarFacturaSimulada();
            enviarWhatsAppSimulado();

            mostrarMensaje("Entrega Registrada",
                    "La entrega ha sido registrada correctamente.\n\n" +
                            "Factura generada: factura_pedido_" + pedidoActual.getId() + ".pdf\n" +
                            "Comprobante enviado por WhatsApp.");

            cerrarModal();

        } catch (SQLException e) {
            realizarRollback(conn);
            LOGGER.log(Level.SEVERE, "Error al registrar entrega: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo registrar la entrega: " + e.getMessage());
        }
    }

    private void registrarPago(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PAGO)) {
            String metodo = metodoPagoComboBox.getSelectionModel().getSelectedItem();
            String referencia = PAGO_EFECTIVO.equals(metodo) ? null : obtenerTexto(referenciaField);

            stmt.setInt(1, pedidoActual.getId());
            stmt.setDouble(2, obtenerMontoCobrar());
            stmt.setString(3, metodo);
            stmt.setString(4, referencia);
            stmt.executeUpdate();
        }
    }

    private void actualizarPedido(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_PEDIDO)) {
            String tipoEntrega = localRadioButton.isSelected() ? TIPO_ENTREGA_LOCAL : TIPO_ENTREGA_DELIVERY;
            String direccion = deliveryRadioButton.isSelected() ? obtenerTexto(direccionField) : null;
            double deliveryCost = deliveryRadioButton.isSelected() ? costoDelivery : 0.0;

            stmt.setString(1, tipoEntrega);
            stmt.setString(2, direccion);
            stmt.setDouble(3, deliveryCost);
            stmt.setInt(4, pedidoActual.getId());
            stmt.executeUpdate();
        }
    }

    @FXML
    private void cancelarResultado(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void limpiarCampos(ActionEvent event) {
        pedidoComboBox.getSelectionModel().clearSelection();
        limpiarDetalles();
    }

    private boolean sonCamposValidos() {
        if (pedidoActual == null) {
            return false;
        }

        String metodo = metodoPagoComboBox.getSelectionModel().getSelectedItem();
        if (metodo == null) {
            return false;
        }

        // Validate reference for non-cash payments
        if (!PAGO_EFECTIVO.equals(metodo)) {
            String referencia = referenciaField.getText();
            if (referencia == null || referencia.isBlank()) {
                return false;
            }
        }

        // Validate delivery details if selected
        if (deliveryRadioButton.isSelected()) {
            String direccion = direccionField.getText();
            String distancia = distanciaField.getText();

            if (direccion == null || direccion.isBlank()) {
                return false;
            }
            if (distancia == null || distancia.isBlank()) {
                return false;
            }
            try {
                Double.parseDouble(distancia);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private double obtenerMontoCobrar() {
        try {
            String texto = montoCobrarField.getText();
            return (texto != null && !texto.isBlank()) ? Double.parseDouble(texto) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String obtenerTexto(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void generarFacturaSimulada() {
        LOGGER.log(Level.INFO, "Generando factura PDF para pedido #{0}", pedidoActual.getId());
        LOGGER.log(Level.INFO, "Archivo: factura_pedido_{0}.pdf", pedidoActual.getId());
    }

    private void enviarWhatsAppSimulado() {
        LOGGER.log(Level.INFO, "Enviando comprobante por WhatsApp para pedido #{0}", pedidoActual.getId());
        LOGGER.log(Level.INFO, "Mensaje: Su pedido #{0} ha sido entregado. Total: ${1}",
                new Object[]{pedidoActual.getId(), montoCobrarField.getText()});
    }

    private void realizarRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error en rollback: {0}", e.getMessage());
            }
        }
    }

    private void registrarActividad(String accion, String detalle) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {

            stmt.setString(1, sessionManager.getUsuarioActual());
            stmt.setString(2, accion);
            stmt.setString(3, detalle);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
        }
    }

    private void cerrarModal() {
        Stage stage = (Stage) guardarResultado.getScene().getWindow();
        stage.close();
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
}
