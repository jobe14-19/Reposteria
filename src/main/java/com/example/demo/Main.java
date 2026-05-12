package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends Application {

    private Scene mainScene;
    private Stage primaryStage;

    private final Map<String, String[]> sections = new LinkedHashMap<>();

    public Main() {
        sections.put("Autenticación y Acceso", new String[]{"Login.fxml"});
        sections.put("Dashboards Principales", new String[]{"DashboardAdmin.fxml", "DashboardCliente.fxml", "DashboardEmpleado.fxml"});
        sections.put("Gestión de Pedidos", new String[]{"Pedidos.fxml", "MisPedidos.fxml", "PedidoModal.fxml", "PedidoDetalleModal.fxml", "CompraModal.fxml"});
        sections.put("Entregas", new String[]{"Entregas.fxml", "EntregaModal.fxml"});
        sections.put("Clientes y Perfiles", new String[]{"Clientes.fxml", "ClienteModal.fxml", "MiPerfil.fxml"});
        sections.put("Inventario e Ingredientes", new String[]{"Inventario.fxml", "IngredienteModal.fxml"});
        sections.put("Personal y Capacitación", new String[]{"Personal.fxml", "Planificacion.fxml", "EmpleadoModal.fxml", "CapacitacionModal.fxml"});
        sections.put("Limpieza y Mantenimiento", new String[]{"Limpieza.fxml", "LimpiezaModal.fxml", "Mantenimiento.fxml", "MantenimientoModal.fxml"});
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setTitle("Pastelería Rosato - Iniciar Sesión");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirVista(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/" + fxmlName));
            Parent viewRoot = loader.load();
            
            BorderPane wrapper = new BorderPane();
            
            // Barra de navegación superior
            HBox navBar = new HBox(15);
            navBar.setPadding(new Insets(10, 20, 10, 20));
            navBar.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
            navBar.setAlignment(Pos.CENTER_LEFT);
            
            Button btnVolver = new Button("← Volver al Menú");
            btnVolver.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 15;");
            btnVolver.setOnAction(e -> primaryStage.setScene(mainScene));
            
            Label title = new Label("Vista actual: " + fxmlName);
            title.setFont(Font.font("System", FontWeight.BOLD, 16));
            title.setStyle("-fx-text-fill: #333333;");
            
            navBar.getChildren().addAll(btnVolver, title);
            
            wrapper.setTop(navBar);
            wrapper.setCenter(viewRoot);
            
            Scene scene = new Scene(wrapper, 1280, 720);
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            System.err.println("Error al abrir " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}