package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PedidoDetalleModalController {

    private static final Logger LOGGER = Logger.getLogger(PedidoDetalleModalController.class.getName());

    // Constantes SQL (CORREGIDO para SQL Server)
    private static final String SQL_DETALLE_PEDIDO =
            "SELECT id_pedido, producto, libras, FORMAT(fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, total, estado, diseno, observaciones FROM pedidos WHERE id_pedido = ?";

    private static final String SQL_TIMELINE_PEDIDO =
            "SELECT FORMAT(fecha_hora, 'yyyy-MM-dd HH:mm') as fecha, accion, detalle FROM actividad WHERE detalle LIKE ? ORDER BY fecha_hora DESC";

    // Constantes
    private static final String SIN_DISENO = "Sin diseño especificado";
    private static final String SIN_OBSERVACIONES = "Sin observaciones";
    private static final String SIN_ACTIVIDAD = "No hay actividad registrada para este pedido.";
    private static final String LIKE_PATTERN = "%%pedido %d%%";

    // UI Components
    @FXML private Label tituloLabel;
    @FXML private Label pedidoIdLabel;
    @FXML private Label productoLabel;
    @FXML private Label librasLabel;
    @FXML private Label fechaEntregaLabel;
    @FXML private Label totalLabel;
    @FXML private Label estadoLabel;
    @FXML private TextArea disenoTextArea;
    @FXML private TextArea observacionesTextArea;
    @FXML private ListView<String> timelineListView;
    @FXML private Button cerrarResultado;

    // Services and Managers
    private DatabaseConnection dbConnection;
    private MisPedidosController.PedidoCliente pedidoActual;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
    }

    public void setPedido(MisPedidosController.PedidoCliente pedido) {
        this.pedidoActual = pedido;

        if (pedido != null) {
            tituloLabel.setText("📋 Detalle del Pedido #" + pedido.getId());
            cargarDetallePedido();
            cargarTimeline();
        }
    }

    private void cargarDetallePedido() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DETALLE_PEDIDO)) {

            stmt.setInt(1, pedidoActual.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    actualizarUIDetalle(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar detalle del pedido: {0}", e.getMessage());
            mostrarError("Error al cargar detalles");
        }
    }

    private void actualizarUIDetalle(ResultSet rs) throws SQLException {
        pedidoIdLabel.setText(String.valueOf(rs.getInt("id_pedido")));
        productoLabel.setText(rs.getString("producto"));
        librasLabel.setText(String.valueOf(rs.getDouble("libras")));
        fechaEntregaLabel.setText(rs.getString("fecha_entrega"));
        totalLabel.setText(String.format("$%.2f", rs.getDouble("total")));

        String estado = rs.getString("estado");
        estadoLabel.setText(estado);
        estadoLabel.setStyle(obtenerEstiloEstado(estado));

        disenoTextArea.setText(obtenerTextoONull(rs.getString("diseno"), SIN_DISENO));
        observacionesTextArea.setText(obtenerTextoONull(rs.getString("observaciones"), SIN_OBSERVACIONES));
    }

    private String obtenerTextoONull(String valor, String defaultValue) {
        return (valor != null && !valor.trim().isEmpty()) ? valor : defaultValue;
    }

    private String obtenerEstiloEstado(String estado) {
        String estadoLower = estado.toLowerCase();
        if (estadoLower.equals("pendiente")) return "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
        if (estadoLower.equals("confirmado")) return "-fx-text-fill: #007BFF; -fx-font-weight: bold;";
        if (estadoLower.equals("en producción")) return "-fx-text-fill: #6F42C1; -fx-font-weight: bold;";
        if (estadoLower.equals("listo para entregar")) return "-fx-text-fill: #17A2B8; -fx-font-weight: bold;";
        if (estadoLower.equals("entregado")) return "-fx-text-fill: #28A745; -fx-font-weight: bold;";
        if (estadoLower.equals("cancelado")) return "-fx-text-fill: #DC3545; -fx-font-weight: bold;";
        return "-fx-text-fill: #666666;";
    }

    private void cargarTimeline() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_TIMELINE_PEDIDO)) {

            String pattern = String.format(LIKE_PATTERN, pedidoActual.getId());
            stmt.setString(1, pattern);

            ObservableList<String> timeline = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fecha = rs.getString("fecha");
                    String accion = rs.getString("accion");
                    String detalle = rs.getString("detalle");
                    timeline.add(String.format("%s - %s: %s", fecha, accion, detalle != null ? detalle : ""));
                }
            }

            if (timeline.isEmpty()) {
                timeline.add(SIN_ACTIVIDAD);
            }

            timelineListView.setItems(timeline);

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar timeline: {0}", e.getMessage());
            mostrarError("Error al cargar el historial del pedido");
        }
    }

    @FXML
    private void cerrarResultado() {
        Stage stage = (Stage) cerrarResultado.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        mostrarAlerta(Alert.AlertType.ERROR, "Error", mensaje);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}