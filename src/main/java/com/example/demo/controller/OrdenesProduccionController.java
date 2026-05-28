package com.example.demo.controller;

import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.Receta;
import com.example.demo.service.PayPalService;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrdenesProduccionController {

    private static final Logger LOGGER = Logger.getLogger(OrdenesProduccionController.class.getName());

    @FXML private TableView<OrdenProduccion> ordenesTable;
    @FXML private TableColumn<OrdenProduccion, String> numColumn, estadoColumn, clienteColumn;
    @FXML private TableColumn<OrdenProduccion, String> fechaColumn, horaInicioColumn, fechaFinColumn;
    @FXML private TableColumn<OrdenProduccion, String> categoriaColumn, vendedorColumn;
    @FXML private TableColumn<OrdenProduccion, String> tipoPagoColumn, estadoPagoColumn, recetaColumn;
    @FXML private TableColumn<OrdenProduccion, Double> librasColumn, totalColumn;
    @FXML private TableColumn<OrdenProduccion, Void> accionesColumn;
    @FXML private ComboBox<String> filtroEstadoCombo;
    @FXML private TextField buscarField;
    @FXML private Label totalLabel;
    @FXML private Button nuevaOrdenButton, refrescarButton;

    private OrdenProduccionDAO ordenDAO;
    private ObservableList<OrdenProduccion> ordenesList;
    private Timer autoRefreshTimer;

    @FXML
    public void initialize() {
        ordenDAO = new OrdenProduccionDAO();
        ordenesList = FXCollections.observableArrayList();

        configurarTabla();
        configurarFiltros();
        cargarOrdenes();

        nuevaOrdenButton.setOnAction(e -> abrirFormulario(null));
        refrescarButton.setOnAction(e -> cargarOrdenes());

        iniciarAutoRefresh();
    }

    private void iniciarAutoRefresh() {
        autoRefreshTimer = new Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    if (ordenesTable.getScene() != null) cargarOrdenes();
                });
            }
        }, 30000, 30000);
    }

    private void configurarTabla() {
        numColumn.setCellValueFactory(new PropertyValueFactory<>("numeroOrden"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        clienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        horaInicioColumn.setCellValueFactory(new PropertyValueFactory<>("fechaInicioStr"));
        fechaFinColumn.setCellValueFactory(new PropertyValueFactory<>("fechaCompletadoStr"));
        categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        vendedorColumn.setCellValueFactory(new PropertyValueFactory<>("vendedor"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        tipoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("tipoPago"));
        recetaColumn.setCellValueFactory(new PropertyValueFactory<>("nombreReceta"));
        recetaColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText("—");
                    setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });

        estadoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
        estadoPagoColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String est, boolean empty) {
                super.updateItem(est, empty);
                if (empty || est == null) { setText(null); setStyle(""); return; }
                setText(est);
                String bg;
                switch (est) {
                    case "PAGADO": case "Pagado": bg = "#28A745"; break;
                    case "PAGADO_PARCIAL": case "En Proceso": bg = "#FF9800"; break;
                    default: bg = "#DC3545";
                }
                setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
                setAlignment(Pos.CENTER);
            }
        });

        estadoColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String est, boolean empty) {
                super.updateItem(est, empty);
                if (empty || est == null) { setText(null); setStyle(""); return; }
                setText(est);
                String bg;
                switch (est) {
                    case "ACTIVA": bg = "#007BFF"; break;
                    case "EN PRODUCCION": bg = "#FF9800"; break;
                    case "COMPLETADA": bg = "#28A745"; break;
                    case "ENTREGADA": bg = "#6C757D"; break;
                    case "CANCELADA": bg = "#DC3545"; break;
                    default: bg = "#6C757D";
                }
                setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
                setAlignment(Pos.CENTER);
            }
        });

        accionesColumn.setCellFactory(col -> new TableCell<>() {
            private final Button verBtn = new Button("Ver");
            private final Button pagarBtn = new Button("Pagar");
            private final Button recetaBtn = new Button("Asignar Receta");
            private final HBox panel = new HBox(4, verBtn, pagarBtn, recetaBtn);
            {
                verBtn.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                verBtn.setOnAction(e -> abrirDetalle(getTableView().getItems().get(getIndex())));
                pagarBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                recetaBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                OrdenProduccion o = getTableView().getItems().get(getIndex());
                boolean sinReceta = o.getNombreReceta() == null || o.getNombreReceta().isEmpty();
                recetaBtn.setVisible(sinReceta);
                recetaBtn.setManaged(sinReceta);
                recetaBtn.setOnAction(e -> asignarReceta(o));
                boolean tieneSaldo = o.getSaldo() > 0;
                boolean noPagado = !"PAGADO".equals(o.getEstadoPago());
                pagarBtn.setVisible(tieneSaldo && noPagado);
                pagarBtn.setManaged(tieneSaldo && noPagado);
                pagarBtn.setOnAction(e -> abrirPago(o));
                setGraphic(panel);
            }
        });

        ordenesTable.setRowFactory(tv -> {
            TableRow<OrdenProduccion> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) abrirDetalle(row.getItem());
            });
            return row;
        });
    }

    private void configurarFiltros() {
        filtroEstadoCombo.getItems().addAll("Todas", "ACTIVA", "EN PRODUCCION", "COMPLETADA", "ENTREGADA", "CANCELADA");
        filtroEstadoCombo.getSelectionModel().selectFirst();
        filtroEstadoCombo.setOnAction(e -> cargarOrdenes());

        buscarField.textProperty().addListener((obs, old, val) -> cargarOrdenes());
    }

    private void cargarOrdenes() {
        String filtroEstado = filtroEstadoCombo.getValue();
        String busqueda = buscarField.getText().toLowerCase().trim();

        java.util.List<OrdenProduccion> todas;
        if (filtroEstado == null || "Todas".equals(filtroEstado)) {
            todas = ordenDAO.listarTodas();
        } else {
            todas = ordenDAO.listarPorEstado(filtroEstado);
        }

        if (!busqueda.isEmpty()) {
            String finalBusqueda = busqueda;
            todas = todas.stream()
                .filter(o -> (o.getNumeroOrden() != null && o.getNumeroOrden().toLowerCase().contains(finalBusqueda))
                    || (o.getCliente() != null && o.getCliente().toLowerCase().contains(finalBusqueda))
                    || (o.getCategoria() != null && o.getCategoria().toLowerCase().contains(finalBusqueda))
                    || (o.getVendedor() != null && o.getVendedor().toLowerCase().contains(finalBusqueda)))
                .toList();
        }

        ordenesList.setAll(todas);
        ordenesTable.setItems(ordenesList);
        totalLabel.setText("Total: " + todas.size() + " ordenes");
    }

    private void asignarReceta(OrdenProduccion orden) {
        Dialog<Receta> dialog = new Dialog<>();
        dialog.setTitle("Asignar Receta");
        dialog.setHeaderText("Selecciona una receta para la orden " + orden.getNumeroOrden());

        ComboBox<Receta> combo = new ComboBox<>();
        combo.setPrefWidth(350);
        java.util.List<Receta> recetas = ordenDAO.listarRecetas();
        combo.getItems().addAll(recetas);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Receta r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getNombreReceta());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Receta r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getNombreReceta());
            }
        });
        if (!recetas.isEmpty()) combo.getSelectionModel().selectFirst();

        VBox content = new VBox(10, new Label("Receta:"), combo);
        content.setPadding(new javafx.geometry.Insets(15));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? combo.getValue() : null);

        dialog.showAndWait().ifPresent(receta -> {
            if (receta == null) return;
            if (ordenDAO.asignarReceta(orden.getId(), receta.getId())) {
                orden.setIdReceta(receta.getId());
                orden.setNombreReceta(receta.getNombreReceta());
                ordenesTable.refresh();
                mostrarMensaje("Receta Asignada", "Receta \"" + receta.getNombreReceta() + "\" asignada a " + orden.getNumeroOrden());
            } else {
                mostrarError("Error", "No se pudo asignar la receta.");
            }
        });
    }

    private void abrirPago(OrdenProduccion orden) {
        if (orden == null) return;
        double saldo = orden.getSaldo();
        if (saldo <= 0) {
            mostrarMensaje("Orden Pagada", "Esta orden ya se encuentra totalmente pagada.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registrar Pago - " + orden.getNumeroOrden());
        dialog.setHeaderText("Registrar pago para el cliente: " + orden.getCliente() + "\nSaldo pendiente: RD$" + String.format("%.2f", saldo));

        TextField montoField = new TextField(String.format("%.2f", saldo).replace(",", "."));
        montoField.setPromptText("Monto");

        ComboBox<String> metodoCombo = new ComboBox<>();
        metodoCombo.getItems().addAll("Efectivo", "Tarjeta de Credito", "Tarjeta de Debito", "Cheque", "Transferencia", "PayPal");
        metodoCombo.setValue("Efectivo");
        metodoCombo.setMaxWidth(Double.MAX_VALUE);

        TextField referenciaField = new TextField();
        referenciaField.setPromptText("Ej. Referencia bancaria, cheque, etc.");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(
            new Label("Monto a pagar (RD$):"),
            montoField,
            new Label("M\u00e9todo de pago:"),
            metodoCombo,
            new Label("Referencia:"),
            referenciaField
        );

        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        montoField.setStyle("-fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #ccc; -fx-border-radius: 4;");
        metodoCombo.setStyle("-fx-padding: 6; -fx-background-radius: 4; -fx-border-color: #ccc; -fx-border-radius: 4;");
        referenciaField.setStyle("-fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #ccc; -fx-border-radius: 4;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String montoStr = montoField.getText().trim();
            String metodo = metodoCombo.getValue();
            String referencia = referenciaField.getText().trim();

            double monto;
            try {
                monto = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                mostrarError("Monto inv\u00e1lido", "El monto ingresado no es un n\u00famero v\u00e1lido.");
                return;
            }

            if (monto <= 0) {
                mostrarError("Monto inv\u00e1lido", "El monto debe ser mayor que cero.");
                return;
            }

            if (monto > saldo) {
                mostrarError("Monto excedido", "El monto a pagar no puede exceder el saldo pendiente de RD$" + String.format("%.2f", saldo));
                return;
            }

            String usuario = SessionManager.getInstance().getUsuarioActual();

            if ("PayPal".equals(metodo)) {
                ejecutarPagoPayPalDesdeLista(orden, monto, usuario);
            } else {
                if (ordenDAO.registrarPagoCompleto(orden.getId(), metodo, referencia, monto, usuario)) {
                    mostrarMensaje("Pago Registrado", "El pago de RD$" + String.format("%.2f", monto) + " ha sido registrado exitosamente.\nLa factura ha sido generada/actualizada.");
                    cargarOrdenes();
                } else {
                    mostrarError("Error", "No se pudo registrar el pago. Verifique los datos o consulte al administrador.");
                }
            }
        }
    }

    private void ejecutarPagoPayPalDesdeLista(OrdenProduccion orden, double monto, String usuario) {
        int idPedido = ordenDAO.asegurarPedidoVinculadoConConex(null, orden.getId());
        if (idPedido <= 0) {
            try (java.sql.Connection c = DatabaseConnection.getInstance().getConnection()) {
                idPedido = ordenDAO.asegurarPedidoVinculadoConConex(c, orden.getId());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error securing connection for PayPal", ex);
            }
        }

        if (idPedido <= 0) {
            mostrarError("Error", "No se pudo vincular la orden con un pedido para el pago con PayPal.");
            return;
        }

        final int finalIdPedido = idPedido;
        final int ordenId = orden.getId();
        final String numOrden = orden.getNumeroOrden();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Pago con PayPal");
        confirm.setHeaderText("Orden " + numOrden);
        confirm.setContentText("Se iniciar\u00e1 el pago de RD$" + String.format("%.2f", monto) + " con PayPal.\nSe abrir\u00e1 el navegador para completar el pago. \u00bfContinuar?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        new Thread(() -> {
            try {
                PayPalService paypal = new PayPalService();
                PayPalService.PayPalCheckoutResult result = paypal.crearCheckoutSession(monto,
                    "Orden " + numOrden + " - Pago de cliente", null, finalIdPedido);

                if (!result.ok) {
                    Platform.runLater(() -> mostrarError("Error", "No se pudo iniciar el pago con PayPal:\n" + result.url));
                    return;
                }

                String finalUrl = result.url;
                Platform.runLater(() -> {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Redirigiendo a PayPal");
                    info.setHeaderText(null);
                    info.setContentText("Se abrir\u00e1 el navegador para completar el pago.\nEspera mientras confirmamos el pago...");
                    info.show();
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new URI(finalUrl));
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error al abrir navegador: {0}", ex.getMessage());
                    }
                });

                boolean confirmado = false;
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(3000);
                    if (paypal.verificarPago(result.sessionId)) {
                        ordenDAO.registrarPagoCompleto(ordenId, "PayPal", "PayPal Checkout Session ID: " + result.sessionId, monto, usuario);
                        confirmado = true;
                        break;
                    }
                }

                boolean finalConfirmado = confirmado;
                Platform.runLater(() -> {
                    if (finalConfirmado) {
                        cargarOrdenes();
                        mostrarMensaje("Pago Exitoso", "El pago de RD$" + String.format("%.2f", monto) + " con PayPal ha sido confirmado y la factura ha sido generada/actualizada.");
                    } else {
                        mostrarMensaje("Pago Pendiente", "El pago no se pudo confirmar en el tiempo esperado. Puede verificar la transacci\u00f3n en PayPal.");
                    }
                });

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en pago PayPal: {0}", e.getMessage());
                Platform.runLater(() -> mostrarError("Error de Pago", "Ocurri\u00f3 un error al procesar el pago:\n" + e.getMessage()));
            }
        }).start();
    }

    private void mostrarMensaje(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void abrirFormulario(OrdenProduccion orden) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/OrdenProduccionForm.fxml"));
            Parent root = loader.load();
            OrdenProduccionFormController controller = loader.getController();
            controller.setOrdenDAO(ordenDAO);
            if (orden != null) controller.setOrden(orden);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 750));
            stage.setTitle(orden == null ? "Nueva Orden de Produccion" : "Editar Orden");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> cargarOrdenes());
            stage.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error abrir formulario: {0}", e.getMessage());
        }
    }

    private void abrirDetalle(OrdenProduccion orden) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/OrdenProduccionDetalle.fxml"));
            Parent root = loader.load();
            OrdenProduccionDetalleController controller = loader.getController();
            controller.setOrdenDAO(ordenDAO);
            controller.setOrdenId(orden.getId());
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 750));
            stage.setTitle("Orden: " + orden.getNumeroOrden());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> cargarOrdenes());
            stage.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error abrir detalle", e);
        }
    }
}
