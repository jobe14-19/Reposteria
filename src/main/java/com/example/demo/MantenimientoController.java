package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class MantenimientoController {

    public enum Maquina {
        TALADRO, SIERRA, COMPRESOR, CINTA_TRANSPORTADORA, HORNO, NEVERA, OTRO
    }

    @FXML private ComboBox<Maquina> maquinaComboBox;
    @FXML private TextArea observacionesTextArea;
    @FXML private Button guardarButton;
    @FXML private Button limpiarButton;

    private Maquina maquinaSeleccionada;

    @FXML
    public void initialize() {
        if (maquinaComboBox != null) {
            maquinaComboBox.getItems().setAll(Maquina.values());
            maquinaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                maquinaSeleccionada = newVal;
            });
        }
    }

    @FXML
    private void abrirModalMantenimiento() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MantenimientoModal.fxml"));
            Parent root = loader.load();

            MantenimientoModalController modalController = loader.getController();

            if (maquinaSeleccionada != null) {
                modalController.setMaquina(maquinaSeleccionada.name());
            }

            Stage stage = new Stage();
            stage.setTitle("Registro de Mantenimiento");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(guardarButton.getScene().getWindow());
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el modal: " + e.getMessage());
        }
    }

    @FXML
    private void guardarMantenimiento() {
        if (maquinaSeleccionada == null) {
            mostrarError("Error", "Debe seleccionar una máquina");
            return;
        }

        String observaciones = observacionesTextArea != null ? observacionesTextArea.getText() : "";

        System.out.println("Máquina: " + maquinaSeleccionada);
        System.out.println("Observaciones: " + observaciones);

        mostrarMensaje("Éxito", "Registro de mantenimiento guardado correctamente");
        limpiarFormulario();
    }

    @FXML
    private void limpiarFormulario() {
        if (maquinaComboBox != null) {
            maquinaComboBox.getSelectionModel().clearSelection();
        }
        if (observacionesTextArea != null) {
            observacionesTextArea.clear();
        }
        maquinaSeleccionada = null;
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}