package com.example.demo.controller;

import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.model.OrdenProduccion;
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

import java.util.logging.Level;
import java.util.logging.Logger;

public class OrdenesProduccionController {

    private static final Logger LOGGER = Logger.getLogger(OrdenesProduccionController.class.getName());

    @FXML private TableView<OrdenProduccion> ordenesTable;
    @FXML private TableColumn<OrdenProduccion, String> numColumn, estadoColumn, clienteColumn;
    @FXML private TableColumn<OrdenProduccion, String> fechaColumn, categoriaColumn, vendedorColumn;
    @FXML private TableColumn<OrdenProduccion, Double> librasColumn, totalColumn;
    @FXML private TableColumn<OrdenProduccion, Void> accionesColumn;
    @FXML private ComboBox<String> filtroEstadoCombo;
    @FXML private TextField buscarField;
    @FXML private Label totalLabel;
    @FXML private Button nuevaOrdenButton, refrescarButton;

    private OrdenProduccionDAO ordenDAO;
    private ObservableList<OrdenProduccion> ordenesList;

    @FXML
    public void initialize() {
        ordenDAO = new OrdenProduccionDAO();
        ordenesList = FXCollections.observableArrayList();

        configurarTabla();
        configurarFiltros();
        cargarOrdenes();

        nuevaOrdenButton.setOnAction(e -> abrirFormulario(null));
        refrescarButton.setOnAction(e -> cargarOrdenes());
    }

    private void configurarTabla() {
        numColumn.setCellValueFactory(new PropertyValueFactory<>("numeroOrden"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        clienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        vendedorColumn.setCellValueFactory(new PropertyValueFactory<>("vendedor"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));

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
            { verBtn.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
              verBtn.setOnAction(e -> abrirDetalle(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : verBtn);
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
            LOGGER.log(Level.SEVERE, "Error abrir detalle: {0}", e.getMessage());
        }
    }
}
