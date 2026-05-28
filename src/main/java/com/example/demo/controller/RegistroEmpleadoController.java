package com.example.demo.controller;

import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistroEmpleadoController {

 private static final Logger LOGGER = Logger.getLogger(RegistroEmpleadoController.class.getName());

 @FXML private TextField nombreField;
 @FXML private TextField cedulaField;
 @FXML private TextField telefonoField;
 @FXML private TextField emailField;
 @FXML private TextField direccionField;
 @FXML private ComboBox<String> cargoComboBox;
 @FXML private TextField usernameField;
 @FXML private PasswordField passwordField;
 @FXML private PasswordField confirmPasswordField;
 @FXML private Label messageLabel;
 @FXML private Button registerButton;

 private DatabaseConnection dbConnection;

 private static final String SQL_INSERTAR_USUARIO =
 "INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado) VALUES (?, ?, ?, ?, 'Activo')";

 private static final String SQL_INSERTAR_EMPLEADO =
 "INSERT INTO empleados (nombre, cedula, telefono, area, estado) VALUES (?, ?, ?, ?, 'Activo')";

 private static final String SQL_VERIFICAR_USUARIO =
 "SELECT COUNT(*) as total FROM usuarios WHERE usuario = ?";

 private static final String SQL_VERIFICAR_CEDULA =
 "SELECT COUNT(*) as total FROM empleados WHERE cedula = ?";

 private static final java.util.Map<String, String> AREA_POR_CARGO = new java.util.HashMap<>();

 static {
 AREA_POR_CARGO.put("RECEPCION", "Ventas");
 AREA_POR_CARGO.put("PLANIFICADOR", "Producción");
 AREA_POR_CARGO.put("ALMACEN", "Administración");
 AREA_POR_CARGO.put("PRODUCCION", "Producción");
 AREA_POR_CARGO.put("DECORACION", "Decoración");
 AREA_POR_CARGO.put("CONTABILIDAD", "Administración");
 AREA_POR_CARGO.put("REPARTIDOR", "Delivery");
 AREA_POR_CARGO.put("RRHH", "Administración");
 AREA_POR_CARGO.put("AUDITOR", "Administración");
 AREA_POR_CARGO.put("ADMIN", "Administración");
 }

 @FXML
 public void initialize() {
 dbConnection = DatabaseConnection.getInstance();

 cargoComboBox.getItems().addAll(
 "ADMIN", "RECEPCION", "PLANIFICADOR", "ALMACEN",
 "PRODUCCION", "DECORACION", "CONTABILIDAD",
 "REPARTIDOR", "RRHH", "AUDITOR"
 );
 cargoComboBox.getSelectionModel().selectFirst();
 }

 @FXML
 public void handleRegister(ActionEvent event) {
 String nombre = obtenerTexto(nombreField);
 String cedula = obtenerTexto(cedulaField);
 String telefono = obtenerTexto(telefonoField);
 String email = obtenerTexto(emailField);
 String direccion = obtenerTexto(direccionField);
 String cargo = cargoComboBox.getSelectionModel().getSelectedItem();
 String username = obtenerTexto(usernameField);
 String password = obtenerTexto(passwordField);
 String confirmPassword = obtenerTexto(confirmPasswordField);

 if (nombre.isEmpty() || cedula.isEmpty() || telefono.isEmpty() ||
 username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
 mostrarError("Todos los campos obligatorios deben estar completos");
 return;
 }

 if (!password.equals(confirmPassword)) {
 mostrarError("Las contraseñas no coinciden");
 return;
 }

 if (password.length() < 6) {
 mostrarError("La contraseña debe tener al menos 6 caracteres");
 return;
 }

 try {
 if (existeUsuario(username)) {
 mostrarError("El nombre de usuario ya está en uso");
 return;
 }

 if (existeCedula(cedula)) {
 mostrarError("Ya existe un empleado con esta cédula");
 return;
 }

 try (Connection conn = dbConnection.getConnection()) {
 conn.setAutoCommit(false);
 try {
 int idUsuario = insertarUsuario(conn, username, password, nombre, cargo);
 insertarEmpleado(conn, nombre, cedula, telefono, cargo, idUsuario);
 conn.commit();

 mostrarExito("Empleado registrado exitosamente como: " + cargo);

 Alert alert = new Alert(Alert.AlertType.INFORMATION);
 alert.setTitle("Registro Exitoso");
 alert.setHeaderText(null);
 alert.setContentText("El empleado " + nombre + " ha sido registrado con el cargo " + cargo + ".\n\nUsuario: " + username);
 alert.showAndWait();

 volverAlLogin(null);

 } catch (SQLException e) {
 conn.rollback();
 LOGGER.log(Level.SEVERE, "Error al registrar empleado: {0}", e.getMessage());
 mostrarError("Error al registrar: " + e.getMessage());
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error de conexión: {0}", e.getMessage());
 mostrarError("Error de conexión a la base de datos");
 }
 }

 private boolean existeUsuario(String username) throws SQLException {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_VERIFICAR_USUARIO)) {
 stmt.setString(1, username);
 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() && rs.getInt("total") > 0;
 }
 }
 }

 private boolean existeCedula(String cedula) throws SQLException {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_VERIFICAR_CEDULA)) {
 stmt.setString(1, cedula);
 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() && rs.getInt("total") > 0;
 }
 }
 }

 private int insertarUsuario(Connection conn, String username, String password, String nombre, String cargo) throws SQLException {
 String sql = "INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado) OUTPUT INSERTED.id_usuario VALUES (?, ?, ?, ?, 'Activo')";
 try (PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, username);
 stmt.setString(2, password);
 stmt.setString(3, nombre);
 stmt.setString(4, cargo);
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 return rs.getInt(1);
 }
 throw new SQLException("No se pudo obtener el ID del usuario");
 }
 }
 }

 private void insertarEmpleado(Connection conn, String nombre, String cedula, String telefono, String cargo, int idUsuario) throws SQLException {
 String area = AREA_POR_CARGO.getOrDefault(cargo, "Administración");
 String sql = "INSERT INTO empleados (nombre, cedula, telefono, area, estado) VALUES (?, ?, ?, ?, 'Activo')";
 try (PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, nombre);
 stmt.setString(2, cedula);
 stmt.setString(3, telefono);
 stmt.setString(4, area);
 stmt.executeUpdate();
 }
 }

 @FXML
 private void volverAlLogin(MouseEvent event) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
 Parent root = loader.load();

 Stage stage = (Stage) registerButton.getScene().getWindow();
 Scene scene = new Scene(root, 1000, 700);
 stage.setScene(scene);
 stage.setTitle("Repostería Rosato - Iniciar Sesión");
 stage.show();

 } catch (IOException e) {
 LOGGER.log(Level.SEVERE, "Error al volver al login: {0}", e.getMessage());
 mostrarError("Error al cargar la pantalla de inicio de sesión");
 }
 }

 @FXML
 private void limpiarCampos(ActionEvent event) {
 nombreField.clear();
 cedulaField.clear();
 telefonoField.clear();
 emailField.clear();
 direccionField.clear();
 cargoComboBox.getSelectionModel().selectFirst();
 usernameField.clear();
 passwordField.clear();
 confirmPasswordField.clear();
 messageLabel.setText("");
 messageLabel.setStyle("");
 }

 private String obtenerTexto(TextField field) {
 return field.getText() == null ? "" : field.getText().trim();
 }

 private void mostrarError(String mensaje) {
 messageLabel.setText(mensaje);
 messageLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
 }

 private void mostrarExito(String mensaje) {
 messageLabel.setText(mensaje);
 messageLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
 }
}
