package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistroClienteController {

    private static final Logger LOGGER = Logger.getLogger(RegistroClienteController.class.getName());

    @FXML private TextField nombreField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;

    @FXML
    public void handleRegister(ActionEvent event) {
        String nombre = nombreField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (nombre.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            mostrarError("Todos los campos son obligatorios");
            return;
        }

        if (!password.equals(confirmPassword)) {
            mostrarError("Las contraseñas no coinciden");
            return;
        }

        if (password.length() < 8) {
            mostrarError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        // Simulación de registro exitoso
        registrarUsuario(nombre, email, username, password);
    }

    private void registrarUsuario(String nombre, String email, String username, String password) {
        // Aquí iría la lógica para guardar en la base de datos
        // Por ahora, simulamos éxito
        mostrarExito("¡Usuario registrado con éxito!");
        
        // Regresar al login después de un breve momento o mediante interacción
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registro Exitoso");
        alert.setHeaderText(null);
        alert.setContentText("Tu cuenta ha sido creada. Ahora puedes iniciar sesión.");
        alert.showAndWait();
        
        volverAlLogin(null);
    }

    @FXML
    private void volverAlLogin(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);
            stage.setTitle("Pastelería Rosato - Iniciar Sesión");
            stage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al volver al login: {0}", e.getMessage());
            mostrarError("Error al cargar la pantalla de inicio de sesión");
        }
    }

    private void mostrarError(String mensaje) {
        messageLabel.setText(mensaje);
        messageLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
    }

    private void mostrarExito(String mensaje) {
        messageLabel.setText(mensaje);
        messageLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
    }
}
