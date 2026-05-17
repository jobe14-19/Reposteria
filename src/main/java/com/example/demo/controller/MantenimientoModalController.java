package com.example.demo.controller;
import com.example.demo.service.SessionManager;

import com.example.demo.dao.MantenimientoDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MantenimientoModalController {

    private static final Logger LOGGER = Logger.getLogger(MantenimientoModalController.class.getName());

    @FXML private Label tituloLabel;
    @FXML private ComboBox<String> maquinaComboBox;
    @FXML private DatePicker fechaMantenimientoPicker;
    @FXML private ComboBox<String> tipoComboBox;
    @FXML private TextArea descripcionTextArea;
    @FXML private DatePicker proximoMantenimientoPicker;
    @FXML private TextField precioTotalField;
    @FXML private Button limpiarButton;
    @FXML private Button cancelarButton;
    @FXML private Button guardarButton;

    private MantenimientoDAO mantenimientoDAO;

    @FXML
    public void initialize() {
        mantenimientoDAO = new MantenimientoDAO();

        // Initialize maquina combobox
        maquinaComboBox.getItems().addAll("Taladro", "Sierra", "Compresor", "Cinta Transportadora", "Horno", "Nevera", "Batidora", "Amasadora", "Otro");
        maquinaComboBox.getSelectionModel().selectFirst();

        // Initialize tipo combobox
        tipoComboBox.getItems().addAll("Preventivo", "Correctivo", "Predictivo", "Emergencia", "Otro");
        tipoComboBox.getSelectionModel().selectFirst();

        // Initialize date pickers
        fechaMantenimientoPicker.setValue(LocalDate.now());
        proximoMantenimientoPicker.setValue(LocalDate.now().plusMonths(1));

        // Add validation listeners
        maquinaComboBox.valueProperty().addListener((obs, old, val) -> validarCampos());
        fechaMantenimientoPicker.valueProperty().addListener((obs, old, val) -> validarCampos());
        tipoComboBox.valueProperty().addListener((obs, old, val) -> validarCampos());
        descripcionTextArea.textProperty().addListener((obs, old, val) -> validarCampos());

        validarCampos();
    }

    public void setMaquina(String maquina) {
        maquinaComboBox.getSelectionModel().select(maquina);
        validarCampos();
    }

    @FXML
    private void guardarMantenimiento(ActionEvent event) {
        if (!validarCamposObligatorios()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios.");
            return;
        }

        try {
            String equipo = maquinaComboBox.getValue();
            String tipo = tipoComboBox.getValue();
            String descripcion = descripcionTextArea.getText() != null ? descripcionTextArea.getText().trim() : "";
            String precioTexto = precioTotalField.getText() != null ? precioTotalField.getText().trim() : "";

            // Build full description including tipo and precio
            StringBuilder descripcionCompleta = new StringBuilder("Tipo: ").append(tipo);
            if (!descripcion.isEmpty()) {
                descripcionCompleta.append(". ").append(descripcion);
            }
            if (!precioTexto.isEmpty()) {
                descripcionCompleta.append(". Costo: $").append(precioTexto);
            }

            String tecnico = SessionManager.getInstance().getUsuarioActual();
            LocalDate fecha = fechaMantenimientoPicker.getValue();
            LocalDate proximo = proximoMantenimientoPicker.getValue();

            boolean guardado = mantenimientoDAO.registrarMantenimiento(
                    equipo, descripcionCompleta.toString(), tecnico, fecha, proximo
            );

            if (guardado) {
                mostrarMensaje("\u00c9xito", "El registro de mantenimiento ha sido guardado correctamente.");
                cerrarModal();
            } else {
                mostrarError("Error", "No se pudo guardar el registro de mantenimiento. Intente nuevamente.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al guardar mantenimiento", e);
            mostrarError("Error", "No se pudo guardar el registro: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarMantenimiento(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void limpiarCampos(ActionEvent event) {
        maquinaComboBox.getSelectionModel().clearSelection();
        fechaMantenimientoPicker.setValue(null);
        tipoComboBox.getSelectionModel().clearSelection();
        descripcionTextArea.clear();
        proximoMantenimientoPicker.setValue(null);
        precioTotalField.clear();
    }

    private boolean validarCamposObligatorios() {
        return maquinaComboBox.getValue() != null &&
                fechaMantenimientoPicker.getValue() != null &&
                tipoComboBox.getValue() != null &&
                descripcionTextArea.getText() != null && !descripcionTextArea.getText().trim().isEmpty();
    }

    private void validarCampos() {
        guardarButton.setDisable(!validarCamposObligatorios());
    }

    private void cerrarModal() {
        Stage stage = (Stage) guardarButton.getScene().getWindow();
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
