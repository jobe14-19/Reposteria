package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LimpiezaController {

    // Definir el enum aquí mismo
    public enum AreaLimpieza {
        COCINA, BANIO, SALA, COMEDOR, PATIO, OFICINA, ALMACEN, OTRO
    }

    @FXML private ComboBox<AreaLimpieza> areaComboBox;
    @FXML private TextArea observacionesTextArea;
    @FXML private Button guardarButton;
    @FXML private Button limpiarButton;

    private AreaLimpieza areaSeleccionada;

    @FXML
    public void initialize() {
        if (areaComboBox != null) {
            areaComboBox.getItems().setAll(AreaLimpieza.values());
            areaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                areaSeleccionada = newVal;
            });
        }
    }

    @FXML
    private void abrirModalLimpieza() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("limpiezaModal.fxml"));
            Parent root = loader.load();

            LimpiezaModalController modalController = loader.getController();

            // Pasar el área como String para evitar conflicto de tipos
            if (areaSeleccionada != null) {
                modalController.setAreaLimpieza(areaSeleccionada.name());
            }

            Stage stage = new Stage();
            stage.setTitle("Registro de Limpieza");
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
    private void guardarLimpieza() {
        if (areaSeleccionada == null) {
            mostrarError("Error", "Debe seleccionar un área de limpieza");
            return;
        }

        String observaciones = observacionesTextArea != null ? observacionesTextArea.getText() : "";

        System.out.println("Área: " + areaSeleccionada);
        System.out.println("Observaciones: " + observaciones);

        mostrarMensaje("Éxito", "Registro de limpieza guardado correctamente");
        limpiarFormulario();
    }

    @FXML
    private void limpiarFormulario() {
        if (areaComboBox != null) {
            areaComboBox.getSelectionModel().clearSelection();
        }
        if (observacionesTextArea != null) {
            observacionesTextArea.clear();
        }
        areaSeleccionada = null;
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