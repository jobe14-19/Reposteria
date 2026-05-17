package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import com.example.demo.model.Cliente;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClienteModalController {

    private static final Logger LOGGER = Logger.getLogger(ClienteModalController.class.getName());

    // Constantes SQL (sin Text Blocks)
    private static final String SQL_UPDATE_CLIENTE =
            "UPDATE clientes SET nombre = ?, apellido = ?, telefono = ?, email = ?, direccion = ?, rnc = ?, usuario = ?, contrasena = ?, fecha_modificacion = GETDATE() WHERE id_cliente = ?";

    private static final String SQL_INSERT_CLIENTE =
            "INSERT INTO clientes (nombre, apellido, telefono, email, direccion, rnc, usuario, contrasena, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";

    private static final String SQL_REGISTRAR_ACTIVIDAD =
            "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    // Patrones de validación
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    // Estilos CSS
    private static final String DEFAULT_STYLE = "-fx-border-color: #E0E0E0; -fx-border-width: 1;";
    private static final String ERROR_STYLE = "-fx-border-color: #E74C3C; -fx-border-width: 2;";

    // UI Components
    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private TextField apellidoField;
    @FXML private TextField telefonoField;
    @FXML private TextField emailField;
    @FXML private TextField direccionField;
    @FXML private TextField rncField;
    @FXML private TextField usuarioField;
    @FXML private PasswordField contrasenaField;
    @FXML private Button guardarResultado;
    @FXML private Button cancelarButton;

    // Services and Managers
    private DatabaseConnection dbConnection;
    private SessionManager sessionManager;
    private Cliente clienteActual;
    private boolean esEdicion = false;
    private int idClienteEdicion = -1;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        sessionManager = SessionManager.getInstance();
        setupFieldValidation();
    }

    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;

        if (cliente != null) {
            esEdicion = true;
            tituloLabel.setText("✏️ Editar Cliente");
            cargarDatosCliente(cliente);
            guardarResultado.setText("Actualizar");
        } else {
            esEdicion = false;
            tituloLabel.setText("👥 Nuevo Cliente");
            limpiarCampos();
            guardarResultado.setText("Guardar");
        }
    }

    private void cargarDatosCliente(Cliente cliente) {
        nombreField.setText(cliente.getNombre());
        apellidoField.setText(cliente.getApellido());
        telefonoField.setText(cliente.getTelefono());
        emailField.setText(cliente.getEmail());
        direccionField.setText(cliente.getDireccion());
        rncField.setText(cliente.getRnc());
        usuarioField.setText(cliente.getUsuario());
        contrasenaField.setText(cliente.getContrasena());
        idClienteEdicion = cliente.getId();
    }


    private void setupFieldValidation() {
        nombreField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        apellidoField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        telefonoField.textProperty().addListener((obs, oldVal, newVal) -> {
            actualizarEstiloTelefono(newVal);
            validarCampos();
        });

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            actualizarEstiloEmail(newVal);
            validarCampos();
        });
    }

    private void actualizarEstiloTelefono(String telefono) {
        if (telefono != null && !telefono.isEmpty() && !esTelefonoValido(telefono)) {
            telefonoField.setStyle(ERROR_STYLE);
        } else {
            telefonoField.setStyle(DEFAULT_STYLE);
        }
    }

    private void actualizarEstiloEmail(String email) {
        if (email != null && !email.isEmpty() && !esEmailValido(email)) {
            emailField.setStyle(ERROR_STYLE);
        } else {
            emailField.setStyle(DEFAULT_STYLE);
        }
    }

    private boolean esEmailValido(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean esTelefonoValido(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    private void validarCampos() {
        boolean camposValidos = sonCamposObligatoriosValidos();
        guardarResultado.setDisable(!camposValidos);
    }

    @FXML
    private void guardarResultado(ActionEvent event) {
        if (!sonCamposObligatoriosValidos()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
            return;
        }

        try (Connection conn = dbConnection.getConnection()) {
            if (esEdicion) {
                actualizarCliente(conn);
            } else {
                crearCliente(conn);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar cliente: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo guardar el cliente: " + e.getMessage());
        }
    }

    private void actualizarCliente(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_CLIENTE)) {
            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerTexto(apellidoField));
            stmt.setString(3, obtenerTexto(telefonoField));
            stmt.setString(4, obtenerTexto(emailField));
            stmt.setString(5, obtenerTexto(direccionField));
            stmt.setString(6, obtenerTexto(rncField));
            stmt.setString(7, obtenerTexto(usuarioField));
            stmt.setString(8, obtenerTextoContrasena());
            stmt.setInt(9, clienteActual.getId());

            if (stmt.executeUpdate() > 0) {
                registrarActividad("ACTUALIZAR CLIENTE",
                        "Cliente actualizado: " + obtenerTexto(nombreField) + " " + obtenerTexto(apellidoField));
                mostrarMensaje("Cliente Actualizado", "El cliente ha sido actualizado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Actualizar", "No se pudo actualizar el cliente.");
            }
        }
    }

    private void crearCliente(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_CLIENTE, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerTexto(apellidoField));
            stmt.setString(3, obtenerTexto(telefonoField));
            stmt.setString(4, obtenerTexto(emailField));
            stmt.setString(5, obtenerTexto(direccionField));
            stmt.setString(6, obtenerTexto(rncField));
            stmt.setString(7, obtenerTexto(usuarioField));
            stmt.setString(8, obtenerTextoContrasena());

            if (stmt.executeUpdate() > 0) {
                registrarActividad("CREAR CLIENTE",
                        "Nuevo cliente creado: " + obtenerTexto(nombreField) + " " + obtenerTexto(apellidoField));
                mostrarMensaje("Cliente Creado", "El cliente ha sido creado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Crear", "No se pudo crear el cliente.");
            }
        }
    }

    @FXML
    private void cancelarResultado(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void eliminar(ActionEvent event) {
        if (!esEdicion || idClienteEdicion <= 0) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Está seguro de eliminar este cliente?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                com.example.demo.dao.ClienteDAO dao = new com.example.demo.dao.ClienteDAO();
                boolean eliminado = dao.eliminarCliente(idClienteEdicion);
                if (eliminado) {
                    mostrarMensaje("Cliente Eliminado", "El cliente ha sido eliminado correctamente.");
                } else {
                    mostrarMensaje("No se pudo eliminar", "El cliente no pudo ser eliminado. Verifique si tiene pedidos asociados.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar cliente: {0}", e.getMessage());
                mostrarError("Error", "No se pudo eliminar el cliente: " + e.getMessage());
            }
            cerrarModal();
        }
    }

    private boolean sonCamposObligatoriosValidos() {
        return !esVacio(nombreField) &&
                !esVacio(apellidoField) &&
                !esVacio(telefonoField);
    }

    private boolean esVacio(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private String obtenerTexto(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String obtenerTextoContrasena() {
        return contrasenaField.getText() == null ? "" : contrasenaField.getText();
    }

    @FXML
    private void limpiarCampos() {
        TextField[] fields = {nombreField, apellidoField, telefonoField, emailField, direccionField, rncField, usuarioField};

        for (TextField field : fields) {
            field.clear();
            field.setStyle(DEFAULT_STYLE);
        }
        contrasenaField.clear();
        contrasenaField.setStyle(DEFAULT_STYLE);
    }

    private void registrarActividad(String accion, String detalle) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {

            stmt.setString(1, sessionManager.getUsuarioActual());
            stmt.setString(2, accion);
            stmt.setString(3, detalle);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
        }
    }

    private void cerrarModal() {
        Stage stage = (Stage) guardarResultado.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.ERROR, titulo, mensaje);
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

}
