package com.example.demo;

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
import java.util.LinkedHashMap;
import java.util.Map;

public class MenuPrincipalController {

    @FXML private VBox sectionsContainer;

    private final Map<String, String[]> sections = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        sections.put("Autenticación y Acceso", new String[]{"Login.fxml", "RegistroCliente.fxml"});
        sections.put("Dashboards Principales", new String[]{"DashboardAdmin.fxml", "DashboardCliente.fxml", "DashboardEmpleado.fxml"});
        sections.put("Gestión de Pedidos", new String[]{"Pedidos.fxml", "MisPedidos.fxml", "PedidoModal.fxml", "PedidoDetalleModal.fxml", "CompraModal.fxml"});
        sections.put("Entregas", new String[]{"Entregas.fxml", "EntregaModal.fxml"});
        sections.put("Clientes y Perfiles", new String[]{"Clientes.fxml", "ClienteModal.fxml", "MiPerfil.fxml"});
        sections.put("Inventario e Ingredientes", new String[]{"Inventario.fxml", "IngredienteModal.fxml"});
        sections.put("Personal y Capacitación", new String[]{"Personal.fxml", "Planificacion.fxml", "EmpleadoModal.fxml", "CapacitacionModal.fxml"});
        sections.put("Limpieza y Mantenimiento", new String[]{"Limpieza.fxml", "LimpiezaModal.fxml", "Mantenimiento.fxml", "MantenimientoModal.fxml"});

        for (Map.Entry<String, String[]> entry : sections.entrySet()) {
            VBox sectionBox = new VBox(15);
            sectionBox.setAlignment(Pos.CENTER);
            sectionBox.getStyleClass().add("menu-card");
            
            Label sectionTitle = new Label(entry.getKey());
            sectionTitle.getStyleClass().add("section-header");
            
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(20);
            flowPane.setVgap(20);
            flowPane.setAlignment(Pos.CENTER);
            
            for (String fxml : entry.getValue()) {
                Button btn = new Button(fxml.replace(".fxml", ""));
                btn.setPrefWidth(200);
                btn.setPrefHeight(50);
                btn.getStyleClass().add("login-button"); // Reusing login-button style for consistency
                btn.setStyle(btn.getStyle() + "; -fx-font-size: 14px;");
                
                btn.setOnAction(e -> abrirVista(fxml));
                flowPane.getChildren().add(btn);
            }
            
            sectionBox.getChildren().addAll(sectionTitle, flowPane);
            sectionsContainer.getChildren().add(sectionBox);
        }
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
            
            Button btnVolver = new Button("← Volver al Menú");
            btnVolver.getStyleClass().add("nav-logout"); // Using red-ish logout style or custom
            btnVolver.setStyle("-fx-background-color: white; -fx-text-fill: #8B5E3C; -fx-font-weight: bold; -fx-background-radius: 10;");
            btnVolver.setOnAction(e -> {
                try {
                    Parent menuRoot = FXMLLoader.load(getClass().getResource("/com/example/demo/MenuPrincipal.fxml"));
                    Stage stage = (Stage) btnVolver.getScene().getWindow();
                    stage.getScene().setRoot(menuRoot);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            
            Label title = new Label("Módulo: " + fxmlName.replace(".fxml", ""));
            title.setFont(Font.font("System", FontWeight.BOLD, 18));
            title.setStyle("-fx-text-fill: white;");
            
            navBar.getChildren().addAll(btnVolver, title);
            
            wrapper.setTop(navBar);
            wrapper.setCenter(viewRoot);
            
            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            stage.getScene().setRoot(wrapper);
            
        } catch (Exception e) {
            System.err.println("Error al abrir " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/demo/Login.fxml"));
            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Pastelería Rosato - Iniciar Sesión");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
