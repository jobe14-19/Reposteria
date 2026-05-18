package com.example.demo.controller;
import com.example.demo.service.SessionManager;

import com.example.demo.dao.LimpiezaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LimpiezaModalController {

    private static final Logger LOGGER = Logger.getLogger(LimpiezaModalController.class.getName());

    @FXML private Label tituloLabel;
    @FXML private ComboBox<String> areaComboBox;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<String> tipoComboBox;
    @FXML private ListView<String> responsablesListView;
    @FXML private CheckBox guantesUsadosCheckBox;
    @FXML private CheckBox panosUsadosCheckBox;
    @FXML private CheckBox detergenteUsadoCheckBox;
    @FXML private CheckBox desinfectanteUsadoCheckBox;
    @FXML private CheckBox alcoholUsadoCheckBox;
    @FXML private CheckBox verificadoCheckBox;
    @FXML private Button limpiarButton;
    @FXML private Button cancelarButton;
    @FXML private Button guardarResultado;

    private LimpiezaDAO limpiezaDAO;

    @FXML
    public void initialize() {
        limpiezaDAO = new LimpiezaDAO();

        // Initialize area combobox
        areaComboBox.getItems().addAll("Cocina", "Ba\u00f1o", "Sala", "Comedor", "Patio", "Oficina", "Almac\u00e9n", "\u00c1rea de Producci\u00f3n", "Decoraci\u00f3n", "Delivery");
        areaComboBox.getSelectionModel().selectFirst();

        // Initialize tipo combobox
        tipoComboBox.getItems().addAll("Limpieza General", "Limpieza Profunda", "Desinfecci\u00f3n", "Mantenimiento de \u00c1rea", "Limpieza de Equipos", "Otra");
        tipoComboBox.getSelectionModel().selectFirst();

        // Initialize fecha
        fechaPicker.setValue(LocalDate.now());

        // Initialize responsables list
        responsablesListView.getItems().addAll(
                SessionManager.getInstance().getUsuarioActual()
        );
        responsablesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        responsablesListView.getSelectionModel().selectFirst();

        // Add listeners for validation
        areaComboBox.valueProperty().addListener((obs, old, val) -> validarCampos());
        fechaPicker.valueProperty().addListener((obs, old, val) -> validarCampos());
        tipoComboBox.valueProperty().addListener((obs, old, val) -> validarCampos());

        guardarResultado.setDisable(false);
    }

    public void setAreaLimpieza(String area) {
        areaComboBox.getSelectionModel().select(area);
        validarCampos();
    }

    @FXML
    private void guardarLimpieza(ActionEvent event) {
        if (!validarCamposObligatorios()) {
            mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios.");
            return;
        }

        if (responsablesListView.getSelectionModel().getSelectedItems().isEmpty()) {
            mostrarError("Campo Requerido", "Por favor seleccione al menos un responsable.");
            return;
        }

        try {
            String area = areaComboBox.getValue();
            String tipo = tipoComboBox.getValue();
            LocalDate fecha = fechaPicker.getValue();

            // Build description from available info
            StringBuilder descripcion = new StringBuilder("Tipo: ").append(tipo);
            descripcion.append(". Productos utilizados: ");
            descripcion.append(guantesUsadosCheckBox.isSelected() ? "Guantes, " : "");
            descripcion.append(panosUsadosCheckBox.isSelected() ? "Pa\u00f1os, " : "");
            descripcion.append(detergenteUsadoCheckBox.isSelected() ? "Detergente, " : "");
            descripcion.append(desinfectanteUsadoCheckBox.isSelected() ? "Desinfectante, " : "");
            descripcion.append(alcoholUsadoCheckBox.isSelected() ? "Alcohol, " : "");
            if (descripcion.length() >= 2 && descripcion.charAt(descripcion.length() - 2) == ',') {
                descripcion.setLength(descripcion.length() - 2);
            } else {
                descripcion.append("Ninguno");
            }
            if (verificadoCheckBox.isSelected()) {
                descripcion.append(". Verificado por supervisor.");
            }

            // Join selected responsables
            String responsables = responsablesListView.getSelectionModel().getSelectedItems().stream()
                    .collect(Collectors.joining(", "));

            boolean guardado = limpiezaDAO.registrarLimpieza(area, descripcion.toString(), responsables, fecha);
            if (guardado) {
                mostrarMensaje("\u00c9xito", "El registro de limpieza ha sido guardado correctamente en el \u00e1rea: " + area);
                cerrarModal();
            } else {
                mostrarError("Error", "No se pudo guardar el registro de limpieza. Intente nuevamente.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al guardar limpieza", e);
            mostrarError("Error", "No se pudo guardar el registro: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarLimpieza(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void limpiarCampos(ActionEvent event) {
        areaComboBox.getSelectionModel().clearSelection();
        fechaPicker.setValue(null);
        tipoComboBox.getSelectionModel().clearSelection();
        responsablesListView.getSelectionModel().clearSelection();
        guantesUsadosCheckBox.setSelected(false);
        panosUsadosCheckBox.setSelected(false);
        detergenteUsadoCheckBox.setSelected(false);
        desinfectanteUsadoCheckBox.setSelected(false);
        alcoholUsadoCheckBox.setSelected(false);
        verificadoCheckBox.setSelected(false);
    }

    private boolean validarCamposObligatorios() {
        return areaComboBox.getValue() != null &&
                fechaPicker.getValue() != null &&
                tipoComboBox.getValue() != null;
    }

    private void validarCampos() {
        guardarResultado.setDisable(!validarCamposObligatorios());
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
