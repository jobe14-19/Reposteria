package com.example.demo.controller;

import com.example.demo.dao.UsuarioAdminDAO;
import com.example.demo.model.UsuarioAdmin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class UsuariosModalController {

 @FXML private Label tituloLabel;
 @FXML private TextField usuarioField;
 @FXML private PasswordField contrasenaField;
 @FXML private TextField nombreField;
 @FXML private ComboBox<String> perfilCombo;
 @FXML private Button guardarButton;

 private UsuarioAdminDAO usuarioDAO;
 private UsuarioAdmin usuarioActual;
 private boolean esEdicion;

 private static final String[] PERFILES = {
 "ADMIN", "RECEPCION", "PLANIFICADOR", "ALMACEN",
 "PRODUCCION", "DECORACION", "CONTABILIDAD", "REPARTIDOR", "RRHH", "AUDITOR", "CLIENTE"
 };

 @FXML
 public void initialize() {
 perfilCombo.getItems().addAll(PERFILES);
 perfilCombo.getSelectionModel().selectFirst();
 }

 public void setUsuarioDAO(UsuarioAdminDAO dao) { this.usuarioDAO = dao; }

 public void setUsuario(UsuarioAdmin u) {
 this.usuarioActual = u;
 this.esEdicion = true;
 tituloLabel.setText("Editar Usuario");
 guardarButton.setText("Actualizar");
 usuarioField.setText(u.getUsuario());
 nombreField.setText(u.getNombre());
 perfilCombo.getSelectionModel().select(u.getPerfil());
 contrasenaField.setManaged(false);
 contrasenaField.setVisible(false);
 }

 @FXML
 private void guardar(ActionEvent event) {
 if (usuarioField.getText() == null || usuarioField.getText().trim().isEmpty()) {
 mostrarError("Campo requerido", "El nombre de usuario es obligatorio.");
 return;
 }
 if (nombreField.getText() == null || nombreField.getText().trim().isEmpty()) {
 mostrarError("Campo requerido", "El nombre completo es obligatorio.");
 return;
 }
 if (perfilCombo.getValue() == null) {
 mostrarError("Campo requerido", "Seleccione un perfil.");
 return;
 }

 if (esEdicion && usuarioActual != null) {
 usuarioActual.setUsuario(usuarioField.getText().trim());
 usuarioActual.setNombre(nombreField.getText().trim());
 usuarioActual.setPerfil(perfilCombo.getValue());
 if (usuarioDAO.actualizar(usuarioActual)) cerrar();
 } else {
 if (!esEdicion && usuarioDAO.existeUsuario(usuarioField.getText().trim())) {
 mostrarError("Usuario existente", "Ese nombre de usuario ya está en uso.");
 return;
 }
 String pwd = contrasenaField.getText();
 if (pwd == null || pwd.trim().isEmpty()) {
 mostrarError("Campo requerido", "La contraseña es obligatoria para nuevos usuarios.");
 return;
 }
 UsuarioAdmin u = new UsuarioAdmin();
 u.setUsuario(usuarioField.getText().trim());
 u.setContrasena(pwd.trim());
 u.setNombre(nombreField.getText().trim());
 u.setPerfil(perfilCombo.getValue());
 if (usuarioDAO.insertar(u)) cerrar();
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
