package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IngredienteModalController {

    private static final Logger LOGGER = Logger.getLogger(IngredienteModalController.class.getName());

    // Constantes SQL usando Text Blocks (Java 15+)
    private static final String SQL_ACTUALIZAR_INGREDIENTE = """
        UPDATE ingredientes SET nombre = ?, categoria = ?, unidad = ?,
        stock_actual = ?, stock_minimo = ?
        WHERE id_ingrediente = ?
        """;

    private static final String SQL_INSERTAR_INGREDIENTE = """
        INSERT INTO ingredientes (nombre, categoria, unidad, stock_actual, stock_minimo, fecha_registro)
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

    private static final String SQL_REGISTRAR_ACTIVIDAD = """
        INSERT INTO actividad (fecha_hora, usuario, accion, detalle)
        VALUES (CURRENT_TIMESTAMP, ?, ?, ?)
        """;

    // Constantes
    private static final String DEFAULT_STYLE = "-fx-border-color: #E0E0E0; -fx-border-width: 1;";
    private static final String TITULO_NUEVO = "🥘 Nuevo Ingrediente";
    private static final String TITULO_EDITAR = "✏️ Editar Ingrediente";

    // UI Components
    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private ComboBox<String> categoriaComboBox;
    @FXML private TextField unidadField;
    @FXML private TextField stockActualField;
    @FXML private TextField stockMinimoField;
    @FXML private Button guardarResultado;
    @FXML private Button cancelarButton;

    // Services and Managers
    private DatabaseConnection dbConnection;
    private SessionManager sessionManager;
    private InventarioController.Ingrediente ingredienteActual;
    private boolean esEdicion = false;
    private int idIngredienteEdicion = -1;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        initializeCategoriaCombo();
        setupFieldValidation();
    }

    private void initializeCategoriaCombo() {
        categoriaComboBox.getItems().addAll("Harinas", "Lácteos", "Huevos", "Grasas",
                "Azúcares", "Frutas", "Frutos Secos", "Especias", "Otros");
        categoriaComboBox.getSelectionModel().selectFirst();
    }

    public void setIngrediente(InventarioController.Ingrediente ingrediente) {
        this.ingredienteActual = ingrediente;

        if (ingrediente != null) {
            esEdicion = true;
            tituloLabel.setText(TITULO_EDITAR);
            cargarDatosIngrediente(ingrediente);
            guardarResultado.setText("Actualizar");
        } else {
            esEdicion = false;
            tituloLabel.setText(TITULO_NUEVO);
            limpiarCampos();
            guardarResultado.setText("Guardar");
        }
    }

    private void cargarDatosIngrediente(InventarioController.Ingrediente ingrediente) {
        nombreField.setText(ingrediente.getNombre());
        categoriaComboBox.getSelectionModel().select(ingrediente.getCategoria());
        unidadField.setText(ingrediente.getUnidad());
        stockActualField.setText(String.valueOf(ingrediente.getStockActual()));
        stockMinimoField.setText(String.valueOf(ingrediente.getStockMinimo()));
        idIngredienteEdicion = ingrediente.getId();
    }

    private void setupFieldValidation() {
        nombreField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        categoriaComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> validarCampos());
        unidadField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        stockActualField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        stockMinimoField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
    }

    private void validarCampos() {
        boolean camposValidos = sonCamposValidos();
        guardarResultado.setDisable(!camposValidos);
    }

    private boolean sonCamposValidos() {
        return !esVacio(nombreField) &&
                categoriaComboBox.getSelectionModel().getSelectedItem() != null &&
                !esVacio(unidadField) &&
                !esVacio(stockActualField) &&
                !esVacio(stockMinimoField);
    }

    private boolean esVacio(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private boolean sonNumerosValidos() {
        try {
            if (!esVacio(stockActualField)) {
                Double.parseDouble(stockActualField.getText().trim());
            }
            if (!esVacio(stockMinimoField)) {
                Double.parseDouble(stockMinimoField.getText().trim());
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    private void guardarResultado(ActionEvent event) {
        if (!sonCamposValidos()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
            return;
        }

        if (!sonNumerosValidos()) {
            mostrarError("Valores Inválidos", "Stock actual y stock mínimo deben ser números válidos.");
            return;
        }

        try (Connection conn = dbConnection.getConnection()) {
            if (esEdicion) {
                actualizarIngrediente(conn);
            } else {
                insertarIngrediente(conn);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al procesar ingrediente: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo procesar el ingrediente: " + e.getMessage());
        } catch (NumberFormatException e) {
            mostrarError("Error de Formato", "Stock actual y stock mínimo deben ser números válidos.");
        }
    }

    private void actualizarIngrediente(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_INGREDIENTE)) {
            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerCategoria());
            stmt.setString(3, obtenerTexto(unidadField));
            stmt.setDouble(4, obtenerStockActual());
            stmt.setDouble(5, obtenerStockMinimo());
            stmt.setInt(6, ingredienteActual.getId());

            if (stmt.executeUpdate() > 0) {
                registrarActividad("ACTUALIZAR INGREDIENTE",
                        "Ingrediente actualizado: " + obtenerTexto(nombreField));
                mostrarMensaje("Ingrediente Actualizado", "El ingrediente ha sido actualizado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Actualizar", "No se pudo actualizar el ingrediente.");
            }
        }
    }

    private void insertarIngrediente(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_INGREDIENTE,
                PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, obtenerTexto(nombreField));
            stmt.setString(2, obtenerCategoria());
            stmt.setString(3, obtenerTexto(unidadField));
            stmt.setDouble(4, obtenerStockActual());
            stmt.setDouble(5, obtenerStockMinimo());

            if (stmt.executeUpdate() > 0) {
                registrarActividad("CREAR INGREDIENTE",
                        "Nuevo ingrediente creado: " + obtenerTexto(nombreField));
                mostrarMensaje("Ingrediente Creado", "El ingrediente ha sido creado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error al Crear", "No se pudo crear el ingrediente.");
            }
        }
    }

    @FXML
    private void cancelarResultado(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void eliminar(ActionEvent event) {
        if (!esEdicion || idIngredienteEdicion <= 0) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Está seguro de eliminar este ingrediente?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (dbConnection != null) {
                try (Connection conn = dbConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM ingredientes WHERE id_ingrediente = ?")) {
                    stmt.setInt(1, idIngredienteEdicion);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    LOGGER.log(Level.INFO, "Modo offline: simulando eliminación de ingrediente");
                }
            }
            mostrarMensaje("Ingrediente Eliminado", "El ingrediente ha sido eliminado correctamente.");
            cerrarModal();
        }
    }

    private String obtenerTexto(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String obtenerCategoria() {
        String categoria = categoriaComboBox.getSelectionModel().getSelectedItem();
        return categoria == null ? "" : categoria;
    }

    private double obtenerStockActual() {
        try {
            String texto = stockActualField.getText();
            return (texto != null && !texto.isBlank()) ? Double.parseDouble(texto.trim()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double obtenerStockMinimo() {
        try {
            String texto = stockMinimoField.getText();
            return (texto != null && !texto.isBlank()) ? Double.parseDouble(texto.trim()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    private void limpiarCampos() {
        nombreField.clear();
        categoriaComboBox.getSelectionModel().selectFirst();
        unidadField.clear();
        stockActualField.clear();
        stockMinimoField.clear();

        // Reset field styles
        nombreField.setStyle(DEFAULT_STYLE);
        unidadField.setStyle(DEFAULT_STYLE);
        stockActualField.setStyle(DEFAULT_STYLE);
        stockMinimoField.setStyle(DEFAULT_STYLE);
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
