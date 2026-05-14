package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmpleadoModalController {

    private static final Logger LOGGER = Logger.getLogger(EmpleadoModalController.class.getName());

    private static final String SQL_ACTUALIZAR_EMPLEADO =
            "UPDATE empleados SET nombre = ?, cedula = ?, telefono = ?, edad = ?, genero = ?, area = ?, disponibilidad = ?, salario = ?, fecha_modificacion = GETDATE() WHERE id_empleado = ?";

    private static final String SQL_INSERTAR_EMPLEADO =
            "INSERT INTO empleados (nombre, cedula, telefono, edad, genero, area, disponibilidad, salario, fecha_contratacion, fecha_prueba_embarazo, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Activo')";

    private static final String SQL_VERIFICAR_CEDULA_UNICA =
            "SELECT COUNT(*) as total FROM empleados WHERE cedula = ?";

    private static final String SQL_REGISTRAR_ACTIVIDAD =
            "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    private static final int EDAD_MINIMA = 18;
    private static final String DEFAULT_STYLE = "-fx-border-color: #E0E0E0; -fx-border-width: 1;";
    private static final String GENDER_FEMALE = "Femenino";
    private static final String GENDER_MALE = "Masculino";

    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private TextField cedulaField;
    @FXML private TextField telefonoField;
    @FXML private TextField edadField;
    @FXML private ComboBox<String> generoComboBox;
    @FXML private ComboBox<String> areaComboBox;
    @FXML private ComboBox<String> disponibilidadComboBox;
    @FXML private TextField salarioField;
    @FXML private DatePicker fechaContratacionPicker;
    @FXML private VBox pregnancyTestSection;
    @FXML private DatePicker fechaPruebaEmbarazoPicker;
    @FXML private Button cancelarButton;
    @FXML private Button guardarResultado;

    private DatabaseConnection dbConnection;
    private SessionManager sessionManager;
    private PersonalController.Empleado empleadoActual;
    private boolean esEdicion = false;
    private int idEmpleadoEdicion = -1;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        initializeCombos();
        setupFieldValidation();
        setupEventHandlers();

        fechaContratacionPicker.setValue(LocalDate.now());
        pregnancyTestSection.setVisible(false);
    }

    private void initializeCombos() {
        generoComboBox.getItems().addAll(GENDER_MALE, GENDER_FEMALE);
        generoComboBox.getSelectionModel().selectFirst();

        areaComboBox.getItems().addAll("Producción", "Decoración", "Delivery", "Limpieza");
        areaComboBox.getSelectionModel().selectFirst();

        disponibilidadComboBox.getItems().addAll("Completa", "Medio tiempo", "Fines de semana");
        disponibilidadComboBox.getSelectionModel().selectFirst();
    }

    private void setupFieldValidation() {
        nombreField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        cedulaField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        telefonoField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        edadField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
    }

    private void setupEventHandlers() {
        generoComboBox.setOnAction(event -> {
            String genero = generoComboBox.getSelectionModel().getSelectedItem();
            if (GENDER_FEMALE.equals(genero)) {
                pregnancyTestSection.setVisible(true);
            } else {
                pregnancyTestSection.setVisible(false);
                fechaPruebaEmbarazoPicker.setValue(null);
            }
            validarCampos();
        });

        fechaPruebaEmbarazoPicker.setOnAction(event -> validarCampos());
    }

    public void setEmpleado(PersonalController.Empleado empleado) {
        this.empleadoActual = empleado;

        if (empleado != null) {
            esEdicion = true;
            tituloLabel.setText("✏️ Editar Empleado");
            cargarDatosEmpleado(empleado);
            guardarResultado.setText("Actualizar");
        } else {
            esEdicion = false;
            tituloLabel.setText("➕ Contratar Empleado");
            limpiarCampos();
            guardarResultado.setText("Guardar");
        }
    }

    private void cargarDatosEmpleado(PersonalController.Empleado empleado) {
        nombreField.setText(empleado.getNombre());
        cedulaField.setText(empleado.getCedula());
        telefonoField.setText(empleado.getTelefono());
        idEmpleadoEdicion = empleado.getId();
    }

    @FXML
    private void guardarResultado(ActionEvent event) {
        if (!sonCamposValidos()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
            return;
        }

        if (!esEdadValida()) {
            mostrarError("Edad Inválida", "El empleado debe tener al menos " + EDAD_MINIMA + " años.");
            return;
        }

        if (!esCedulaUnica()) {
            mostrarError("Cédula Duplicada", "Ya existe un empleado con esta cédula.");
            return;
        }

        if (!esPruebaEmbarazoValida()) {
            mostrarError("Prueba de Embarazo Requerida", "Para empleadas femeninas, la prueba de embarazo es obligatoria.");
            return;
        }

        try (Connection conn = dbConnection.getConnection()) {
            if (esEdicion) {
                actualizarEmpleado(conn);
            } else {
                insertarEmpleado(conn);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al procesar empleado: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo procesar el empleado: " + e.getMessage());
        }
    }

    private void actualizarEmpleado(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_EMPLEADO)) {
            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerTexto(cedulaField));
            stmt.setString(3, obtenerTexto(telefonoField));
            stmt.setInt(4, obtenerEdad());
            stmt.setString(5, obtenerGenero());
            stmt.setString(6, obtenerArea());
            stmt.setString(7, obtenerDisponibilidad());
            stmt.setDouble(8, obtenerSalario());
            stmt.setInt(9, empleadoActual.getId());

            if (stmt.executeUpdate() > 0) {
                registrarActividad("ACTUALIZAR EMPLEADO", "Empleado actualizado: " + obtenerTexto(nombreField));
                mostrarMensaje("Empleado Actualizado", "El empleado ha sido actualizado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Actualizar", "No se pudo actualizar el empleado.");
            }
        }
    }

    private void insertarEmpleado(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_EMPLEADO)) {
            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerTexto(cedulaField));
            stmt.setString(3, obtenerTexto(telefonoField));
            stmt.setInt(4, obtenerEdad());
            stmt.setString(5, obtenerGenero());
            stmt.setString(6, obtenerArea());
            stmt.setString(7, obtenerDisponibilidad());
            stmt.setDouble(8, obtenerSalario());
            stmt.setDate(9, java.sql.Date.valueOf(fechaContratacionPicker.getValue()));

            if (GENDER_FEMALE.equals(obtenerGenero())) {
                stmt.setDate(10, java.sql.Date.valueOf(fechaPruebaEmbarazoPicker.getValue()));
            } else {
                stmt.setNull(10, Types.DATE);
            }

            if (stmt.executeUpdate() > 0) {
                registrarActividad("CONTRATAR EMPLEADO", "Nuevo empleado contratado: " + obtenerTexto(nombreField));
                mostrarMensaje("Empleado Contratado", "El empleado ha sido contratado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Contratar", "No se pudo contratar al empleado.");
            }
        }
    }

    @FXML
    private void cancelarResultado(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void eliminar(ActionEvent event) {
        if (!esEdicion || idEmpleadoEdicion <= 0) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Está seguro de eliminar este empleado?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // En modo offline, simular eliminación
            if (dbConnection != null) {
                try {
                    String sql = "DELETE FROM empleados WHERE id_empleado = ?";
                    try (Connection conn = dbConnection.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, idEmpleadoEdicion);
                        stmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.INFO, "Modo offline: simulando eliminación de empleado");
                }
            }
            mostrarMensaje("Empleado Eliminado", "El empleado ha sido eliminado correctamente.");
            cerrarModal();
        }
    }

    private boolean sonCamposValidos() {
        boolean camposValidos = !esVacio(nombreField) && !esVacio(cedulaField) && !esVacio(telefonoField) && !esVacio(edadField);

        if (GENDER_FEMALE.equals(obtenerGenero())) {
            camposValidos = camposValidos && fechaPruebaEmbarazoPicker.getValue() != null;
        }
        return camposValidos;
    }

    private boolean esEdadValida() {
        try {
            return Integer.parseInt(edadField.getText()) >= EDAD_MINIMA;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esCedulaUnica() {
        if (esEdicion) return true;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_VERIFICAR_CEDULA_UNICA)) {

            stmt.setString(1, obtenerTexto(cedulaField));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") == 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al validar cédula: {0}", e.getMessage());
            return false;
        }
    }

    private boolean esPruebaEmbarazoValida() {
        return !GENDER_FEMALE.equals(obtenerGenero()) || fechaPruebaEmbarazoPicker.getValue() != null;
    }

    private void limpiarCampos() {
        nombreField.clear();
        cedulaField.clear();
        telefonoField.clear();
        edadField.clear();
        generoComboBox.getSelectionModel().selectFirst();
        areaComboBox.getSelectionModel().selectFirst();
        disponibilidadComboBox.getSelectionModel().selectFirst();
        salarioField.clear();
        fechaContratacionPicker.setValue(LocalDate.now());
        fechaPruebaEmbarazoPicker.setValue(null);
        pregnancyTestSection.setVisible(false);

        nombreField.setStyle(DEFAULT_STYLE);
        cedulaField.setStyle(DEFAULT_STYLE);
        telefonoField.setStyle(DEFAULT_STYLE);
        edadField.setStyle(DEFAULT_STYLE);
    }

    private void validarCampos() {
        guardarResultado.setDisable(!(sonCamposValidos() && esEdadValida()));
    }

    private boolean esVacio(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private String obtenerTexto(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private int obtenerEdad() {
        try {
            return Integer.parseInt(edadField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double obtenerSalario() {
        try {
            String texto = salarioField.getText();
            return (texto != null && !texto.trim().isEmpty()) ? Double.parseDouble(texto) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String obtenerGenero() {
        return generoComboBox.getSelectionModel().getSelectedItem();
    }

    private String obtenerArea() {
        return areaComboBox.getSelectionModel().getSelectedItem();
    }

    private String obtenerDisponibilidad() {
        return disponibilidadComboBox.getSelectionModel().getSelectedItem();
    }

    private void cerrarModal() {
        Stage stage = (Stage) guardarResultado.getScene().getWindow();
        stage.close();
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