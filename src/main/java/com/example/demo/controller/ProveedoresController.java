package com.example.demo.controller;

import com.example.demo.dao.ProveedorDAO;
import com.example.demo.model.Proveedor;
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

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProveedoresController {

 private static final Logger LOGGER = Logger.getLogger(ProveedoresController.class.getName());

 private static final String BADGE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;";

 @FXML private Button nuevoButton;
 @FXML private TableView<Proveedor> proveedoresTable;
 @FXML private TableColumn<Proveedor, Integer> idColumn;
 @FXML private TableColumn<Proveedor, String> nombreColumn;
 @FXML private TableColumn<Proveedor, String> contactoColumn;
 @FXML private TableColumn<Proveedor, String> telefonoColumn;
 @FXML private TableColumn<Proveedor, String> emailColumn;
 @FXML private TableColumn<Proveedor, String> direccionColumn;
 @FXML private TableColumn<Proveedor, String> estadoColumn;
 @FXML private TableColumn<Proveedor, Void> accionesColumn;
 @FXML private Label totalLabel;

 private ProveedorDAO proveedorDAO;
 private ObservableList<Proveedor> listaProveedores;

 @FXML
 public void initialize() {
 SessionManager session = SessionManager.getInstance();
 if (!session.tienePermiso(Permiso.INVENTARIO_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso.");
 return;
 }
 proveedorDAO = new ProveedorDAO();
 listaProveedores = FXCollections.observableArrayList();

 idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
 contactoColumn.setCellValueFactory(new PropertyValueFactory<>("contacto"));
 telefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
 emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
 direccionColumn.setCellValueFactory(new PropertyValueFactory<>("direccion"));

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

 accionesColumn.setCellFactory(param -> new TableCell<>() {
 private final Button editarBtn = new Button("Editar");
 private final Button toggleBtn = new Button();
 private final HBox hbox = new HBox(5);
 {
 editarBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 toggleBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
 hbox.getChildren().addAll(editarBtn, toggleBtn);
 }
 @Override protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) { setGraphic(null); return; }
 Proveedor p = getTableView().getItems().get(getIndex());
 editarBtn.setOnAction(e -> abrirModal(p));
 toggleBtn.setText("Activo".equals(p.getEstado()) ? "Desactivar" : "Activar");
 toggleBtn.setOnAction(e -> toggleEstado(p));
 setGraphic(hbox);
 }
 });

 cargarProveedores();
 }

 private void cargarProveedores() {
 listaProveedores.setAll(proveedorDAO.listarTodos());
 proveedoresTable.setItems(listaProveedores);
 totalLabel.setText("Total: " + listaProveedores.size() + " proveedores");
 }

 @FXML
 private void nuevoProveedor(ActionEvent event) {
 abrirModal(null);
 }

 private void abrirModal(Proveedor proveedor) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ProveedoresModal.fxml"));
 Parent root = loader.load();
 ProveedoresModalController controller = loader.getController();
 controller.setProveedorDAO(proveedorDAO);
 if (proveedor != null) controller.setProveedor(proveedor);

 Stage stage = new Stage();
 stage.setTitle(proveedor == null ? "Nuevo Proveedor" : "Editar Proveedor");
 stage.setScene(new Scene(root, 500, 400));
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 cargarProveedores();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
 }
 }

 private void toggleEstado(Proveedor p) {
 String accion = "Activo".equals(p.getEstado()) ? "desactivar" : "activar";
 Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
 alert.setTitle("Cambiar Estado");
 alert.setHeaderText(accion + " proveedor: " + p.getNombre() + "?");
 if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
 if (proveedorDAO.toggleEstado(p.getId())) cargarProveedores();
 }
 }

 private void mostrarError(String titulo, String msg) {
 new Alert(Alert.AlertType.ERROR, msg).showAndWait();
 }
}
