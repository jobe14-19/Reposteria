package com.example.demo.controller;

import com.example.demo.dao.ProductoDAO;
import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Producto;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RecetaModalController {

    private static final Logger LOGGER = Logger.getLogger(RecetaModalController.class.getName());

    @FXML private Label tituloLabel;
    @FXML private ComboBox<Producto> productoComboBox;
    @FXML private TextArea descripcionField;
    @FXML private Spinner<Double> porcionesSpinner;
    @FXML private TableView<IngredienteItem> ingredientesTable;
    @FXML private TableColumn<IngredienteItem, Boolean> seleccionColumn;
    @FXML private TableColumn<IngredienteItem, String> ingNombreColumn;
    @FXML private TableColumn<IngredienteItem, Double> ingCantidadColumn;
    @FXML private TableColumn<IngredienteItem, String> ingUnidadColumn;
    @FXML private Button guardarButton;

    private RecetaDAO recetaDAO;
    private Receta recetaEdicion;
    private ObservableList<IngredienteItem> ingredientesList;
    private boolean datosCargados = false;

    public void setRecetaDAO(RecetaDAO dao) { this.recetaDAO = dao; }

    public void setReceta(Receta r) {
        this.recetaEdicion = r;
        if (r != null) {
            tituloLabel.setText("Editar Receta");
            descripcionField.setText(r.getDescripcion());
            porcionesSpinner.getValueFactory().setValue(r.getPorciones());
            if (datosCargados) {
                seleccionarProducto(r.getIdProducto());
                marcarIngredientesReceta(r);
            }
        }
    }

    @FXML
    public void initialize() {
        porcionesSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 100.0, 1.0, 0.5));
        porcionesSpinner.setEditable(true);

        productoComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Producto p) { return p == null ? "" : p.getNombre(); }
            @Override
            public Producto fromString(String s) { return null; }
        });

        ingNombreColumn.setCellValueFactory(data -> data.getValue().nombreProperty());
        ingUnidadColumn.setCellValueFactory(data -> data.getValue().unidadProperty());

        seleccionColumn.setCellValueFactory(data -> data.getValue().seleccionadoProperty());
        seleccionColumn.setCellFactory(CheckBoxTableCell.forTableColumn(seleccionColumn));
        seleccionColumn.setEditable(true);
        ingredientesTable.setEditable(true);

        ingCantidadColumn.setCellValueFactory(data -> data.getValue().cantidadProperty().asObject());
        ingCantidadColumn.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Double> spinner = new Spinner<>(0.0, 9999.0, 0.0, 0.5);
            {
                spinner.setEditable(true);
                spinner.valueProperty().addListener((obs, old, val) -> {
                    IngredienteItem item = getTableView().getItems().get(getIndex());
                    if (item != null) item.setCantidad(val);
                });
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                IngredienteItem i = getTableView().getItems().get(getIndex());
                spinner.getValueFactory().setValue(i.getCantidad());
                setGraphic(spinner);
            }
        });

        ingredientesList = FXCollections.observableArrayList();
        ProductoDAO productoDAO = new ProductoDAO();
        List<Producto> productos = productoDAO.listarTodos();
        productoComboBox.setItems(FXCollections.observableArrayList(productos));

        cargarIngredientesDisponibles();

        if (recetaEdicion != null) {
            tituloLabel.setText("Editar Receta");
            descripcionField.setText(recetaEdicion.getDescripcion());
            porcionesSpinner.getValueFactory().setValue(recetaEdicion.getPorciones());
            seleccionarProducto(recetaEdicion.getIdProducto());
            marcarIngredientesReceta(recetaEdicion);
        }

        datosCargados = true;
    }

    private void seleccionarProducto(int idProducto) {
        for (Producto p : productoComboBox.getItems()) {
            if (p.getId() == idProducto) {
                productoComboBox.getSelectionModel().select(p);
                break;
            }
        }
    }

    private void marcarIngredientesReceta(Receta r) {
        if (r.getIngredientes() != null) {
            for (RecetaIngrediente ri : r.getIngredientes()) {
                for (IngredienteItem item : ingredientesList) {
                    if (item.getIdIngrediente() == ri.getIdIngrediente()) {
                        item.setSeleccionado(true);
                        item.setCantidad(ri.getCantidad());
                        break;
                    }
                }
            }
        }
    }

    private void cargarIngredientesDisponibles() {
        if (recetaDAO == null) recetaDAO = new RecetaDAO();
        List<RecetaIngrediente> disponibles = recetaDAO.obtenerIngredientesDisponibles();
        for (RecetaIngrediente ri : disponibles) {
            ingredientesList.add(new IngredienteItem(ri.getIdIngrediente(),
                ri.getNombreIngrediente(), 0.0, ri.getUnidad(), false));
        }
        ingredientesTable.setItems(ingredientesList);
    }

    @FXML
    private void guardar(ActionEvent event) {
        Producto producto = productoComboBox.getValue();
        if (producto == null) {
            mostrarError("Campo requerido", "Seleccione un producto.");
            return;
        }

        List<IngredienteItem> seleccionados = ingredientesList.stream()
            .filter(IngredienteItem::isSeleccionado)
            .filter(i -> i.getCantidad() > 0)
            .collect(Collectors.toList());

        if (seleccionados.isEmpty()) {
            mostrarError("Sin ingredientes", "Debe seleccionar al menos un ingrediente con cantidad mayor a 0.");
            return;
        }

        List<RecetaIngrediente> ingredientes = seleccionados.stream()
            .map(i -> new RecetaIngrediente(i.getIdIngrediente(), i.getNombre(),
                i.getCantidad(), i.getUnidad()))
            .collect(Collectors.toList());

        java.util.List<Receta.PasoReceta> pasosVacio = new java.util.ArrayList<>();
        if (recetaEdicion == null) {
            Receta nueva = new Receta();
            nueva.setIdProducto(producto.getId());
            nueva.setNombreProducto(producto.getNombre());
            nueva.setNombreReceta(producto.getNombre() + " (Receta)");
            nueva.setDescripcion(descripcionField.getText().trim());
            nueva.setCategoria("Otros");
            nueva.setPorciones(porcionesSpinner.getValue());
            nueva.setCantidadProducida(porcionesSpinner.getValue());
            nueva.setEstado("Activo");
            int id = recetaDAO.insertar(nueva, ingredientes, pasosVacio);
            if (id > 0) cerrar();
            else mostrarError("Error", "No se pudo crear la receta.");
        } else {
            recetaEdicion.setIdProducto(producto.getId());
            recetaEdicion.setNombreProducto(producto.getNombre());
            recetaEdicion.setDescripcion(descripcionField.getText().trim());
            recetaEdicion.setPorciones(porcionesSpinner.getValue());
            if (recetaDAO.actualizar(recetaEdicion, ingredientes, pasosVacio)) cerrar();
            else mostrarError("Error", "No se pudo actualizar la receta.");
        }
    }

    @FXML
    private void cancelar(ActionEvent event) { cerrar(); }

    private void cerrar() { ((Stage) guardarButton.getScene().getWindow()).close(); }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class IngredienteItem {
        private final int idIngrediente;
        private final SimpleStringProperty nombre;
        private final SimpleDoubleProperty cantidad;
        private final SimpleStringProperty unidad;
        private final SimpleBooleanProperty seleccionado;

        public IngredienteItem(int idIngrediente, String nombre, double cantidad, String unidad, boolean seleccionado) {
            this.idIngrediente = idIngrediente;
            this.nombre = new SimpleStringProperty(nombre);
            this.cantidad = new SimpleDoubleProperty(cantidad);
            this.unidad = new SimpleStringProperty(unidad);
            this.seleccionado = new SimpleBooleanProperty(seleccionado);
        }

        public int getIdIngrediente() { return idIngrediente; }
        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public double getCantidad() { return cantidad.get(); }
        public SimpleDoubleProperty cantidadProperty() { return cantidad; }
        public void setCantidad(double c) { cantidad.set(c); }
        public String getUnidad() { return unidad.get(); }
        public SimpleStringProperty unidadProperty() { return unidad; }
        public boolean isSeleccionado() { return seleccionado.get(); }
        public SimpleBooleanProperty seleccionadoProperty() { return seleccionado; }
        public void setSeleccionado(boolean s) { seleccionado.set(s); }
    }
}
