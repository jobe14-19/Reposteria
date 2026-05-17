package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1000, 700);
            stage.setTitle("🍰 Pastelería Rosato - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            
            LOGGER.log(Level.INFO, "Aplicación iniciada correctamente - Pantalla de login cargada");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la pantalla de login: {0}", e.getMessage());
            mostrarErrorCritico(stage, e);
        }
    }

    private void mostrarErrorCritico(Stage stage, IOException e) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error Crítico");
            alert.setHeaderText("No se pudo iniciar la aplicación");
            alert.setContentText("Error: " + e.getMessage() + "\n\nVerifique que el archivo Login.fxml existe en resources/com/example/demo/");
            alert.showAndWait();
        } catch (Exception ex) {
            System.err.println("Error crítico al iniciar la aplicación: " + e.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
