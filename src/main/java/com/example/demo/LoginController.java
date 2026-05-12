package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private static final String ERROR_STYLE = "-fx-text-fill: #E74C3C; -fx-font-weight: bold;";

    private static final String VALID_USERNAME = "AnelizEr";
    private static final String VALID_PASSWORD = "12345678";
    private static final String VALID_PROFILE = "ADMIN";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label messageLabel;
    @FXML private Label forgotPasswordLink;
    @FXML private Label registerLink;
    @FXML private Button loginButton;

    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        configurarEventos();
        cargarCredencialesGuardadas();
    }

    private void configurarEventos() {
        usernameField.setOnAction(event -> handleLogin(null));
        passwordField.setOnAction(event -> handleLogin(null));

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> limpiarMensaje());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> limpiarMensaje());
    }

    private void limpiarMensaje() {
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    private void cargarCredencialesGuardadas() {
        // Versión simplificada sin Preferences
        // Las credenciales no se guardan entre sesiones
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            mostrarError("Por favor ingrese usuario y contraseña");
            return;
        }

        if (validarCredenciales(username, password)) {
            sessionManager.iniciarSesion(1, username, VALID_PROFILE);
            redirigirAlDashboard();
        } else {
            mostrarError("Usuario o contraseña incorrectos");
        }
    }

    private boolean validarCredenciales(String username, String password) {
        return VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
    }

    private void redirigirAlDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MenuPrincipal.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Menú Principal");
            stage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el menú principal: {0}", e.getMessage());
            mostrarError("Error al cargar la interfaz principal");
        }
    }

    private String obtenerFxmlPorPerfil(String perfil) {
        if (SessionManager.PERFIL_CLIENTE.equals(perfil)) {
            return "DashboardCliente.fxml";
        } else if (SessionManager.PERFIL_EMPLEADO.equals(perfil)) {
            return "DashboardEmpleado.fxml";
        } else {
            return "DashboardAdmin.fxml";
        }
    }

    @FXML
    private void handleForgotPassword(MouseEvent event) {
        mostrarInformacion("Función de recuperación de contraseña en desarrollo");
    }

    @FXML
    private void abrirRegistro(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RegistroCliente.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);
            stage.setTitle("🍰 Pastelería Rosato - Registro de Cliente");
            stage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir formulario de registro: {0}", e.getMessage());
            mostrarError("Error al abrir el formulario de registro");
        }
    }

    private void mostrarError(String mensaje) {
        messageLabel.setText(mensaje);
        messageLabel.setStyle(ERROR_STYLE);
    }

    private void mostrarInformacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}