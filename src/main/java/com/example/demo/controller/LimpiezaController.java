package com.example.demo.controller;
import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LimpiezaController {

 private static final Logger LOGGER = Logger.getLogger(LimpiezaController.class.getName());

 @FXML private Button registrarLimpiezaButton;
 @FXML private Button actualizarMaterialesButton;
 @FXML private VBox checklistContainer;
 @FXML private TextField nuevoItemField;
 @FXML private Button agregarItemButton;
 @FXML private TableView<LimpiezaRecord> limpiezaTable;
 @FXML private TableColumn<LimpiezaRecord, String> areaColumn;
 @FXML private TableColumn<LimpiezaRecord, String> ultimaLimpiezaColumn;
 @FXML private TableColumn<LimpiezaRecord, Long> diasSinLimpiezaColumn;
 @FXML private TableColumn<LimpiezaRecord, String> estadoColumn;
 @FXML private TableColumn<LimpiezaRecord, Void> accionColumn;
 @FXML private Label totalLabel;

 private DatabaseConnection dbConnection;
 private ObservableList<LimpiezaRecord> limpiezaList;

 @FXML
 public void initialize() {
 dbConnection = DatabaseConnection.getInstance();
 SessionManager session = SessionManager.getInstance();

 if (!session.tienePermiso(Permiso.LIMPIEZA_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso para acceder a la gestión de limpieza.");
 return;
 }

 crearTablaChecklist();
 configurarTabla();
 cargarDatosLimpieza();
 cargarChecklist();
 setupEvents();
 }

 private void configurarTabla() {
 areaColumn.setCellValueFactory(new PropertyValueFactory<>("area"));
 ultimaLimpiezaColumn.setCellValueFactory(new PropertyValueFactory<>("ultimaLimpieza"));
 diasSinLimpiezaColumn.setCellValueFactory(new PropertyValueFactory<>("diasSinLimpieza"));
 estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
 }

 private void setupEvents() {
 registrarLimpiezaButton.setOnAction(this::abrirModalLimpieza);
 actualizarMaterialesButton.setOnAction(e -> abrirGestionMateriales());
 agregarItemButton.setOnAction(e -> agregarItem());
 nuevoItemField.setOnAction(e -> agregarItem());
 }

 private void cargarDatosLimpieza() {
 limpiezaList = FXCollections.observableArrayList();
 // Agrupamos por área para obtener la última fecha de limpieza
 String sql = "SELECT area, MAX(fecha_limpieza) as ultima_fecha FROM limpieza GROUP BY area";
 
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 String area = rs.getString("area");
 String ultima = rs.getString("ultima_fecha");
 
 long dias = 0;
 String estado = "Al día";
 if (ultima != null) {
 try {
 LocalDate lastDate = LocalDate.parse(ultima);
 dias = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
 if (dias > 3) estado = "Pendiente";
 if (dias > 7) estado = "Crítico";
 } catch (Exception e) {
 LOGGER.warning("Error parsing date: " + ultima);
 }
 }

 limpiezaList.add(new LimpiezaRecord(area, ultima, dias, estado));
 }
 limpiezaTable.setItems(limpiezaList);
 totalLabel.setText("Total: " + limpiezaList.size() + " áreas");

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar datos de limpieza: {0}", e.getMessage());
 }
 }

 private void crearTablaChecklist() {
String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='checklist_items' AND xtype='U') " + 
  "CREATE TABLE checklist_items (" + 
  "id_checklist INT IDENTITY(1,1) PRIMARY KEY, " + 
  "nombre VARCHAR(200) NOT NULL, " + 
  "estado VARCHAR(10) DEFAULT 'Activo')";
  try (Connection conn = dbConnection.getConnection();
  Statement stmt = conn.createStatement()) {
  stmt.execute(sql);
  String alterSql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('checklist_items') AND name = 'estado') ALTER TABLE checklist_items ADD estado VARCHAR(10) DEFAULT 'Activo'";
  try { stmt.execute(alterSql); } catch (SQLException ignored) {}
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "No se pudo crear tabla checklist_items: {0}", e.getMessage());
 }
 }

 private void cargarChecklist() {
 checklistContainer.getChildren().clear();
 String sql = "SELECT id_checklist, nombre FROM checklist_items WHERE estado = 'Activo' ORDER BY id_checklist";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 int id = rs.getInt("id_checklist");
 String nombre = rs.getString("nombre");
 HBox row = new HBox(10);
 row.setAlignment(Pos.CENTER_LEFT);
 CheckBox cb = new CheckBox(nombre);
 cb.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
 Button btnEliminar = new Button("Eliminar");
 btnEliminar.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 4; -fx-cursor: hand;");
 btnEliminar.setOnAction(e -> eliminarItem(id));
 row.getChildren().addAll(cb, btnEliminar);
 checklistContainer.getChildren().add(row);
 }
 if (checklistContainer.getChildren().isEmpty()) {
 checklistContainer.getChildren().add(
 new Label("No hay materiales en el checklist. Agregue uno arriba.")
 );
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar checklist: {0}", e.getMessage());
 }
 }

 private void agregarItem() {
 String nombre = nuevoItemField.getText();
 if (nombre == null || nombre.isBlank()) {
 mostrarError("Campo requerido", "Ingrese el nombre del material.");
 nuevoItemField.requestFocus();
 return;
 }
 String sql = "INSERT INTO checklist_items (nombre) VALUES (?)";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, nombre.trim());
 stmt.executeUpdate();
 nuevoItemField.clear();
 cargarChecklist();
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al agregar item: {0}", e.getMessage());
 mostrarError("Error", "No se pudo agregar el material.");
 }
 }

 private void eliminarItem(int id) {
 String sql = "UPDATE checklist_items SET estado = 'Inactivo' WHERE id_checklist = ?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, id);
 stmt.executeUpdate();
 cargarChecklist();
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al eliminar item: {0}", e.getMessage());
 }
 }

 private void abrirModalLimpieza(ActionEvent event) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/LimpiezaModal.fxml"));
 Parent root = loader.load();
 Stage stage = new Stage();
 stage.setTitle("Registro de Limpieza");
 stage.setScene(new Scene(root));
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 cargarDatosLimpieza();
 } catch (IOException e) {
 LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir el modal de limpieza");
 }
 }

 private void abrirGestionMateriales() {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MaterialModal.fxml"));
 Parent root = loader.load();
 Stage stage = new Stage();
 stage.setTitle("Gestión de Materiales de Limpieza");
 stage.setScene(new Scene(root));
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 } catch (IOException e) {
 LOGGER.log(Level.SEVERE, "Error al abrir gestión de materiales: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir la gestión de materiales");
 }
 }

 private void mostrarError(String titulo, String mensaje) {
 Alert alert = new Alert(Alert.AlertType.ERROR);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }

 private void mostrarMensaje(String titulo, String mensaje) {
 Alert alert = new Alert(Alert.AlertType.INFORMATION);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }

 public static class LimpiezaRecord {
 private String area;
 private String ultimaLimpieza;
 private long diasSinLimpieza;
 private String estado;

 public LimpiezaRecord(String area, String ultimaLimpieza, long diasSinLimpieza, String estado) {
 this.area = area;
 this.ultimaLimpieza = ultimaLimpieza;
 this.diasSinLimpieza = diasSinLimpieza;
 this.estado = estado;
 }

 public String getArea() { return area; }
 public String getUltimaLimpieza() { return ultimaLimpieza; }
 public long getDiasSinLimpieza() { return diasSinLimpieza; }
 public String getEstado() { return estado; }
 }
}
