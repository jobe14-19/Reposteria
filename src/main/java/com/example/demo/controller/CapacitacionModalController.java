package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CapacitacionModalController {

 private static final Logger LOGGER = Logger.getLogger(CapacitacionModalController.class.getName());
 private static final int UMBRAL_CAPACITACIONES = 3;

 private static final String SQL_CARGAR_EMPLEADOS = "SELECT id_empleado, nombre FROM empleados WHERE estado = 'Activo' ORDER BY nombre";
 private static final String SQL_INSERTAR_CAPACITACION = "INSERT INTO capacitaciones (id_empleado, tema, fecha, duracion, capacitador, usuario_registra) VALUES (?, ?, ?, ?, ?, ?)";
 private static final String SQL_OBTENER_ID_EMPLEADO = "SELECT id_empleado FROM empleados WHERE nombre = ?";
 private static final String SQL_CONTAR_CAPACITACIONES = "SELECT COUNT(*) as total FROM capacitaciones WHERE id_empleado = ?";
 private static final String SQL_REGISTRAR_ACTIVIDAD = "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

 @FXML private Label tituloLabel;
 @FXML private ComboBox<String> empleadoComboBox;
 @FXML private TextField temaField;
 @FXML private DatePicker fechaPicker;
 @FXML private TextField duracionField;
 @FXML private TextField capacitadorField;
 @FXML private Button limpiarButton;
 @FXML private Button cancelarButton;
 @FXML private Button guardarResultado;

 private DatabaseConnection dbConnection;
 private SessionManager sessionManager;
 private PersonalController.Empleado empleadoActual;

 @FXML
 public void initialize() {
 dbConnection = DatabaseConnection.getInstance();
 sessionManager = SessionManager.getInstance();

 initializeCombos();
 setupFieldValidation();
 setupButtonHandlers();
 fechaPicker.setValue(LocalDate.now());

 if (guardarResultado != null) {
 guardarResultado.setDisable(true);
 }
 }

 private void setupButtonHandlers() {
 if (guardarResultado != null) {
 guardarResultado.setOnAction(this::guardarResultado);
 }

 if (cancelarButton != null) {
 cancelarButton.setOnAction(this::cancelarResultado);
 }
 }

 private void initializeCombos() {
 if (empleadoComboBox == null) {
 LOGGER.warning("empleadoComboBox no está inicializado");
 return;
 }

 empleadoComboBox.getItems().clear();

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_EMPLEADOS);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 empleadoComboBox.getItems().add(rs.getString("nombre"));
 }

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar empleados: {0}", e.getMessage());
 mostrarError("Error de Base de Datos", "No se pudieron cargar los empleados: " + e.getMessage());
 }
 }

 private void setupFieldValidation() {
 if (empleadoComboBox != null) {
 empleadoComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 }
 if (temaField != null) {
 temaField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 }
 if (duracionField != null) {
 duracionField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 }
 if (capacitadorField != null) {
 capacitadorField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 }
 if (fechaPicker != null) {
 fechaPicker.valueProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 }
 }

 public void setEmpleado(PersonalController.Empleado empleado) {
 this.empleadoActual = empleado;
 if (empleado != null && empleadoComboBox != null) {
 String nombreEmpleado = empleado.getNombre();
 if (nombreEmpleado != null) {
 empleadoComboBox.getSelectionModel().select(nombreEmpleado);
 }
 }
 }

 @FXML
 private void guardarResultado(ActionEvent event) {
 if (empleadoComboBox == null || temaField == null || fechaPicker == null ||
 duracionField == null || capacitadorField == null) {
 mostrarError("Error de Inicialización", "Los componentes del formulario no están correctamente inicializados.");
 return;
 }

 if (!validarCamposObligatorios()) {
 mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
 return;
 }

 try {
 guardarCapacitacionEnBD();
 String nombreEmpleado = empleadoComboBox.getSelectionModel().getSelectedItem();
 registrarActividad("REGISTRAR CAPACITACIÓN", "Capacitación registrada para: " + nombreEmpleado);
 verificarProgresoCapacitacion(nombreEmpleado);

 mostrarMensaje("Capacitación Registrada", "La capacitación ha sido registrada correctamente.");
 cerrarModal();

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al registrar capacitación: {0}", e.getMessage());
 mostrarError("Error de Base de Datos", "No se pudo registrar la capacitación: " + e.getMessage());
 } catch (NumberFormatException e) {
 mostrarError("Error de Formato", "La duración debe ser un número válido.");
 }
 }

 private void guardarCapacitacionEnBD() throws SQLException {
 String nombreEmpleado = empleadoComboBox.getSelectionModel().getSelectedItem();
 if (nombreEmpleado == null || nombreEmpleado.trim().isEmpty()) {
 throw new SQLException("No se ha seleccionado un empleado válido");
 }

 int idEmpleado = obtenerIdEmpleado(nombreEmpleado);
 if (idEmpleado == 0) {
 throw new SQLException("No se encontró el empleado seleccionado en la base de datos");
 }

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_CAPACITACION)) {

 stmt.setInt(1, idEmpleado);
 stmt.setString(2, temaField.getText().trim());
 stmt.setDate(3, java.sql.Date.valueOf(fechaPicker.getValue()));
 stmt.setInt(4, Integer.parseInt(duracionField.getText().trim()));
 stmt.setString(5, capacitadorField.getText().trim());
 stmt.setString(6, sessionManager.getUsuarioActual());
 stmt.executeUpdate();
 }
 }

 @FXML
 private void cancelarResultado(ActionEvent event) {
 cerrarModal();
 }

 @FXML
 private void limpiarCampos(ActionEvent event) {
 empleadoComboBox.getSelectionModel().clearSelection();
 temaField.clear();
 fechaPicker.setValue(null);
 duracionField.clear();
 capacitadorField.clear();
 }

 private boolean validarCamposObligatorios() {
 if (empleadoComboBox == null || empleadoComboBox.getSelectionModel() == null) {
 return false;
 }

 return empleadoComboBox.getSelectionModel().getSelectedItem() != null &&
 !isBlank(temaField.getText()) &&
 fechaPicker != null && fechaPicker.getValue() != null &&
 !isBlank(duracionField.getText()) &&
 !isBlank(capacitadorField.getText());
 }

 private boolean isBlank(String str) {
 return str == null || str.trim().isEmpty();
 }

 private int obtenerIdEmpleado(String nombreEmpleado) throws SQLException {
 if (isBlank(nombreEmpleado)) return 0;

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_EMPLEADO)) {

 stmt.setString(1, nombreEmpleado.trim());
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 return rs.getInt("id_empleado");
 }
 return 0;
 }
 }
 }

 private void verificarProgresoCapacitacion(String nombreEmpleado) {
 if (nombreEmpleado == null) return;

 try {
 int idEmpleado = obtenerIdEmpleado(nombreEmpleado);
 if (idEmpleado == 0) return;

 int totalCapacitaciones = contarCapacitacionesEmpleado(idEmpleado);

 if (totalCapacitaciones >= UMBRAL_CAPACITACIONES) {
 mostrarMensaje("¡Capacitación Completada!",
 String.format("El empleado %s ha completado %d capacitaciones y ahora está capacitado al 100%%.",
 nombreEmpleado, totalCapacitaciones));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al verificar progreso: {0}", e.getMessage());
 }
 }

 private int contarCapacitacionesEmpleado(int idEmpleado) throws SQLException {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CONTAR_CAPACITACIONES)) {

 stmt.setInt(1, idEmpleado);
 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() ? rs.getInt("total") : 0;
 }
 }
 }

 private void validarCampos() {
 if (guardarResultado != null) {
 guardarResultado.setDisable(!validarCamposObligatorios());
 }
 }

 private void cerrarModal() {
 if (guardarResultado != null && guardarResultado.getScene() != null) {
 Stage stage = (Stage) guardarResultado.getScene().getWindow();
 if (stage != null) {
 stage.close();
 }
 }
 }

 private void registrarActividad(String accion, String detalle) {
 if (sessionManager == null || sessionManager.getUsuarioActual() == null) {
 LOGGER.warning("No se pudo registrar actividad: usuario no disponible");
 return;
 }

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {

 stmt.setString(1, sessionManager.getUsuarioActual());
 stmt.setString(2, accion);
 stmt.setString(3, detalle);
 stmt.executeUpdate();
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
 }
 }

 private void mostrarError(String titulo, String mensaje) {
 mostrarAlerta(Alert.AlertType.ERROR, titulo, mensaje);
 }

 private void mostrarMensaje(String titulo, String mensaje) {
 mostrarAlerta(Alert.AlertType.INFORMATION, titulo, mensaje);
 }

 private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
 Alert alert = new Alert(tipo);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }
}
