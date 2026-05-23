package com.example.demo.controller;

import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
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

import java.util.logging.Level;
import java.util.logging.Logger;

public class RecetasController {

    private static final Logger LOGGER = Logger.getLogger(RecetasController.class.getName());

    @FXML private TableView<Receta> recetasTable;
    @FXML private TableColumn<Receta, Integer> idColumn;
    @FXML private TableColumn<Receta, String> productoColumn;
    @FXML private TableColumn<Receta, String> descripcionColumn;
    @FXML private TableColumn<Receta, Double> porcionesColumn;
    @FXML private TableColumn<Receta, Integer> ingredientesColumn;
    @FXML private TableColumn<Receta, Void> accionesColumn;
    @FXML private Label totalLabel;

    private RecetaDAO recetaDAO;
    private ObservableList<Receta> listaRecetas;

    @FXML
    public void initialize() {
        recetaDAO = new RecetaDAO();
        listaRecetas = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        porcionesColumn.setCellValueFactory(new PropertyValueFactory<>("porciones"));
        ingredientesColumn.setCellValueFactory(new PropertyValueFactory<>("totalIngredientes"));

        configurarColumnaAcciones();
        cargarRecetas();
    }

    private void configurarColumnaAcciones() {
        accionesColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editarBtn = new Button("Editar");
            private final Button eliminarBtn = new Button("Eliminar");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5, editarBtn, eliminarBtn);
            {
                editarBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                eliminarBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                editarBtn.setOnAction(e -> abrirModal(getTableView().getItems().get(getIndex())));
                eliminarBtn.setOnAction(e -> eliminarReceta(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void cargarRecetas() {
        listaRecetas.setAll(recetaDAO.listarTodas());
        recetasTable.setItems(listaRecetas);
        totalLabel.setText("Total: " + listaRecetas.size() + " recetas");
    }

    @FXML
    private void nuevaReceta(ActionEvent event) {
        abrirModal(null);
    }

    private void abrirModal(Receta receta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaModal.fxml"));
            Parent root = loader.load();
            RecetaModalController controller = loader.getController();
            controller.setRecetaDAO(recetaDAO);
            if (receta != null) controller.setReceta(receta);

            Stage stage = new Stage();
            stage.setTitle(receta == null ? "Nueva Receta" : "Editar Receta");
            stage.setScene(new Scene(root, 600, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarRecetas();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
        }
    }

    private void eliminarReceta(Receta receta) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminacion");
        alert.setHeaderText("Eliminar receta de: " + receta.getNombreProducto() + "?");
        alert.setContentText("La receta pasara a estado Inactivo.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (recetaDAO.eliminar(receta.getId())) {
                cargarRecetas();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Eliminado");
                info.setHeaderText("Receta eliminada");
                info.show();
            }
        }
    }
}
