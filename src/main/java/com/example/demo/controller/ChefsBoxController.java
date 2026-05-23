package com.example.demo.controller;

import com.example.demo.dao.ChefBoxDAO;
import com.example.demo.model.ChefBox;
import com.example.demo.service.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChefsBoxController {

    private static final Logger LOGGER = Logger.getLogger(ChefsBoxController.class.getName());

    @FXML private Button nuevaBoxButton;
    @FXML private Label totalLabel;
    @FXML private TableView<ChefBox> chefsBoxTable;
    @FXML private TableColumn<ChefBox, Integer> idColumn;
    @FXML private TableColumn<ChefBox, String> nombreColumn;
    @FXML private TableColumn<ChefBox, String> descripcionColumn;
    @FXML private TableColumn<ChefBox, Double> precioColumn;
    @FXML private TableColumn<ChefBox, Integer> productosColumn;
    @FXML private TableColumn<ChefBox, Boolean> disponibleColumn;
    @FXML private TableColumn<ChefBox, Void> accionesColumn;

    private ChefBoxDAO chefBoxDAO;
    private ObservableList<ChefBox> listaBoxes;

    private static final String BTN_EDITAR_STYLE = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BTN_TOGGLE_STYLE = "-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BTN_ELIMINAR_STYLE = "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        chefBoxDAO = new ChefBoxDAO();
        listaBoxes = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        precioColumn.setCellValueFactory(new PropertyValueFactory<>("precio"));
        productosColumn.setCellValueFactory(new PropertyValueFactory<>("totalProductos"));

        disponibleColumn.setCellValueFactory(new PropertyValueFactory<>("disponible"));
        disponibleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    ChefBox box = (ChefBox) getTableRow().getItem();
                    setText(box.isDisponible() ? "Si" : "No");
                    setStyle(box.isDisponible() ? "-fx-text-fill: #28A745; -fx-font-weight: bold;" : "-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                }
            }
        });

        precioColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        });

        configurarColumnaAcciones();
        cargarBoxes();
    }

    private void configurarColumnaAcciones() {
        accionesColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editarButton = new Button("Editar");
            private final Button toggleButton = new Button();
            private final Button eliminarButton = new Button("Eliminar");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5);

            {
                editarButton.setStyle(BTN_EDITAR_STYLE);
                toggleButton.setStyle(BTN_TOGGLE_STYLE);
                eliminarButton.setStyle(BTN_ELIMINAR_STYLE);

                editarButton.setOnAction(event -> {
                    ChefBox box = getTableView().getItems().get(getIndex());
                    abrirModal(box);
                });

                eliminarButton.setOnAction(event -> {
                    ChefBox box = getTableView().getItems().get(getIndex());
                    eliminarBox(box);
                });

                toggleButton.setOnAction(event -> {
                    ChefBox box = getTableView().getItems().get(getIndex());
                    toggleDisponible(box);
                });

                hbox.getChildren().addAll(editarButton, toggleButton, eliminarButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    ChefBox box = getTableView().getItems().get(getIndex());
                    toggleButton.setText(box.isDisponible() ? "Desactivar" : "Activar");
                    setGraphic(hbox);
                }
            }
        });
    }

    private void cargarBoxes() {
        listaBoxes.setAll(chefBoxDAO.listarTodas());
        chefsBoxTable.setItems(listaBoxes);
        totalLabel.setText("Total: " + listaBoxes.size() + " cajas");
    }

    @FXML
    private void nuevaBox(ActionEvent event) {
        abrirModal(null);
    }

    private void abrirModal(ChefBox box) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ChefsBoxModal.fxml"));
            Parent root = loader.load();
            ChefsBoxModalController controller = loader.getController();
            controller.setChefBoxDAO(chefBoxDAO);
            if (box != null) controller.setBox(box);

            Stage stage = new Stage();
            stage.setTitle(box == null ? "Nueva Caja Especial" : "Editar Caja Especial");
            stage.setScene(new Scene(root, 700, 550));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarBoxes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
        }
    }

    private void eliminarBox(ChefBox box) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminacion");
        alert.setHeaderText("Esta seguro de eliminar la caja: " + box.getNombre() + "?");
        alert.setContentText("Esta accion no se puede deshacer.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (chefBoxDAO.eliminar(box.getId())) {
                cargarBoxes();
                mostrarMensaje("Caja eliminada", "La caja ha sido eliminada correctamente.");
            }
        }
    }

    private void toggleDisponible(ChefBox box) {
        String nuevoEstado = box.isDisponible() ? "desactivar" : "activar";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cambiar disponibilidad");
        alert.setHeaderText("Esta seguro de " + nuevoEstado + " la caja: " + box.getNombre() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (chefBoxDAO.toggleDisponible(box.getId(), !box.isDisponible())) {
                cargarBoxes();
            }
        }
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
