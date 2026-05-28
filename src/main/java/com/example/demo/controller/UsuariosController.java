package com.example.demo.controller;

import com.example.demo.dao.UsuarioAdminDAO;
import com.example.demo.model.UsuarioAdmin;
import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;

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

import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuariosController {

 private static final Logger LOGGER = Logger.getLogger(UsuariosController.class.getName());
 private static final String BADGE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;";

 @FXML private Button nuevoButton;
 @FXML private TableView<UsuarioAdmin> usuariosTable;
 @FXML private TableColumn<UsuarioAdmin, Integer> idColumn;
 @FXML private TableColumn<UsuarioAdmin, String> usuarioColumn;
 @FXML private TableColumn<UsuarioAdmin, String> nombreColumn;
 @FXML private TableColumn<UsuarioAdmin, String> perfilColumn;
 @FXML private TableColumn<UsuarioAdmin, String> estadoColumn;
 @FXML private TableColumn<UsuarioAdmin, String> fechaColumn;
 @FXML private TableColumn<UsuarioAdmin, Void> accionesColumn;
 @FXML private Label totalLabel;

 private UsuarioAdminDAO usuarioDAO;
 private ObservableList<UsuarioAdmin> listaUsuarios;

 @FXML
 public void initialize() {
 SessionManager session = SessionManager.getInstance();
 if (!session.tienePermiso(Permiso.DASHBOARD_ADMIN_LEER)) {
 mostrarError("Acceso Denegado", "Solo administradores pueden gestionar usuarios.");
 return;
 }
 usuarioDAO = new UsuarioAdminDAO();
 listaUsuarios = FXCollections.observableArrayList();

 idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 usuarioColumn.setCellValueFactory(new PropertyValueFactory<>("usuario"));
 nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
 perfilColumn.setCellValueFactory(new PropertyValueFactory<>("perfil"));
 fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaRegistro"));

 estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
 estadoColumn.setCellFactory(col -> new TableCell<>() {
 private final Label badge = new Label();
 private final HBox hbox = new HBox(5);
 { badge.setStyle(BADGE_BASE); hbox.getChildren().add(badge); }
 @Override protected void updateItem(String item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) { setGraphic(null); return; }
 String color = "Activo".equals(item) ? "#28A745" : "#DC3545";
 badge.setStyle(BADGE_BASE + "-fx-background-color: " + color + ";");
 badge.setText(item.toUpperCase());
 setGraphic(hbox);
 }
 });

 perfilColumn.setCellFactory(col -> new TableCell<>() {
 @Override protected void updateItem(String item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) { setText(null); return; }
 String color = switch (item) {
 case "ADMIN" -> "#E74C3C";
 case "CLIENTE" -> "#3498DB";
 default -> "#6C757D";
 };
 setText(item);
 setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
 }
 });

 accionesColumn.setCellFactory(param -> new TableCell<>() {
 private final Button editarBtn = new Button("Editar");
 private final Button pwdBtn = new Button("Contraseña");
 private final Button toggleBtn = new Button();
 private final HBox hbox = new HBox(5);
 {
 editarBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 pwdBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 toggleBtn.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 hbox.getChildren().addAll(editarBtn, pwdBtn, toggleBtn);
 }
 @Override protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) { setGraphic(null); return; }
 UsuarioAdmin u = getTableView().getItems().get(getIndex());
 editarBtn.setOnAction(e -> abrirModal(u));
 pwdBtn.setOnAction(e -> cambiarContrasena(u));
 toggleBtn.setText("Activo".equals(u.getEstado()) ? "Desactivar" : "Activar");
 toggleBtn.setOnAction(e -> toggleEstado(u));
 setGraphic(hbox);
 }
 });

 cargarUsuarios();
 }

 private void cargarUsuarios() {
 listaUsuarios.setAll(usuarioDAO.listarTodos());
 usuariosTable.setItems(listaUsuarios);
 totalLabel.setText("Total: " + listaUsuarios.size() + " usuarios");
 }

 @FXML
 private void nuevoUsuario(ActionEvent event) {
 abrirModal(null);
 }

 private void abrirModal(UsuarioAdmin usuario) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/UsuariosModal.fxml"));
 Parent root = loader.load();
 UsuariosModalController controller = loader.getController();
 controller.setUsuarioDAO(usuarioDAO);
 if (usuario != null) controller.setUsuario(usuario);

 Stage stage = new Stage();
 stage.setTitle(usuario == null ? "Nuevo Usuario" : "Editar Usuario");
 stage.setScene(new Scene(root, 520, 420));
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 cargarUsuarios();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
 }
 }

 private void cambiarContrasena(UsuarioAdmin u) {
 TextInputDialog dialog = new TextInputDialog();
 dialog.setTitle("Cambiar Contraseña");
 dialog.setHeaderText("Usuario: " + u.getUsuario());
 dialog.setContentText("Nueva contraseña:");
 dialog.showAndWait().ifPresent(pwd -> {
 if (!pwd.trim().isEmpty()) {
 if (usuarioDAO.actualizarContrasena(u.getId(), pwd.trim())) {
 mostrarMensaje("Contraseña actualizada");
 } else {
 mostrarError("Error", "No se pudo actualizar la contraseña.");
 }
 }
 });
 }

 private void toggleEstado(UsuarioAdmin u) {
 String accion = "Activo".equals(u.getEstado()) ? "desactivar" : "activar";
 Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
 alert.setTitle("Cambiar Estado");
 alert.setHeaderText(accion + " usuario: " + u.getUsuario() + "?");
 if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
 if (usuarioDAO.toggleEstado(u.getId())) cargarUsuarios();
 }
 }

 private void mostrarError(String t, String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
 private void mostrarMensaje(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}
