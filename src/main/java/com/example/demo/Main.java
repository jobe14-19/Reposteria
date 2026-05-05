package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // Ventana simple sin FXML para probar
        StackPane root = new StackPane();
        root.getChildren().add(new Label("Pastelería Rosato - Sistema funcionando"));
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Pastelería Rosato");
        stage.setScene(scene);
        stage.show();

        System.out.println("¡Aplicación ejecutándose correctamente!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}