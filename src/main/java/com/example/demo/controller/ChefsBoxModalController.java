package com.example.demo.controller;

import com.example.demo.dao.ChefBoxDAO;
import com.example.demo.model.ChefBox;
import com.example.demo.model.ChefBox.ChefBoxProducto;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChefsBoxModalController {

    private static final Logger LOGGER = Logger.getLogger(ChefsBoxModalController.class.getName());

    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private TextArea descripcionField;
    @FXML private TextField precioField;
    @FXML private CheckBox disponibleCheckBox;
    @FXML private TableView<ProductoSeleccionable> productosTable;
    @FXML private TableColumn<ProductoSeleccionable, Boolean> seleccionColumn;
    @FXML private TableColumn<ProductoSeleccionable, String> prodNombreColumn;
    @FXML private TableColumn<ProductoSeleccionable, Integer> prodCantidadColumn;
    @FXML private TableColumn<ProductoSeleccionable, Double> prodPrecioColumn;
    @FXML private Button guardarButton;
    @FXML private Button cancelarButton;

    private ChefBoxDAO chefBoxDAO;
    private ChefBox boxEdicion;
    private ObservableList<ProductoSeleccionable> productosList;

    @FXML
    public void initialize() {
        productosList = FXCollections.observableArrayList();
        prodNombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));

        prodPrecioColumn.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        prodPrecioColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        });

        seleccionColumn.setCellValueFactory(data -> data.getValue().seleccionadoProperty());
        seleccionColumn.setCellFactory(CheckBoxTableCell.forTableColumn(seleccionColumn));
        seleccionColumn.setEditable(true);
        productosTable.setEditable(true);

        prodCantidadColumn.setCellValueFactory(data -> data.getValue().cantidadProperty().asObject());
        prodCantidadColumn.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(0, 999, 1);
            {
                spinner.setEditable(true);
                spinner.valueProperty().addListener((obs, old, val) -> {
                    ProductoSeleccionable p = getTableView().getItems().get(getIndex());
                    if (p != null) p.setCantidad(val);
                });
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    ProductoSeleccionable p = getTableView().getItems().get(getIndex());
                    spinner.getValueFactory().setValue(p.getCantidad());
                    setGraphic(spinner);
                }
            }
        });
    }

    public void setChefBoxDAO(ChefBoxDAO dao) {
        this.chefBoxDAO = dao;
        cargarProductos();
    }

    public void setBox(ChefBox box) {
        this.boxEdicion = box;
        tituloLabel.setText("Editar Caja Especial");
        nombreField.setText(box.getNombre());
        descripcionField.setText(box.getDescripcion());
        precioField.setText(String.valueOf(box.getPrecio()));
        disponibleCheckBox.setSelected(box.isDisponible());
        if (chefBoxDAO != null) cargarProductos();
    }

    private void cargarProductos() {
        List<ChefBoxProducto> disponibles = chefBoxDAO.obtenerProductosDisponibles();
        List<ChefBoxProducto> existentes = (boxEdicion != null && boxEdicion.getProductos() != null)
            ? boxEdicion.getProductos() : new ArrayList<>();

        productosList.clear();
        for (ChefBoxProducto disp : disponibles) {
            boolean incluido = false;
            int cantidad = 0;
            for (ChefBoxProducto ext : existentes) {
                if (ext.getIdProducto() == disp.getIdProducto()) {
                    incluido = true;
                    cantidad = ext.getCantidad();
                    break;
                }
            }
            ProductoSeleccionable ps = new ProductoSeleccionable(
                disp.getIdProducto(), disp.getNombreProducto(),
                disp.getPrecioUnitario(), incluido,
                incluido ? cantidad : 1);
            productosList.add(ps);
        }
        productosTable.setItems(productosList);
    }

    @FXML
    private void guardar(ActionEvent event) {
        String nombre = nombreField.getText();
        if (nombre == null || nombre.isBlank()) {
            mostrarError("Campo requerido", "El nombre de la caja es obligatorio.");
            nombreField.requestFocus();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioField.getText().trim());
        } catch (NumberFormatException e) {
            mostrarError("Campo invalido", "Ingrese un precio valido.");
            precioField.requestFocus();
            return;
        }

        List<ChefBoxProducto> seleccionados = productosList.stream()
            .filter(ProductoSeleccionable::isSeleccionado)
            .map(p -> new ChefBoxProducto(p.getIdProducto(), p.getNombreProducto(),
                p.getCantidad(), p.getPrecioUnitario()))
            .collect(Collectors.toList());

        if (seleccionados.isEmpty()) {
            mostrarError("Sin productos", "Debe incluir al menos un producto en la caja.");
            return;
        }

        if (boxEdicion == null) {
            ChefBox nueva = new ChefBox(0, nombre.trim(), descripcionField.getText(),
                precio, disponibleCheckBox.isSelected(), null, "Activo", 0);
            int id = chefBoxDAO.insertar(nueva, seleccionados);
            if (id > 0) cerrar();
        } else {
            boxEdicion.setNombre(nombre.trim());
            boxEdicion.setDescripcion(descripcionField.getText());
            boxEdicion.setPrecio(precio);
            boxEdicion.setDisponible(disponibleCheckBox.isSelected());
            if (chefBoxDAO.actualizar(boxEdicion, seleccionados)) cerrar();
        }
    }

    @FXML
    private void cancelar(ActionEvent event) {
        cerrar();
    }

    private void cerrar() {
        ((Stage) guardarButton.getScene().getWindow()).close();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ProductoSeleccionable {
        private final int idProducto;
        private final String nombreProducto;
        private final double precioUnitario;
        private final SimpleBooleanProperty seleccionado;
        private final SimpleIntegerProperty cantidad;

        public ProductoSeleccionable(int idProducto, String nombreProducto, double precioUnitario,
                                      boolean seleccionado, int cantidad) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
            this.precioUnitario = precioUnitario;
            this.seleccionado = new SimpleBooleanProperty(seleccionado);
            this.cantidad = new SimpleIntegerProperty(cantidad);
            this.seleccionado.addListener((obs, old, val) -> {
                if (!val) this.cantidad.set(0);
                else if (this.cantidad.get() <= 0) this.cantidad.set(1);
            });
        }

        public int getIdProducto() { return idProducto; }
        public String getNombreProducto() { return nombreProducto; }
        public double getPrecioUnitario() { return precioUnitario; }
        public boolean isSeleccionado() { return seleccionado.get(); }
        public SimpleBooleanProperty seleccionadoProperty() { return seleccionado; }
        public void setSeleccionado(boolean s) { seleccionado.set(s); }
        public int getCantidad() { return cantidad.get(); }
        public SimpleIntegerProperty cantidadProperty() { return cantidad; }
        public void setCantidad(int c) { cantidad.set(c); }
    }
}
