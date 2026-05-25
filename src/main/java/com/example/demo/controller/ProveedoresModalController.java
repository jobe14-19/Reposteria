package com.example.demo.controller;

import com.example.demo.dao.ProveedorDAO;
import com.example.demo.model.Proveedor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProveedoresModalController {

 @FXML private Label tituloLabel;
 @FXML private TextField nombreField;
 @FXML private TextField contactoField;
 @FXML private TextField telefonoField;
 @FXML private TextField emailField;
 @FXML private TextField direccionField;
 @FXML private Button guardarButton;

 private ProveedorDAO proveedorDAO;
 private Proveedor proveedorActual;
 private boolean esEdicion;

 @FXML
 public void initialize() {}

 public void setProveedorDAO(ProveedorDAO dao) { this.proveedorDAO = dao; }

 public void setProveedor(Proveedor p) {
 this.proveedorActual = p;
 this.esEdicion = true;
 tituloLabel.setText("Editar Proveedor");
 guardarButton.setText("Actualizar");
 nombreField.setText(p.getNombre());
 contactoField.setText(p.getContacto());
 telefonoField.setText(p.getTelefono());
 emailField.setText(p.getEmail());
 direccionField.setText(p.getDireccion());
 }

 @FXML
 private void guardar(ActionEvent event) {
 if (nombreField.getText() == null || nombreField.getText().trim().isEmpty()) {
 mostrarError("Campo requerido", "El nombre del proveedor es obligatorio.");
 return;
 }
 if (esEdicion && proveedorActual != null) {
 proveedorActual.setNombre(nombreField.getText().trim());
 proveedorActual.setContacto(contactoField.getText().trim());
 proveedorActual.setTelefono(telefonoField.getText().trim());
 proveedorActual.setEmail(emailField.getText().trim());
 proveedorActual.setDireccion(direccionField.getText().trim());
 if (proveedorDAO.actualizar(proveedorActual)) cerrar();
 } else {
 Proveedor p = new Proveedor(0, nombreField.getText().trim(), contactoField.getText().trim(),
 telefonoField.getText().trim(), emailField.getText().trim(), direccionField.getText().trim(), "Activo");
 if (proveedorDAO.insertar(p)) cerrar();
 }
 }

 @FXML
 private void cancelar(ActionEvent event) { cerrar(); }

 private void cerrar() {
 ((Stage) guardarButton.getScene().getWindow()).close();
 }

 private void mostrarError(String t, String m) {
 new Alert(Alert.AlertType.ERROR, m).showAndWait();
 }
}
