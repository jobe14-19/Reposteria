package com.example.demo.controller;
import com.example.demo.service.Permiso;
import com.example.demo.service.PermisoService;
import com.example.demo.service.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MenuPrincipalController {

 @FXML private VBox sectionsContainer;

 private final Map<String, String[]> sections = new LinkedHashMap<>();

 @FXML
 public void initialize() {
 SessionManager session = SessionManager.getInstance();
 String perfil = session.getPerfilActual();
 String area = session.getAreaActual();

 // Mapa de visibilidad: Perfil -> Lista de FXMLs permitidos
 // Si el perfil es ADMIN, se permite todo.
 
 sections.put("Dashboards Principales", new String[]{"DashboardAdmin.fxml", "DashboardCliente.fxml", "DashboardEmpleado.fxml"});
        sections.put("Gestión de Pedidos", new String[]{"Pedidos.fxml", "PedidoModal.fxml", "CompraModal.fxml"});
 sections.put("Entregas", new String[]{"Entregas.fxml", "EntregaModal.fxml"});
 sections.put("Clientes y Perfiles", new String[]{"Clientes.fxml", "ClienteModal.fxml", "MiPerfil.fxml"});
 sections.put("Inventario e Ingredientes", new String[]{"Inventario.fxml", "IngredienteModal.fxml"});
 sections.put("Personal y Capacitación", new String[]{"Personal.fxml", "Planificacion.fxml", "EmpleadoModal.fxml", "CapacitacionModal.fxml"});
 sections.put("Limpieza y Mantenimiento", new String[]{"Limpieza.fxml", "LimpiezaModal.fxml", "Mantenimiento.fxml", "MantenimientoModal.fxml"});
 sections.put("Autenticación y Acceso", new String[]{"Login.fxml", "RegistroCliente.fxml"});

 for (Map.Entry<String, String[]> entry : sections.entrySet()) {
 String sectionTitleStr = entry.getKey();
 String[] fxmls = entry.getValue();
 
 // Filtrar FXMLs por sección
 List<String> filteredFxmls = new ArrayList<>();
 for (String fxml : fxmls) {
 if (esVisible(fxml, perfil, area)) {
 filteredFxmls.add(fxml);
 }
 }
 
 if (filteredFxmls.isEmpty()) continue;

 VBox sectionBox = new VBox(15);
 sectionBox.setAlignment(Pos.CENTER);
 sectionBox.getStyleClass().add("menu-card");
 
 Label sectionTitle = new Label(sectionTitleStr);
 sectionTitle.getStyleClass().add("section-header");
 
 FlowPane flowPane = new FlowPane();
 flowPane.setHgap(20);
 flowPane.setVgap(20);
 flowPane.setAlignment(Pos.CENTER);
 
 for (String fxml : filteredFxmls) {
 Button btn = new Button(fxml.replace(".fxml", ""));
 btn.setPrefWidth(200);
 btn.setPrefHeight(50);
 btn.getStyleClass().add("login-button");
 btn.setStyle(btn.getStyle() + "; -fx-font-size: 14px;");
 
 btn.setOnAction(e -> abrirVista(fxml));
 flowPane.getChildren().add(btn);
 }
 
 sectionBox.getChildren().addAll(sectionTitle, flowPane);
 sectionsContainer.getChildren().add(sectionBox);
 }
 }

 private boolean esVisible(String fxml, String perfil, String area) {
 SessionManager session = SessionManager.getInstance();
 if (session.tienePermiso(Permiso.DASHBOARD_ADMIN_LEER)) return true;

 if (PermisoService.tienePermiso(perfil, Permiso.DASHBOARD_CLIENTE_LEER)) {
  return fxml.equals("DashboardCliente.fxml") ||
  fxml.equals("PedidoModal.fxml") ||
  fxml.equals("MiPerfil.fxml");
 }

 if (fxml.equals("MiPerfil.fxml") && session.tienePermiso(Permiso.PERFIL_LEER)) return true;
 if (fxml.equals("DashboardEmpleado.fxml") && session.tienePermiso(Permiso.DASHBOARD_EMPLEADO_LEER)) return true;

  if (fxml.equals("Pedidos.fxml")) {
 return session.tienePermiso(Permiso.PEDIDOS_LEER);
 }
 if (fxml.equals("PedidoModal.fxml") || fxml.equals("CompraModal.fxml")) {
 return session.tienePermiso(Permiso.PEDIDOS_CREAR);
 }
 if (fxml.equals("Clientes.fxml") || fxml.equals("ClienteModal.fxml")) {
 return session.tienePermiso(Permiso.CLIENTES_LEER);
 }
 if (fxml.equals("Entregas.fxml") || fxml.equals("EntregaModal.fxml")) {
 return session.tienePermiso(Permiso.ENTREGAS_LEER);
 }
 if (fxml.equals("Inventario.fxml") || fxml.equals("IngredienteModal.fxml")) {
 return session.tienePermiso(Permiso.INVENTARIO_LEER);
 }
 if (fxml.equals("Personal.fxml") || fxml.equals("EmpleadoModal.fxml")) {
 return session.tienePermiso(Permiso.PERSONAL_LEER);
 }
 if (fxml.equals("Planificacion.fxml")) {
 return session.tienePermiso(Permiso.PRODUCCION_LEER);
 }
 if (fxml.equals("CapacitacionModal.fxml")) {
 return session.tienePermiso(Permiso.CAPACITACIONES_CREAR);
 }
 if (fxml.equals("Limpieza.fxml") || fxml.equals("LimpiezaModal.fxml")) {
 return session.tienePermiso(Permiso.LIMPIEZA_LEER);
 }
 if (fxml.equals("Mantenimiento.fxml") || fxml.equals("MantenimientoModal.fxml")) {
 return session.tienePermiso(Permiso.MANTENIMIENTO_LEER);
 }

 return false;
 }

 private void abrirVista(String fxmlName) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/" + fxmlName));
 Parent viewRoot = loader.load();

 BorderPane wrapper = new BorderPane();

 HBox navBar = new HBox(15);
 navBar.setPadding(new Insets(15, 30, 15, 30));
 navBar.setStyle("-fx-background-color: #8B5E3C; -fx-border-color: #7A4D2B; -fx-border-width: 0 0 2 0;");
 navBar.setAlignment(Pos.CENTER_LEFT);

 Button btnVolver = new Button("← Cerrar");
 btnVolver.getStyleClass().add("nav-logout");
 btnVolver.setStyle("-fx-background-color: white; -fx-text-fill: #8B5E3C; -fx-font-weight: bold; -fx-background-radius: 10;");
 btnVolver.setOnAction(e -> {
 Stage stage = (Stage) btnVolver.getScene().getWindow();
 stage.close();
 });

 Label title = new Label("Módulo: " + fxmlName.replace(".fxml", ""));
 title.setFont(Font.font("System", FontWeight.BOLD, 18));
 title.setStyle("-fx-text-fill: white;");

 navBar.getChildren().addAll(btnVolver, title);

 wrapper.setTop(navBar);
 wrapper.setCenter(viewRoot);

 Stage newStage = new Stage();
 Scene scene = new Scene(wrapper, 1280, 720);
 newStage.setScene(scene);
 newStage.setTitle(" Repostería Rosato - " + fxmlName.replace(".fxml", ""));
 newStage.show();

 } catch (Exception e) {
 System.err.println("Error al abrir " + fxmlName + ": " + e.getMessage());
 e.printStackTrace();
 }
 }

 @FXML
 private void cerrarSesion(ActionEvent event) {
 SessionManager.getInstance().cerrarSesion();
 try {
 Parent root = FXMLLoader.load(getClass().getResource("/com/example/demo/Login.fxml"));
 Stage stage = (Stage) sectionsContainer.getScene().getWindow();
 stage.getScene().setRoot(root);
 stage.setTitle(" Repostería Rosato - Iniciar Sesión");
 } catch (IOException e) {
 e.printStackTrace();
 }
 }
}
