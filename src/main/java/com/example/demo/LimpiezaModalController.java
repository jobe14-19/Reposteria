package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LimpiezaModalController {

    // Definir el enum aquí también (puede ser el mismo nombre)
    public enum AreaLimpieza {
        COCINA, BANIO, SALA, COMEDOR, PATIO, OFICINA, ALMACEN, OTRO
    }

    @FXML private Label tituloLabel;
    @FXML private Label fechaLabel;
    @FXML private ComboBox<AreaLimpieza> areaComboBox;
    @FXML private TextArea descripcionTextArea;
    @FXML private TextField responsableField;
    @FXML private DatePicker fechaLimpiezaPicker;
    @FXML private Button cancelarButton;
    @FXML private Button guardarButton;

    private AreaLimpieza areaLimpieza;

    @FXML
    public void initialize() {
        if (fechaLimpiezaPicker != null) {
            fechaLimpiezaPicker.setValue(LocalDate.now());
        }

        if (fechaLabel != null) {
            fechaLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        if (areaComboBox != null) {
            areaComboBox.getItems().setAll(AreaLimpieza.values());
            areaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                this.areaLimpieza = newVal;
                validarCampos();
            });
        }

        if (responsableField != null) {
            responsableField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        }
        if (descripcionTextArea != null) {
            descripcionTextArea.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        }

        if (guardarButton != null) {
            guardarButton.setDisable(true);
        }
    }

    // Recibir el área como String y convertir al enum local
    public void setAreaLimpieza(String area) {
        try {
            this.areaLimpieza = AreaLimpieza.valueOf(area);
            if (areaComboBox != null) {
                areaComboBox.getSelectionModel().select(this.areaLimpieza);
            }
        } catch (IllegalArgumentException e) {
            this.areaLimpieza = AreaLimpieza.OTRO;
        }
        validarCampos();
    }

    @FXML
    private void guardarLimpieza(ActionEvent event) {
        if (!validarCamposObligatorios()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios.");
            return;
        }

        try {
            String area = areaLimpieza != null ? areaLimpieza.name() : "";
            String descripcion = descripcionTextArea != null ? descripcionTextArea.getText() : "";
            String responsable = responsableField != null ? responsableField.getText() : "";
            LocalDate fecha = fechaLimpiezaPicker != null ? fechaLimpiezaPicker.getValue() : LocalDate.now();

            System.out.println("Guardando limpieza:");
            System.out.println("Área: " + area);
            System.out.println("Descripción: " + descripcion);
            System.out.println("Responsable: " + responsable);
            System.out.println("Fecha: " + fecha);

            mostrarMensaje("Éxito", "El registro de limpieza ha sido guardado correctamente.");
            cerrarModal();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo guardar el registro: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarLimpieza(ActionEvent event) {
        cerrarModal();
    }

    private boolean validarCamposObligatorios() {
        return areaLimpieza != null &&
                responsableField != null && !responsableField.getText().trim().isEmpty() &&
                descripcionTextArea != null && !descripcionTextArea.getText().trim().isEmpty() &&
                fechaLimpiezaPicker != null && fechaLimpiezaPicker.getValue() != null;
    }

    private void validarCampos() {
        if (guardarButton != null) {
            guardarButton.setDisable(!validarCamposObligatorios());
        }
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