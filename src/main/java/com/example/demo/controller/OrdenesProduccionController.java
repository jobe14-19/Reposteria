package com.example.demo.controller;

import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.Receta;
import com.example.demo.service.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

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
        estadoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
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
            private final Button recetaBtn = new Button("Asignar Receta");
            private final HBox panel = new HBox(4, verBtn, recetaBtn);
            {
                verBtn.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                verBtn.setOnAction(e -> abrirDetalle(getTableView().getItems().get(getIndex())));
                recetaBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                OrdenProduccion o = getTableView().getItems().get(getIndex());
                boolean sinReceta = o.getNombreReceta() == null || o.getNombreReceta().isEmpty();
                recetaBtn.setVisible(sinReceta);
                recetaBtn.setManaged(sinReceta);
                recetaBtn.setOnAction(e -> asignarReceta(o));
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
