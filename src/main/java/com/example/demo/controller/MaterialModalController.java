package com.example.demo.controller;

import com.example.demo.dao.MaterialDAO;
import com.example.demo.model.Material;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialModalController {

    private static final Logger LOGGER = Logger.getLogger(MaterialModalController.class.getName());

    @FXML private TextField nombreField;
    @FXML private TextField unidadField;
    @FXML private TextField stockActualField;
    @FXML private TextField stockMinimoField;
    @FXML private Button agregarButton;
    @FXML private Button actualizarButton;
    @FXML private Button limpiarFormButton;
    @FXML private TableView<Material> materialesTable;
    @FXML private TableColumn<Material, String> nombreColumn;
    @FXML private TableColumn<Material, String> unidadColumn;
    @FXML private TableColumn<Material, Integer> stockActualColumn;
    @FXML private TableColumn<Material, Integer> stockMinimoColumn;
    @FXML private TableColumn<Material, Void> accionColumn;

    private MaterialDAO materialDAO;
    private ObservableList<Material> materialesList;
    private Material materialSeleccionado;

    @FXML
    public void initialize() {
        materialDAO = new MaterialDAO();
        materialDAO.crearTablaSiNoExiste();

        configurarTabla();
        cargarMateriales();
        setupSelectionListener();
        actualizarButton.setDisable(true);
    }

    private void configurarTabla() {
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        unidadColumn.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        stockActualColumn.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        stockMinimoColumn.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        accionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnEliminar = new Button("Eliminar");
            {
                btnEliminar.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
                btnEliminar.setOnAction(event -> {
                    Material material = getTableView().getItems().get(getIndex());
                    eliminarMaterial(material);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });
    }

    private void cargarMateriales() {
        materialesList = FXCollections.observableArrayList(materialDAO.obtenerTodos());
        materialesTable.setItems(materialesList);
    }

    private void setupSelectionListener() {
        materialesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                materialSeleccionado = selected;
                nombreField.setText(selected.getNombre());
                unidadField.setText(selected.getUnidad());
                stockActualField.setText(String.valueOf(selected.getStockActual()));
                stockMinimoField.setText(String.valueOf(selected.getStockMinimo()));
                actualizarButton.setDisable(false);
            } else {
                limpiarFormulario();
            }
        });
    }

    @FXML
    private void agregarMaterial() {
        if (!validarCampos()) return;

        Material material = new Material();
        material.setNombre(nombreField.getText().trim());
        material.setUnidad(unidadField.getText().trim());
        material.setStockActual(parseInt(stockActualField.getText(), 0));
        material.setStockMinimo(parseInt(stockMinimoField.getText(), 1));

        if (materialDAO.insertar(material)) {
            cargarMateriales();
            limpiarFormulario();
            mostrarMensaje("Material agregado", "El material ha sido registrado correctamente.");
        } else {
            mostrarError("Error", "No se pudo agregar el material.");
        }
    }

    @FXML
    private void actualizarMaterial() {
        if (materialSeleccionado == null || !validarCampos()) return;

        materialSeleccionado.setNombre(nombreField.getText().trim());
        materialSeleccionado.setUnidad(unidadField.getText().trim());
        materialSeleccionado.setStockActual(parseInt(stockActualField.getText(), 0));
        materialSeleccionado.setStockMinimo(parseInt(stockMinimoField.getText(), 1));

        if (materialDAO.actualizar(materialSeleccionado)) {
            cargarMateriales();
            limpiarFormulario();
            mostrarMensaje("Material actualizado", "El material ha sido actualizado correctamente.");
        } else {
            mostrarError("Error", "No se pudo actualizar el material.");
        }
    }

    private void eliminarMaterial(Material material) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Está seguro de eliminar el material \"" + material.getNombre() + "\"?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (materialDAO.eliminar(material.getId())) {
                cargarMateriales();
                limpiarFormulario();
                mostrarMensaje("Material eliminado", "El material ha sido eliminado.");
            } else {
                mostrarError("Error", "No se pudo eliminar el material.");
            }
        }
    }

    @FXML
    private void limpiarFormulario() {
        nombreField.clear();
        unidadField.clear();
        stockActualField.clear();
        stockMinimoField.clear();
        materialSeleccionado = null;
        actualizarButton.setDisable(true);
        materialesTable.getSelectionModel().clearSelection();
    }

    private boolean validarCampos() {
        if (nombreField.getText() == null || nombreField.getText().isBlank()) {
            mostrarError("Campo requerido", "El nombre del material es obligatorio.");
            nombreField.requestFocus();
            return false;
        }
        if (unidadField.getText() == null || unidadField.getText().isBlank()) {
            unidadField.setText("unidad");
        }
        return true;
    }

    private int parseInt(String text, int defaultValue) {
        try {
            return (text != null && !text.isBlank()) ? Integer.parseInt(text.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) materialesTable.getScene().getWindow();
        stage.close();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
