package com.example.demo.controller;

import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.OrdenProduccion.OrdenIngrediente;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;
import com.example.demo.service.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrdenProduccionFormController {

    private static final Logger LOGGER = Logger.getLogger(OrdenProduccionFormController.class.getName());

    @FXML private Label tituloLabel, numOrdenLabel;
    @FXML private ComboBox<String> categoriaCombo, revestimientoCombo, sucursalCombo, baseCombo, masaCombo, formaCombo;
    @FXML private ComboBox<Receta> recetaCombo;
    @FXML private DatePicker fechaPicker;
    @FXML private TextField horaField, clienteField, direccionField, telefonoField, vendedorField;
    @FXML private TextField librasField, pisosField, lustresField, camuflajesField, floresField;
    @FXML private TextField mensajeField, adornosField, rellenosField;
    @FXML private TextField costoEstField, costoRealField, precioField, anticipoField, saldoField;
    @FXML private TextArea observacionesArea, decoracionArea;
    @FXML private TableView<IngredienteResumen> ingredientesTable;
    @FXML private TableColumn<IngredienteResumen, String> ingNombreCol, ingUnidadCol, ingCantidadCol;
    @FXML private Label stockWarningLabel;
    @FXML private Button guardarButton, cancelarButton, cargarRecetaButton;

    private OrdenProduccionDAO ordenDAO;
    private OrdenProduccion ordenEdicion;

    public void setOrdenDAO(OrdenProduccionDAO dao) { this.ordenDAO = dao; }

    public void setOrden(OrdenProduccion o) {
        this.ordenEdicion = o;
        if (o != null) cargarOrdenParaEdicion();
    }

    @FXML
    public void initialize() {
        if (ordenDAO == null) ordenDAO = new OrdenProduccionDAO();

        categoriaCombo.getItems().addAll("Pastel", "Galleta", "Postre", "Pan", "Reposteria Fina", "Decoracion");
        revestimientoCombo.getItems().addAll("Fondant", "Buttercream", "Ganache", "Merengue", "Crema", "Ninguno");
        sucursalCombo.getItems().addAll("Principal", "Sucursal Norte", "Sucursal Sur", "Sucursal Este");
        baseCombo.getItems().addAll("Bizcocho Vainilla", "Bizcocho Chocolate", "Bizcocho Red Velvet", "Bizcocho Zanahoria", "Bizcocho Limon", "Base Galleta");
        masaCombo.getItems().addAll("Masa Suave", "Masa Firme", "Masa Quebrada", "Masa Hojaldre", "Masa Choux");
        formaCombo.getItems().addAll("Redonda", "Cuadrada", "Rectangular", "Corazon", "Personalizada");

        configurarTablaIngredientes();
        configurarRecetaCombo();

        categoriaCombo.getSelectionModel().selectFirst();
        revestimientoCombo.getSelectionModel().selectFirst();
        sucursalCombo.getSelectionModel().selectFirst();
        baseCombo.getSelectionModel().selectFirst();
        masaCombo.getSelectionModel().selectFirst();
        formaCombo.getSelectionModel().selectFirst();

        if (ordenEdicion == null) {
            numOrdenLabel.setText("Nueva — " + ordenDAO.generarNumeroOrden());
            tituloLabel.setText("Nueva Orden de Produccion");
        }

        calcularSaldo();
    }

    private void configurarRecetaCombo() {
        List<Receta> recetas = ordenDAO.listarRecetas();
        recetaCombo.setItems(FXCollections.observableArrayList(recetas));
        recetaCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Receta r) { return r == null ? "" : r.getNombreReceta() + " (" + r.getNombreProducto() + ")"; }
            @Override public Receta fromString(String s) { return null; }
        });

        cargarRecetaButton.setOnAction(e -> {
            Receta r = recetaCombo.getValue();
            if (r == null) return;
            Receta completa = ordenDAO.listarRecetas().stream().filter(x -> x.getId() == r.getId()).findFirst().orElse(null);
            if (completa == null) return;
            completa = new com.example.demo.dao.RecetaDAO().obtenerPorId(r.getId());
            if (completa == null) return;

            categoriaCombo.setValue(completa.getCategoria() != null ? completa.getCategoria() : categoriaCombo.getValue());
            librasField.setText(String.valueOf(completa.getCantidadProducida()));
            costoEstField.setText(String.valueOf(completa.getCostoEstimado()));

            ObservableList<IngredienteResumen> ings = FXCollections.observableArrayList();
            for (RecetaIngrediente ri : completa.getIngredientes()) {
                ings.add(new IngredienteResumen(ri.getNombreIngrediente(), ri.getCantidad(), ri.getUnidad()));
            }
            ingredientesTable.setItems(ings);
            stockWarningLabel.setVisible(false);
        });
    }

    private void configurarTablaIngredientes() {
        ingNombreCol.setCellValueFactory(d -> d.getValue().nombreProperty());
        ingCantidadCol.setCellValueFactory(d -> d.getValue().cantidadStrProperty());
        ingUnidadCol.setCellValueFactory(d -> d.getValue().unidadProperty());
    }

    private void calcularSaldo() {
        precioField.textProperty().addListener((obs, o, n) -> actualizarSaldo());
        anticipoField.textProperty().addListener((obs, o, n) -> actualizarSaldo());
    }

    private void actualizarSaldo() {
        try {
            double precio = Double.parseDouble(precioField.getText().trim().isEmpty() ? "0" : precioField.getText().trim());
            double anticipo = Double.parseDouble(anticipoField.getText().trim().isEmpty() ? "0" : anticipoField.getText().trim());
            saldoField.setText(String.format("%.2f", precio - anticipo));
        } catch (NumberFormatException e) {
            saldoField.setText("0.00");
        }
    }

    private void cargarOrdenParaEdicion() {
        tituloLabel.setText("Editar Orden: " + ordenEdicion.getNumeroOrden());
        numOrdenLabel.setText(ordenEdicion.getNumeroOrden());
        if (ordenEdicion.getCategoria() != null) categoriaCombo.setValue(ordenEdicion.getCategoria());
        if (ordenEdicion.getRevestimiento() != null) revestimientoCombo.setValue(ordenEdicion.getRevestimiento());
        if (ordenEdicion.getSucursal() != null) sucursalCombo.setValue(ordenEdicion.getSucursal());
        if (ordenEdicion.getBaseTipo() != null) baseCombo.setValue(ordenEdicion.getBaseTipo());
        if (ordenEdicion.getMaso() != null) masaCombo.setValue(ordenEdicion.getMaso());
        if (ordenEdicion.getForma() != null) formaCombo.setValue(ordenEdicion.getForma());
        clienteField.setText(ordenEdicion.getCliente());
        direccionField.setText(ordenEdicion.getDireccion());
        telefonoField.setText(ordenEdicion.getTelefono());
        vendedorField.setText(ordenEdicion.getVendedor());
        librasField.setText(String.valueOf(ordenEdicion.getLibras()));
        pisosField.setText(String.valueOf(ordenEdicion.getPisos()));
        lustresField.setText(ordenEdicion.getLustres());
        camuflajesField.setText(ordenEdicion.getCamuflajes());
        floresField.setText(ordenEdicion.getFlores());
        mensajeField.setText(ordenEdicion.getMensaje());
        adornosField.setText(ordenEdicion.getAdornos());
        rellenosField.setText(ordenEdicion.getRellenos());
        decoracionArea.setText(ordenEdicion.getDecoracion());
        observacionesArea.setText(ordenEdicion.getObservaciones());
        costoEstField.setText(String.valueOf(ordenEdicion.getCostoEstimado()));
        costoRealField.setText(String.valueOf(ordenEdicion.getCostoReal()));
        precioField.setText(String.valueOf(ordenEdicion.getPrecioVenta()));
        anticipoField.setText(String.valueOf(ordenEdicion.getAnticipo()));
        saldoField.setText(String.format("%.2f", ordenEdicion.getPrecioVenta() - ordenEdicion.getAnticipo()));
    }

    @FXML
    private void guardar() {
        if (clienteField.getText() == null || clienteField.getText().trim().isEmpty()) {
            mostrarError("Campo requerido", "El nombre del cliente es obligatorio.");
            return;
        }

        OrdenProduccion o = ordenEdicion != null ? ordenEdicion : new OrdenProduccion();
        if (ordenEdicion == null) {
            o.setNumeroOrden(ordenDAO.generarNumeroOrden());
            o.setUsuarioCrea(SessionManager.getInstance().getUsuarioActual());
        }
        o.setCategoria(categoriaCombo.getValue());
        o.setRevestimiento(revestimientoCombo.getValue());
        o.setSucursal(sucursalCombo.getValue());
        o.setFechaEntrega(fechaPicker.getValue() != null ? fechaPicker.getValue().toString() : null);
        o.setHoraEntrega(horaField.getText());
        o.setCliente(clienteField.getText().trim());
        o.setDireccion(direccionField.getText());
        o.setTelefono(telefonoField.getText());
        o.setVendedor(vendedorField.getText());
        try { o.setLibras(Double.parseDouble(librasField.getText().trim())); } catch (NumberFormatException e) { o.setLibras(0); }
        o.setBaseTipo(baseCombo.getValue());
        o.setMaso(masaCombo.getValue());
        o.setForma(formaCombo.getValue());
        try { o.setPisos(Integer.parseInt(pisosField.getText().trim())); } catch (NumberFormatException e) { o.setPisos(1); }
        o.setLustres(lustresField.getText());
        o.setDecoracion(decoracionArea.getText());
        o.setCamuflajes(camuflajesField.getText());
        o.setFlores(floresField.getText());
        o.setMensaje(mensajeField.getText());
        o.setObservaciones(observacionesArea.getText());
        o.setAdornos(adornosField.getText());
        o.setRellenos(rellenosField.getText());
        try { o.setCostoEstimado(Double.parseDouble(costoEstField.getText().trim())); } catch (NumberFormatException e) { o.setCostoEstimado(0); }
        try { o.setCostoReal(Double.parseDouble(costoRealField.getText().trim())); } catch (NumberFormatException e) { o.setCostoReal(0); }
        try { o.setPrecioVenta(Double.parseDouble(precioField.getText().trim())); } catch (NumberFormatException e) { o.setPrecioVenta(0); }
        try { o.setAnticipo(Double.parseDouble(anticipoField.getText().trim())); } catch (NumberFormatException e) { o.setAnticipo(0); }
        try { o.setSaldo(Double.parseDouble(saldoField.getText().trim())); } catch (NumberFormatException e) { o.setSaldo(0); }
        if (recetaCombo.getValue() != null) o.setIdReceta(recetaCombo.getValue().getId());

        List<OrdenIngrediente> ingredientes = null;
        if (ingredientesTable.getItems() != null && !ingredientesTable.getItems().isEmpty()) {
            ingredientes = ingredientesTable.getItems().stream()
                .map(i -> new OrdenIngrediente(0, i.getNombre(), i.getUnidad(), i.getCantidad()))
                .collect(Collectors.toList());
        }
        o.setIngredientes(ingredientes);

        boolean ok;
        if (ordenEdicion == null) {
            ok = ordenDAO.insertar(o) > 0;
        } else {
            ok = ordenDAO.actualizar(o);
        }

        if (ok) cerrar();
        else mostrarError("Error", "No se pudo guardar la orden.");
    }

    @FXML private void cancelar() { cerrar(); }
    private void cerrar() { ((Stage) cancelarButton.getScene().getWindow()).close(); }
    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    public static class IngredienteResumen {
        private final javafx.beans.property.SimpleStringProperty nombre, unidad;
        private final double cantidad;
        public IngredienteResumen(String nombre, double cantidad, String unidad) {
            this.nombre = new javafx.beans.property.SimpleStringProperty(nombre);
            this.cantidad = cantidad;
            this.unidad = new javafx.beans.property.SimpleStringProperty(unidad);
        }
        public String getNombre() { return nombre.get(); }
        public javafx.beans.property.SimpleStringProperty nombreProperty() { return nombre; }
        public double getCantidad() { return cantidad; }
        public String getUnidad() { return unidad.get(); }
        public javafx.beans.property.SimpleStringProperty unidadProperty() { return unidad; }
        public String getCantidadStr() { return String.format("%.2f", cantidad); }
        public javafx.beans.property.SimpleStringProperty cantidadStrProperty() {
            return new javafx.beans.property.SimpleStringProperty(getCantidadStr());
        }
    }
}
