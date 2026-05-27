package com.example.demo.controller;
import com.example.demo.service.PayPalConfig;
import com.example.demo.service.PayPalService;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.dao.PagoDAO;
import com.example.demo.dao.PedidoDAO;
import com.example.demo.model.Pago;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientePedidosController {

    private static final Logger LOGGER = Logger.getLogger(ClientePedidosController.class.getName());

    private static final String SQL_MIS_PEDIDOS =
        "SELECT id_pedido, producto, libras, FORMAT(fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, total, adelanto, estado, ISNULL(tipo_pago, 'Efectivo') as tipo_pago, ISNULL(estado_pago, 'Pendiente') as estado_pago FROM pedidos WHERE username = ? ORDER BY id_pedido DESC";

    private static final String SQL_ACTUALIZAR_ESTADO_PAGO =
        "UPDATE pedidos SET estado_pago = ? WHERE id_pedido = ?";

    @FXML private Label totalLabel;
    @FXML private TableView<PedidoCliente> pedidosTable;
    @FXML private TableColumn<PedidoCliente, Integer> idColumn;
    @FXML private TableColumn<PedidoCliente, String> productoColumn;
    @FXML private TableColumn<PedidoCliente, Double> librasColumn;
    @FXML private TableColumn<PedidoCliente, String> fechaColumn;
    @FXML private TableColumn<PedidoCliente, Double> totalColumn;
    @FXML private TableColumn<PedidoCliente, Double> saldoColumn;
    @FXML private TableColumn<PedidoCliente, String> tipoPagoColumn;
    @FXML private TableColumn<PedidoCliente, String> estadoPagoColumn;
    @FXML private TableColumn<PedidoCliente, String> estadoColumn;
    @FXML private TableColumn<PedidoCliente, Void> accionesColumn;

    private SessionManager session;
    private DatabaseConnection dbConnection;

    private static final String BADGE_BASE = "-fx-background-radius: 10; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";

    @FXML
    public void initialize() {
        session = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();
        configurarTabla();
        cargarPedidos();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));

        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        saldoColumn.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        saldoColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                double total = getTableView().getItems().get(getIndex()).getTotal();
                if (total == 0) {
                    setText("Sin precio");
                    setStyle("-fx-text-fill: #999; -fx-font-weight: bold; -fx-font-style: italic;");
                } else if (item > 0) {
                    setText(String.format("Pendiente $%.2f", item));
                    setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                } else {
                    setText("Pagado");
                    setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
                }
            }
        });

        tipoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("tipoPago"));

        estadoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
        estadoPagoColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String est, boolean empty) {
                super.updateItem(est, empty);
                if (empty || est == null) { setText(null); setStyle(""); return; }
                setText(est);
                String bg;
                switch (est) {
                    case "Pagado": bg = "#28A745"; break;
                    case "En Proceso": bg = "#FF9800"; break;
                    case "Pendiente": bg = "#DC3545"; break;
                    default: bg = "#6C757D";
                }
                setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        estadoColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            private final HBox hbox = new HBox(5, badge);
            { badge.setStyle(BADGE_BASE); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String color = switch (item.toLowerCase()) {
                    case "pendiente" -> "#F39C12";
                    case "confirmado" -> "#007BFF";
                    case "en producci\u00f3n" -> "#FF9800";
                    case "listo" -> "#17A2B8";
                    case "entregado" -> "#28A745";
                    case "cancelado" -> "#DC3545";
                    default -> "#666";
                };
                badge.setStyle(BADGE_BASE + "-fx-background-color: " + color + ";");
                badge.setText(item.toUpperCase());
                setGraphic(hbox);
            }
        });

        accionesColumn.setCellFactory(param -> new TableCell<>() {
            private final Button detalleBtn = new Button("Detalle");
            {
                detalleBtn.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
                detalleBtn.setOnAction(e -> mostrarDetalle(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : detalleBtn);
            }
        });
    }

    private void cargarPedidos() {
        String username = session.getUsuarioActual();
        if (username == null) { totalLabel.setText("Inicie sesion para ver sus pedidos"); return; }
        ObservableList<PedidoCliente> list = FXCollections.observableArrayList();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_MIS_PEDIDOS)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new PedidoCliente(
                        rs.getInt("id_pedido"),
                        rs.getString("producto"),
                        rs.getDouble("libras"),
                        rs.getString("fecha_entrega"),
                        rs.getDouble("total"),
                        rs.getDouble("adelanto"),
                        rs.getString("estado"),
                        rs.getString("tipo_pago"),
                        rs.getString("estado_pago")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error cargar pedidos: {0}", e.getMessage());
        }
        pedidosTable.setItems(list);
        totalLabel.setText("Total: " + list.size() + " pedido(s)");
    }

    private void mostrarDetalle(PedidoCliente p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Pedido #" + p.getId());
        dialog.setHeaderText("Detalle del Pedido");

        VBox content = new VBox(8);
        content.setPadding(new Insets(15));

        Label estadoPagoLabel = new Label("Estado Pago: " + p.getEstadoPago());
        estadoPagoLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> estadoPagoCombo = new ComboBox<>();
        estadoPagoCombo.getItems().addAll("Pendiente", "Pagado", "En Proceso", "Reembolsado");
        estadoPagoCombo.setValue(p.getEstadoPago());
        Button cambiarPagoBtn = new Button("Cambiar Estado Pago");
        cambiarPagoBtn.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white; -fx-font-weight: bold;");
        cambiarPagoBtn.setOnAction(e -> {
            String nuevoEstado = estadoPagoCombo.getValue();
            if (nuevoEstado == null || nuevoEstado.equals(p.getEstadoPago())) return;
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO_PAGO)) {
                stmt.setString(1, nuevoEstado);
                stmt.setInt(2, p.getId());
                stmt.executeUpdate();
                p.setEstadoPago(nuevoEstado);
                estadoPagoLabel.setText("Estado Pago: " + nuevoEstado);
                cargarPedidos();
                mostrarMensaje("Actualizado", "Estado de pago cambiado a: " + nuevoEstado);
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Error cambiar estado pago: {0}", ex.getMessage());
                mostrarError("Error", "No se pudo cambiar el estado de pago.");
            }
        });

        HBox pagoRow = new HBox(10, estadoPagoCombo, cambiarPagoBtn);

        boolean puedePagar = !"Pagado".equals(p.getEstadoPago()) && p.getTotal() > 0 && PayPalConfig.isConfigured();
        Button pagarBtn = new Button("Pagar con PayPal");
        pagarBtn.setStyle("-fx-background-color: #0070BA; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
        pagarBtn.setVisible(puedePagar);
        pagarBtn.setManaged(puedePagar);
        pagarBtn.setOnAction(e -> pagarConPayPal(p));

        double total = p.getTotal();
        double saldo = p.getSaldo();
        String saldoText;
        if (total == 0) {
            saldoText = "Sin precio (pendiente del cajero)";
        } else if (saldo > 0) {
            saldoText = String.format("Pendiente $%.2f", saldo);
        } else {
            saldoText = "Pagado";
        }
        Label saldoDetalleLbl = new Label("Saldo: " + saldoText);
        if (total == 0) saldoDetalleLbl.setStyle("-fx-text-fill: #999; -fx-font-weight: bold;");
        else if (saldo > 0) saldoDetalleLbl.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
        else saldoDetalleLbl.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");

        content.getChildren().addAll(
            new Label("Producto: " + p.getProducto()),
            new Label("Libras: " + p.getLibras()),
            new Label("Fecha de entrega: " + p.getFechaEntrega()),
            new Label("Total: $" + (total == 0 ? "0.00 (sin asignar)" : String.format("%.2f", total))),
            new Label("Adelanto: $" + String.format("%.2f", p.getAdelanto())),
            saldoDetalleLbl,
            new Label("Tipo de Pago: " + p.getTipoPago()),
            estadoPagoLabel,
            pagoRow,
            pagarBtn,
            new Label("Estado: " + p.getEstado())
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(380, 380);
        dialog.showAndWait();
    }

    private void pagarConPayPal(PedidoCliente p) {
        double saldo = p.getSaldo();
        if (saldo <= 0) {
            mostrarError("Error", "Este pedido ya esta pagado.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Pago con PayPal");
        confirm.setHeaderText("Pedido #" + p.getId());
        confirm.setContentText("Vas a pagar $" + String.format("%.2f", saldo) + " con PayPal.\nSe abrira el navegador para completar el pago.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        new Thread(() -> {
            try {
                PayPalService paypal = new PayPalService();
                PayPalService.PayPalCheckoutResult result = paypal.crearCheckoutSession(saldo,
                    "Pedido #" + p.getId() + " - Pago pendiente", null, p.getId());

                if (!result.ok) {
                    Platform.runLater(() -> mostrarError("Error", "No se pudo iniciar el pago con PayPal:\n" + result.url));
                    return;
                }

                String finalUrl = result.url;
                Platform.runLater(() -> {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Redirigiendo a PayPal");
                    info.setHeaderText(null);
                    info.setContentText("Se abrira el navegador para completar el pago.\nEspera mientras confirmamos el pago...");
                    info.show();
                    try {
                        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(finalUrl));
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error al abrir navegador: {0}", ex.getMessage());
                    }
                });

                boolean confirmado = false;
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(3000);
                    if (paypal.verificarPago(result.sessionId)) {
                        new PedidoDAO().actualizarAdelantoYEstadoPago(p.getId(), p.getTotal(), "Pagado");
                        new OrdenProduccionDAO().actualizarPagoPorIdPedido(p.getId(), p.getTotal(), "Pagado");
                        new PagoDAO().insertar(new Pago(0, p.getId(), p.getTotal(), null, "PayPal", "PayPal Checkout", "Pagado"));
                        confirmado = true;
                        break;
                    }
                }

                boolean finalConfirmado = confirmado;
                Platform.runLater(() -> {
                    if (finalConfirmado) {
                        cargarPedidos();
                        mostrarMensaje("Pago Exitoso", "Pago de $" + String.format("%.2f", saldo) + " confirmado.\nPedido #" + p.getId() + " pagado correctamente.");
                    } else {
                        mostrarMensaje("Pago Pendiente",
                            "El pago no se confirmo en el tiempo esperado.\nPuedes intentarlo de nuevo desde tus pedidos.");
                    }
                });

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en pago PayPal: {0}", e.getMessage());
                Platform.runLater(() -> mostrarError("Error de Pago", "Ocurrio un error al procesar el pago:\n" + e.getMessage()));
            }
        }).start();
    }

    private void mostrarMensaje(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    @FXML
    private void nuevoPedido(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ClientePedidoForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 750));
            stage.setTitle("Nuevo Pedido - Pastel Personalizado");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarPedidos();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir formulario: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir el formulario de pedido");
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class PedidoCliente {
        private int id;
        private String producto;
        private double libras;
        private String fechaEntrega;
        private double total;
        private double adelanto;
        private String estado;
        private String tipoPago;
        private String estadoPago;

        public PedidoCliente(int id, String producto, double libras, String fechaEntrega, double total, double adelanto, String estado) {
            this(id, producto, libras, fechaEntrega, total, adelanto, estado, "Efectivo", "Pendiente");
        }

        public PedidoCliente(int id, String producto, double libras, String fechaEntrega, double total, double adelanto, String estado, String tipoPago, String estadoPago) {
            this.id = id;
            this.producto = producto;
            this.libras = libras;
            this.fechaEntrega = fechaEntrega;
            this.total = total;
            this.adelanto = adelanto;
            this.estado = estado;
            this.tipoPago = tipoPago;
            this.estadoPago = estadoPago;
        }

        public int getId() { return id; }
        public String getProducto() { return producto; }
        public double getLibras() { return libras; }
        public String getFechaEntrega() { return fechaEntrega; }
        public double getTotal() { return total; }
        public double getAdelanto() { return adelanto; }
        public double getSaldo() { return total - adelanto; }
        public String getEstado() { return estado; }
        public String getTipoPago() { return tipoPago; }
        public String getEstadoPago() { return estadoPago; }
        public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    }
}
