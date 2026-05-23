package com.example.demo.controller;

import com.example.demo.dao.ProductoDAO;
import com.example.demo.model.Producto;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class ProductoModalController {

    private static final Logger LOGGER = Logger.getLogger(ProductoModalController.class.getName());

    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private TextArea descripcionField;
    @FXML private TextField precioBaseField;
    @FXML private TextField precioUnitarioField;
    @FXML private TextField costoDisenioField;
    @FXML private Button guardarButton;

    private ProductoDAO productoDAO;
    private Producto productoEdicion;

    public void setProductoDAO(ProductoDAO dao) { this.productoDAO = dao; }

    public void setProducto(Producto p) {
        this.productoEdicion = p;
        tituloLabel.setText("Editar Producto");
        nombreField.setText(p.getNombre());
        descripcionField.setText(p.getDescripcion());
        precioBaseField.setText(String.valueOf(p.getPrecioBase()));
        precioUnitarioField.setText(String.valueOf(p.getPrecioUnitario()));
        costoDisenioField.setText(String.valueOf(p.getCostoDisenio()));
    }

    @FXML
    private void guardar(ActionEvent event) {
        String nombre = nombreField.getText();
        if (nombre == null || nombre.isBlank()) {
            mostrarError("Campo requerido", "El nombre del producto es obligatorio.");
            nombreField.requestFocus();
            return;
        }
        double precioBase, precioUnitario, costoDisenio;
        try {
            precioBase = Double.parseDouble(precioBaseField.getText().trim());
            precioUnitario = Double.parseDouble(precioUnitarioField.getText().trim());
            costoDisenio = Double.parseDouble(costoDisenioField.getText().trim());
        } catch (NumberFormatException e) {
            mostrarError("Campo invalido", "Ingrese valores numericos validos en los precios.");
            return;
        }
        if (precioBase < 0 || precioUnitario < 0 || costoDisenio < 0) {
            mostrarError("Valor invalido", "Los precios no pueden ser negativos.");
            return;
        }

        if (productoEdicion == null) {
            Producto nuevo = new Producto(0, nombre.trim(), precioBase, precioUnitario, costoDisenio,
                descripcionField.getText().trim(), "Activo", 0);
            int id = productoDAO.insertar(nuevo);
            if (id > 0) cerrar();
        } else {
            productoEdicion.setNombre(nombre.trim());
            productoEdicion.setDescripcion(descripcionField.getText().trim());
            productoEdicion.setPrecioBase(precioBase);
            productoEdicion.setPrecioUnitario(precioUnitario);
            productoEdicion.setCostoDisenio(costoDisenio);
            if (productoDAO.actualizar(productoEdicion)) cerrar();
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
}
