package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiPerfilController {

    private static final Logger LOGGER = Logger.getLogger(MiPerfilController.class.getName());

    private static final String SQL_CLIENTE_DATA = "SELECT id_cliente, nombre, telefono, email, direccion, usuario, estado FROM clientes WHERE id_cliente = ?";
    private static final String SQL_EMPLEADO_DATA = "SELECT id_empleado, nombre, cedula as telefono, area, estado FROM empleados WHERE id_empleado = ?";
    private static final String SQL_CAPACITACIONES_COUNT = "SELECT COUNT(*) as total FROM capacitaciones WHERE id_empleado = ?";
    private static final String SQL_HISTORIAL_ACTIVIDAD = "SELECT FORMAT(fecha_hora, 'dd/MM/yyyy HH:mm:ss') as fecha, accion, detalle FROM actividad WHERE usuario = ? ORDER BY fecha_hora DESC";
    private static final String SQL_UPDATE_CLIENTE = "UPDATE clientes SET nombre = ?, telefono = ?, email = ?, direccion = ? WHERE id_cliente = ?";
    private static final String SQL_UPDATE_EMPLEADO = "UPDATE empleados SET nombre = ?, cedula = ? WHERE id_empleado = ?";
    private static final String SQL_UPDATE_CLIENTE_PASSWORD = "UPDATE clientes SET contrasena = ? WHERE id_cliente = ?";
    private static final String SQL_UPDATE_EMPLEADO_PASSWORD = "UPDATE empleados SET contrasena = ? WHERE id_empleado = ?";
    private static final String SQL_VALIDAR_CLIENTE_PASSWORD = "SELECT contrasena FROM clientes WHERE id_cliente = ?";
    private static final String SQL_VALIDAR_EMPLEADO_PASSWORD = "SELECT contrasena FROM empleados WHERE id_empleado = ?";

    private static final String PERFIL_CLIENTE = "CLIENTE";
    private static final String PERFIL_EMPLEADO = "EMPLEADO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_ACTIVO_STYLE = "-fx-font-size: 12px; -fx-text-fill: #28A745; -fx-font-weight: bold;";
    private static final String ESTADO_INACTIVO_STYLE = "-fx-font-size: 12px; -fx-text-fill: #DC3545; -fx-font-weight: bold;";

    private static final int CAPACITACIONES_REQUERIDAS = 3;

    @FXML private Label initialsLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private TextField nombreField;
    @FXML private TextField apellidoField;
    @FXML private TextField telefonoField;
    @FXML private TextField emailField;
    @FXML private TextField direccionField;
    @FXML private TextField usuarioField;
    @FXML private VBox employeeFieldsSection;
    @FXML private TextField areaField;
    @FXML private Label capacitacionesLabel;
    @FXML private PasswordField contrasenaActualField;
    @FXML private PasswordField nuevaContrasenaField;
    @FXML private PasswordField confirmarContrasenaField;
    @FXML private ListView<String> actividadListView;
    @FXML private Label totalActividadLabel;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private String perfil;
    private int idUsuario;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        perfil = sessionManager.getPerfilActual();
        idUsuario = sessionManager.getIdUsuarioActual();

        cargarDatosUsuario();
        cargarHistorialActividad();
    }

    private void cargarDatosUsuario() {
        try (Connection conn = dbConnection.getConnection()) {
            DatosUsuario datosUsuario = null;

            if (PERFIL_CLIENTE.equals(perfil)) {
                datosUsuario = cargarDatosCliente(conn);
            } else if (PERFIL_EMPLEADO.equals(perfil) || PERFIL_ADMIN.equals(perfil)) {
                datosUsuario = cargarDatosEmpleado(conn);
            }

            if (datosUsuario != null) {
                actualizarUIconDatos(datosUsuario);
            } else {
                usarDatosSesion();
            }

        } catch (SQLException e) {
            LOGGER.log(Level.INFO, "Datos de usuario no encontrados en BD, usando datos de sesión");
            usarDatosSesion();
        }
    }

    private DatosUsuario cargarDatosCliente(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_CLIENTE_DATA)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DatosUsuario(
                            rs.getString("nombre"), "",
                            rs.getString("telefono"), rs.getString("email"),
                            rs.getString("direccion"), rs.getString("usuario"), rs.getString("estado")
                    );
                }
            }
        }
        return null;
    }

    private DatosUsuario cargarDatosEmpleado(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_EMPLEADO_DATA)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    employeeFieldsSection.setVisible(true);
                    areaField.setText(rs.getString("area"));
                    cargarCapacitaciones();
                    return new DatosUsuario(
                            rs.getString("nombre"), "",
                            rs.getString("telefono"), "",
                            "", rs.getString("nombre"), rs.getString("estado")
                    );
                }
            }
        }
        return null;
    }

    private void actualizarUIconDatos(DatosUsuario datos) {
        String initials = obtenerIniciales(datos.getNombre(), datos.getApellido());
        initialsLabel.setText(initials);
        roleLabel.setText(perfil);
        actualizarEstadoLabel(datos.getEstado());

        nombreField.setText(datos.getNombre());
        apellidoField.setText(datos.getApellido());
        telefonoField.setText(datos.getTelefono());
        emailField.setText(datos.getEmail());
        direccionField.setText(datos.getDireccion());
        usuarioField.setText(datos.getUsuario());

        if (PERFIL_CLIENTE.equals(perfil)) {
            employeeFieldsSection.setVisible(false);
        }
    }

    private void actualizarEstadoLabel(String estado) {
        statusLabel.setText(estado);
        String estilo = ESTADO_ACTIVO.equalsIgnoreCase(estado) ? ESTADO_ACTIVO_STYLE : ESTADO_INACTIVO_STYLE;
        statusLabel.setStyle(estilo);
    }

    private void cargarCapacitaciones() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CAPACITACIONES_COUNT)) {

            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    capacitacionesLabel.setText(total + "/" + CAPACITACIONES_REQUERIDAS + " completadas");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar capacitaciones: {0}", e.getMessage());
        }
    }

    private void usarDatosSesion() {
        String nombre = sessionManager.getUsuarioActual();
        String perfil = sessionManager.getPerfilActual();
        String initials = nombre != null && !nombre.isEmpty() ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        initialsLabel.setText(initials);
        roleLabel.setText(perfil != null ? perfil : "DESCONOCIDO");
        statusLabel.setText("Activo");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #28A745; -fx-font-weight: bold;");
        nombreField.setText(nombre);
        if (PERFIL_CLIENTE.equals(perfil)) {
            employeeFieldsSection.setVisible(false);
        } else {
            employeeFieldsSection.setVisible(true);
            areaField.setText(sessionManager.getAreaActual());
            capacitacionesLabel.setText("--/3 completadas");
        }
        usuarioField.setText(nombre);
        actividadListView.setItems(FXCollections.observableArrayList("No hay actividad reciente."));
    }

    private void cargarHistorialActividad() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_HISTORIAL_ACTIVIDAD)) {

            stmt.setString(1, sessionManager.getUsuarioActual());
            ObservableList<String> actividad = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fecha = rs.getString("fecha");
                    String accion = rs.getString("accion");
                    String detalle = rs.getString("detalle");
                    actividad.add(String.format("%s - %s: %s", fecha, accion, detalle != null ? detalle : ""));
                }
            }

            if (actividad.isEmpty()) actividad.add("No hay actividad reciente.");
            actividadListView.setItems(actividad);
            totalActividadLabel.setText(actividad.size() + " actividades");

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar historial de actividad: {0}", e.getMessage());
            actividadListView.setItems(FXCollections.observableArrayList("No se pudo cargar el historial de actividad"));
            totalActividadLabel.setText("0 actividades");
        }
    }

    @FXML
    private void guardarCambios(ActionEvent event) {
        String nombre = nombreField.getText() != null ? nombreField.getText().trim() : "";
        String telefono = telefonoField.getText() != null ? telefonoField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String direccion = direccionField.getText() != null ? direccionField.getText().trim() : "";

        if (nombre.isEmpty() || telefono.isEmpty()) {
            mostrarError("Datos Inválidos", "Por favor complete todos los campos requeridos.");
            return;
        }

        try (Connection conn = dbConnection.getConnection()) {
            boolean actualizado = false;
            if (PERFIL_CLIENTE.equals(perfil)) {
                actualizado = actualizarCliente(conn, nombre, telefono, email, direccion);
            } else if (PERFIL_EMPLEADO.equals(perfil) || PERFIL_ADMIN.equals(perfil)) {
                actualizado = actualizarEmpleado(conn, nombre, telefono);
            }

            if (actualizado) {
                mostrarMensaje("Datos Actualizados", "Sus datos personales han sido actualizados correctamente.");
            } else {
                mostrarError("Error al Actualizar", "No se pudieron actualizar los datos.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar cambios: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron guardar los cambios: " + e.getMessage());
        }
    }

    private boolean actualizarCliente(Connection conn, String nombre, String telefono, String email, String direccion) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_CLIENTE)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setString(3, email);
            stmt.setString(4, direccion);
            stmt.setInt(5, idUsuario);
            return stmt.executeUpdate() > 0;
        }
    }

    private boolean actualizarEmpleado(Connection conn, String nombre, String telefono) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_EMPLEADO)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setInt(3, idUsuario);
            return stmt.executeUpdate() > 0;
        }
    }

    @FXML
    private void actualizarContrasena(ActionEvent event) {
        String contrasenaActual = contrasenaActualField.getText() != null ? contrasenaActualField.getText() : "";
        String nuevaContrasena = nuevaContrasenaField.getText() != null ? nuevaContrasenaField.getText() : "";
        String confirmarContrasena = confirmarContrasenaField.getText() != null ? confirmarContrasenaField.getText() : "";

        if (contrasenaActual.isEmpty()) { mostrarError("Error", "La contraseña actual es requerida"); return; }
        if (nuevaContrasena.isEmpty()) { mostrarError("Error", "La nueva contraseña es requerida"); return; }
        if (confirmarContrasena.isEmpty()) { mostrarError("Error", "La confirmación es requerida"); return; }
        if (!nuevaContrasena.equals(confirmarContrasena)) { mostrarError("Error", "Las contraseñas no coinciden"); return; }
        if (nuevaContrasena.length() < 6) { mostrarError("Error", "La contraseña debe tener al menos 6 caracteres"); return; }
        if (nuevaContrasena.equals(contrasenaActual)) { mostrarError("Error", "La nueva contraseña debe ser diferente"); return; }

        try (Connection conn = dbConnection.getConnection()) {
            if (!validarContrasenaActual(conn, contrasenaActual)) {
                mostrarError("Contraseña Incorrecta", "La contraseña actual ingresada es incorrecta.");
                return;
            }

            boolean actualizado = actualizarContrasenaEnBD(conn, nuevaContrasena);
            if (actualizado) {
                mostrarMensaje("Contraseña Actualizada", "Su contraseña ha sido actualizada correctamente.");
                limpiarCamposContrasena();
            } else {
                mostrarError("Error al Actualizar", "No se pudo actualizar la contraseña.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar contraseña: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo actualizar la contraseña: " + e.getMessage());
        }
    }

    private boolean validarContrasenaActual(Connection conn, String contrasenaActual) throws SQLException {
        String sql = PERFIL_CLIENTE.equals(perfil) ? SQL_VALIDAR_CLIENTE_PASSWORD : SQL_VALIDAR_EMPLEADO_PASSWORD;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("contrasena");
                    return contrasenaActual.equals(storedPassword);
                }
            }
        }
        return false;
    }

    private boolean actualizarContrasenaEnBD(Connection conn, String nuevaContrasena) throws SQLException {
        String sql = PERFIL_CLIENTE.equals(perfil) ? SQL_UPDATE_CLIENTE_PASSWORD : SQL_UPDATE_EMPLEADO_PASSWORD;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevaContrasena);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        }
    }

    @FXML
    private void limpiarCampos(ActionEvent event) {
        cargarDatosUsuario();
        limpiarCamposContrasena();
    }

    @FXML
    private void volverAlMenu(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/demo/MenuPrincipal.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al volver al menú: {0}", e.getMessage());
        }
    }

    private void limpiarCamposContrasena() {
        contrasenaActualField.clear();
        nuevaContrasenaField.clear();
        confirmarContrasenaField.clear();
    }

    private String obtenerIniciales(String nombre, String apellido) {
        StringBuilder builder = new StringBuilder();
        if (nombre != null && !nombre.trim().isEmpty()) builder.append(nombre.trim().charAt(0));
        if (apellido != null && !apellido.trim().isEmpty()) builder.append(apellido.trim().charAt(0));
        else if (nombre != null && nombre.trim().length() > 1) builder.append(nombre.trim().charAt(1));
        String result = builder.toString().toUpperCase();
        return result.isEmpty() ? "?" : result;
    }

    @FXML
    private void cerrar(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    public static class DatosUsuario {
        private String nombre, apellido, telefono, email, direccion, usuario, estado;

        public DatosUsuario(String nombre, String apellido, String telefono, String email, String direccion, String usuario, String estado) {
            this.nombre = nombre;
            this.apellido = apellido;
            this.telefono = telefono;
            this.email = email;
            this.direccion = direccion;
            this.usuario = usuario;
            this.estado = estado;
        }

        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getTelefono() { return telefono; }
        public String getEmail() { return email; }
        public String getDireccion() { return direccion; }
        public String getUsuario() { return usuario; }
        public String getEstado() { return estado; }
        public boolean esActivo() { return ESTADO_ACTIVO.equalsIgnoreCase(estado); }
        public String getNombreCompleto() {
            return apellido != null && !apellido.isBlank() ? nombre + " " + apellido : nombre;
        }
    }
}
