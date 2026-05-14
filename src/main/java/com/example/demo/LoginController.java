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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.demo.dao.UsuarioDAO;
import com.example.demo.dao.ClienteDAO;
import com.example.demo.SessionManager;
import com.example.demo.DatabaseConnection;

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
    private UsuarioDAO usuarioDAO;
    private ClienteDAO clienteDAO;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        usuarioDAO = new UsuarioDAO();
        clienteDAO = new ClienteDAO();
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

        // 1. Intentar como Usuario (Admin/Empleado)
        Optional<DatabaseConnection.Usuario> userOpt = usuarioDAO.validarCredenciales(username, password);
        
        if (userOpt.isPresent()) {
            DatabaseConnection.Usuario user = userOpt.get();
            String area = "";
            if (SessionManager.PERFIL_EMPLEADO.equals(user.getPerfil())) {
                area = usuarioDAO.obtenerAreaEmpleado(user.getId());
            }
            sessionManager.iniciarSesion(user.getId(), user.getNombre(), user.getPerfil(), area);
            redirigirAlDashboard();
            return;
        }

        // 2. Intentar como Cliente
        Optional<DatabaseConnection.Usuario> clienteOpt = clienteDAO.validarCredenciales(username, password);
        if (clienteOpt.isPresent()) {
            DatabaseConnection.Usuario cliente = clienteOpt.get();
            sessionManager.iniciarSesion(cliente.getId(), cliente.getNombre(), cliente.getPerfil(), "");
            redirigirAlDashboard();
            return;
        }

        mostrarError("Usuario o contraseña incorrectos");
    }

    private void redirigirAlDashboard() {
        try {
            String perfil = sessionManager.getPerfilActual();
            String fxml = obtenerFxmlPorPerfil(perfil);
            
            // Si es ADMIN, lo llevamos al Menu Principal que tiene acceso a todo
            // Para otros roles, también podemos llevarlos al Menu Principal pero filtrado,
            // o directamente a su Dashboard. El usuario pidió que si es cliente solo vea lo relevante.
            
            // Decisión: Todos van al MenuPrincipal, pero el MenuPrincipal se filtrará.
            // Excepto si queremos una experiencia más directa.
            // El usuario dijo "si yo entro con un usuario de tipo cliente solo dejes acceder a l informacion que es relevante para un cliente"
            
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