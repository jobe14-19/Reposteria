package com.example.demo.controller;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistroClienteController {

    private static final Logger LOGGER = Logger.getLogger(RegistroClienteController.class.getName());

    @FXML private TextField nombreField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField telefonoField;
    @FXML private TextField direccionField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;

    private DatabaseConnection dbConnection;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String nombre = nombreField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String telefono = telefonoField.getText().trim();
        String direccion = direccionField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (nombre.isEmpty() || email.isEmpty() || username.isEmpty() || telefono.isEmpty() || 
            direccion.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
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

        if (!email.contains("@") || !email.contains(".")) {
            mostrarError("Ingrese un email válido");
            return;
        }

        // Validar que el username no exista
        if (existeUsuario(username)) {
            mostrarError("El nombre de usuario ya está en uso");
            return;
        }

        // Registrar en la base de datos
        registrarUsuario(nombre, email, username, telefono, direccion, password);
    }

    private boolean existeUsuario(String username) {
        try {
            var usuarioOpt = dbConnection.getUsuarioPorCredenciales(username, "");
            return usuarioOpt.isPresent();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar si existe el usuario: {0}", e.getMessage());
            return false;
        }
    }

    private void registrarUsuario(String nombre, String email, String username, String telefono, String direccion, String password) {
        try {
            DatabaseConnection.Cliente nuevoCliente = new DatabaseConnection.Cliente(
                nombre, telefono, email, direccion, username, password
            );
            
            OptionalInt idGenerado = dbConnection.insertarCliente(nuevoCliente);
            
            if (idGenerado.isPresent()) {
                mostrarExito("¡Usuario registrado con éxito! ID: " + idGenerado.getAsInt());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registro Exitoso");
                alert.setHeaderText(null);
                alert.setContentText("Tu cuenta ha sido creada. Ahora puedes iniciar sesión.");
                alert.showAndWait();
                
                volverAlLogin(null);
            } else {
                mostrarError("No se pudo registrar el usuario. Intente nuevamente.");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario: {0}", e.getMessage());
            mostrarError("Error al registrar el usuario: " + e.getMessage());
        }
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

    @FXML
    private void limpiarCampos(ActionEvent event) {
        nombreField.clear();
        emailField.clear();
        usernameField.clear();
        telefonoField.clear();
        direccionField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        messageLabel.setText("");
        messageLabel.setStyle("");
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
