package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MantenimientoModalController {

    public enum Maquina {
        TALADRO, SIERRA, COMPRESOR, CINTA_TRANSPORTADORA, HORNO, NEVERA, OTRO
    }

    @FXML private Label tituloLabel;
    @FXML private Label fechaLabel;
    @FXML private ComboBox<Maquina> maquinaComboBox;
    @FXML private TextArea descripcionTextArea;
    @FXML private TextField tecnicoField;
    @FXML private DatePicker fechaMantenimientoPicker;
    @FXML private Button cancelarButton;
    @FXML private Button guardarButton;

    private Maquina maquina;

    @FXML
    public void initialize() {
        if (fechaMantenimientoPicker != null) {
            fechaMantenimientoPicker.setValue(LocalDate.now());
        }

        if (fechaLabel != null) {
            fechaLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        if (maquinaComboBox != null) {
            maquinaComboBox.getItems().setAll(Maquina.values());
            maquinaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                this.maquina = newVal;
                validarCampos();
            });
        }

        if (tecnicoField != null) {
            tecnicoField.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        }
        if (descripcionTextArea != null) {
            descripcionTextArea.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        }

        if (guardarButton != null) {
            guardarButton.setDisable(true);
        }
    }

    public void setMaquina(String maquina) {
        try {
            this.maquina = Maquina.valueOf(maquina);
            if (maquinaComboBox != null) {
                maquinaComboBox.getSelectionModel().select(this.maquina);
            }
        } catch (IllegalArgumentException e) {
            this.maquina = Maquina.OTRO;
        }
        validarCampos();
    }

    @FXML
    private void guardarMantenimiento(ActionEvent event) {
        if (!validarCamposObligatorios()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios.");
            return;
        }

        try {
            String maquinaStr = maquina != null ? maquina.name() : "";
            String descripcion = descripcionTextArea != null ? descripcionTextArea.getText() : "";
            String tecnico = tecnicoField != null ? tecnicoField.getText() : "";
            LocalDate fecha = fechaMantenimientoPicker != null ? fechaMantenimientoPicker.getValue() : LocalDate.now();

            System.out.println("Guardando mantenimiento:");
            System.out.println("Máquina: " + maquinaStr);
            System.out.println("Descripción: " + descripcion);
            System.out.println("Técnico: " + tecnico);
            System.out.println("Fecha: " + fecha);

            mostrarMensaje("Éxito", "El registro de mantenimiento ha sido guardado correctamente.");
            cerrarModal();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo guardar el registro: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarMantenimiento(ActionEvent event) {
        cerrarModal();
    }

    private boolean validarCamposObligatorios() {
        return maquina != null &&
                tecnicoField != null && !tecnicoField.getText().trim().isEmpty() &&
                descripcionTextArea != null && !descripcionTextArea.getText().trim().isEmpty() &&
                fechaMantenimientoPicker != null && fechaMantenimientoPicker.getValue() != null;
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