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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MantenimientoController {

 private static final Logger LOGGER = Logger.getLogger(MantenimientoController.class.getName());

 private static final String BADGE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";

 @FXML private Button registrarMantenimientoButton;
 @FXML private Button verHistorialButton;
 @FXML private Button cambiarEstadoButton;
 @FXML private Button actualizarAlertasButton;
 @FXML private ListView<String> alertasListView;
 @FXML private TableView<Maquina> maquinasTable;
 @FXML private TableColumn<Maquina, Integer> idColumn;
 @FXML private TableColumn<Maquina, String> nombreColumn;
 @FXML private TableColumn<Maquina, String> utilidadColumn;
 @FXML private TableColumn<Maquina, String> estadoColumn;
 @FXML private TableColumn<Maquina, String> ultimoMantenimientoColumn;
 @FXML private TableColumn<Maquina, String> proximoMantenimientoColumn;
 @FXML private TableColumn<Maquina, Long> diasRestantesColumn;
 @FXML private TableColumn<Maquina, Void> accionesColumn;
 @FXML private Label totalLabel;

 private DatabaseConnection dbConnection;
 private ObservableList<Maquina> maquinasList;

 @FXML
 public void initialize() {
 dbConnection = DatabaseConnection.getInstance();
 SessionManager session = SessionManager.getInstance();

 if (!session.tienePermiso(Permiso.MANTENIMIENTO_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso para acceder a la gestión de mantenimiento.");
 return;
 }

 configurarTabla();
 cargarMaquinas();
 cargarAlertas();
 setupEvents();
 }

 private void configurarTabla() {
 idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
 utilidadColumn.setCellValueFactory(new PropertyValueFactory<>("utilidad"));

 estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
 estadoColumn.setCellFactory(col -> new TableCell<>() {
 private final Label badge = new Label();
 private final HBox hbox = new HBox(5);
 { badge.setStyle(BADGE_BASE); hbox.getChildren().add(badge); }
 @Override protected void updateItem(String item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) { setGraphic(null); return; }
 String color = switch (item) {
 case "Operativo" -> "#28A745";
 case "Mantenimiento" -> "#FF9800";
 case "Fuera de servicio" -> "#DC3545";
 default -> "#6C757D";
 };
 badge.setStyle(BADGE_BASE + "-fx-background-color: " + color + ";");
 badge.setText(item.toUpperCase());
 setGraphic(hbox);
 }
 });

 ultimoMantenimientoColumn.setCellValueFactory(new PropertyValueFactory<>("ultimoMantenimiento"));
 proximoMantenimientoColumn.setCellValueFactory(new PropertyValueFactory<>("proximoMantenimiento"));

 diasRestantesColumn.setCellValueFactory(new PropertyValueFactory<>("diasRestantes"));
 diasRestantesColumn.setCellFactory(col -> new TableCell<>() {
 @Override protected void updateItem(Long item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) { setText(null); return; }
 String color = item <= 0 ? "#DC3545" : item <= 7 ? "#FF9800" : "#28A745";
 setText(item <= 0 ? "VENCIDO" : item + " días");
 setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
 }
 });

 accionesColumn.setCellFactory(param -> new TableCell<>() {
 private final Button historialBtn = new Button("Historial");
 private final Button mantenimientoBtn = new Button("Mant.");
 private final HBox hbox = new HBox(5);
 {
 historialBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 mantenimientoBtn.setStyle("-fx-background-color: #f55580; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 hbox.getChildren().addAll(historialBtn, mantenimientoBtn);
 }
 @Override protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) { setGraphic(null); return; }
 Maquina m = getTableView().getItems().get(getIndex());
 historialBtn.setOnAction(e -> mostrarMensaje("Historial", "Historial de mantenimiento de: " + m.getNombre()));
 mantenimientoBtn.setOnAction(e -> abrirModalMantenimientoConMaquina(m.getNombre()));
 setGraphic(hbox);
 }
 });
 }

 private void setupEvents() {
 registrarMantenimientoButton.setOnAction(this::abrirModalMantenimiento);
 verHistorialButton.setOnAction(e -> mostrarMensaje("Historial", "Seleccione una máquina y use el botón en la tabla."));
 cambiarEstadoButton.setOnAction(this::cambiarEstadoMaquina);
 actualizarAlertasButton.setOnAction(e -> cargarAlertas());
 }

 private void cambiarEstadoMaquina(ActionEvent event) {
 Maquina seleccionada = maquinasTable.getSelectionModel().getSelectedItem();
 if (seleccionada == null) {
 mostrarError("Sin Selección", "Por favor seleccione una máquina de la tabla.");
 return;
 }

 String[] opciones = {"Operativo", "Mantenimiento", "Fuera de servicio"};
 ChoiceDialog<String> dialog = new ChoiceDialog<>(seleccionada.getEstado(), opciones);
 dialog.setTitle("Cambiar Estado");
 dialog.setHeaderText("Máquina: " + seleccionada.getNombre());
 dialog.setContentText("Seleccione el nuevo estado:");

 dialog.showAndWait().ifPresent(nuevoEstado -> {
 if (!nuevoEstado.equals(seleccionada.getEstado())) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement("UPDATE maquinas SET estado = ? WHERE id_maquina = ?")) {
 stmt.setString(1, nuevoEstado);
 stmt.setInt(2, seleccionada.getId());
 if (stmt.executeUpdate() > 0) {
 mostrarMensaje("Estado Actualizado", "La máquina '" + seleccionada.getNombre() + "' ahora está: " + nuevoEstado);
 cargarMaquinas();
 cargarAlertas();
 }
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al actualizar estado: {0}", e.getMessage());
 mostrarError("Error", "No se pudo actualizar el estado: " + e.getMessage());
 }
 }
 });
 }

 private void cargarMaquinas() {
 maquinasList = FXCollections.observableArrayList();
 String sql = "SELECT id_maquina, nombre, utilidad, estado, ultimo_mantenimiento, proximo_mantenimiento FROM maquinas";
 
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 String ultimo = rs.getString("ultimo_mantenimiento");
 String proximo = rs.getString("proximo_mantenimiento");
 
 long dias = 0;
 if (proximo != null) {
 try {
 LocalDate proxDate = LocalDate.parse(proximo);
 dias = ChronoUnit.DAYS.between(LocalDate.now(), proxDate);
 } catch (Exception e) {
 LOGGER.warning("Error parsing date: " + proximo);
 }
 }

 maquinasList.add(new Maquina(
 rs.getInt("id_maquina"),
 rs.getString("nombre"),
 rs.getString("utilidad"),
 rs.getString("estado"),
 ultimo,
 proximo,
 dias
 ));
 }
 maquinasTable.setItems(maquinasList);
 totalLabel.setText("Total: " + maquinasList.size() + " máquinas");

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar máquinas: {0}", e.getMessage());
 }
 }

 private void cargarAlertas() {
 ObservableList<String> alertas = FXCollections.observableArrayList();
 if (maquinasList != null) {
 for (Maquina m : maquinasList) {
 if (m.getDiasRestantes() <= 0) {
 alertas.add(" VENCIDO - " + m.getNombre() + " requiere mantenimiento urgente");
 } else if (m.getDiasRestantes() <= 3) {
 alertas.add(" CRITICO - " + m.getNombre() + " vence en " + m.getDiasRestantes() + " días");
 } else if (m.getDiasRestantes() <= 7) {
 alertas.add(" PRONTO - " + m.getNombre() + " requiere mantenimiento en " + m.getDiasRestantes() + " días");
 }
 }
 }
 if (alertas.isEmpty()) {
 alertas.add(" Todos los equipos están al día");
 }
 alertasListView.setItems(alertas);
 }

 private void abrirModalMantenimiento(ActionEvent event) {
 abrirModalMantenimientoConMaquina(null);
 }

 private void abrirModalMantenimientoConMaquina(String maquina) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MantenimientoModal.fxml"));
 Parent root = loader.load();
 if (maquina != null) {
 MantenimientoModalController controller = loader.getController();
 controller.setMaquina(maquina);
 }
 Stage stage = new Stage();
 stage.setTitle("Registro de Mantenimiento");
 stage.setScene(new Scene(root));
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 cargarMaquinas();
 cargarAlertas();
 } catch (IOException e) {
 LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir el modal de mantenimiento");
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

 public static class Maquina {
 private int id;
 private String nombre;
 private String utilidad;
 private String estado;
 private String ultimoMantenimiento;
 private String proximoMantenimiento;
 private long diasRestantes;

 public Maquina(int id, String nombre, String utilidad, String estado, String ultimoMantenimiento, String proximoMantenimiento, long diasRestantes) {
 this.id = id;
 this.nombre = nombre;
 this.utilidad = utilidad;
 this.estado = estado;
 this.ultimoMantenimiento = ultimoMantenimiento;
 this.proximoMantenimiento = proximoMantenimiento;
 this.diasRestantes = diasRestantes;
 }

 public int getId() { return id; }
 public String getNombre() { return nombre; }
 public String getUtilidad() { return utilidad; }
 public String getEstado() { return estado; }
 public String getUltimoMantenimiento() { return ultimoMantenimiento; }
 public String getProximoMantenimiento() { return proximoMantenimiento; }
 public long getDiasRestantes() { return diasRestantes; }
 }
}
