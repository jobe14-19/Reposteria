package com.example.demo.controller;

import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RecetasController {

    private static final Logger LOGGER = Logger.getLogger(RecetasController.class.getName());

    @FXML private TextField buscarField;
    @FXML private ComboBox<String> categoriaFilter;
    @FXML private TableView<Receta> recetasTable;
    @FXML private TableColumn<Receta, Integer> idColumn;
    @FXML private TableColumn<Receta, String> productoColumn;
    @FXML private TableColumn<Receta, String> descripcionColumn;
    @FXML private TableColumn<Receta, String> categoriaColumn;
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
        categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        porcionesColumn.setCellValueFactory(new PropertyValueFactory<>("porciones"));

        ingredientesColumn.setCellValueFactory(new PropertyValueFactory<>("totalIngredientes"));
        ingredientesColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String color = item <= 2 ? "#DC3545" : item <= 5 ? "#FF9800" : "#28A745";
                setText(String.valueOf(item));
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        configurarColumnaAcciones();
        cargarCategorias();
        setupListeners();
        cargarRecetas();
    }

    private void configurarColumnaAcciones() {
        accionesColumn.setCellFactory(param -> new TableCell<>() {
            private final Button verBtn = new Button("Ver");
            private final Button editarBtn = new Button("Editar");
            private final Button duplicarBtn = new Button("Duplicar");
            private final Button eliminarBtn = new Button("Eliminar");
            private final HBox hbox = new HBox(5, verBtn, editarBtn, duplicarBtn, eliminarBtn);
            {
                verBtn.setStyle("-fx-background-color: #17A2B8; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                editarBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                duplicarBtn.setStyle("-fx-background-color: #9B59B6; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                eliminarBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                verBtn.setOnAction(e -> verReceta(getTableView().getItems().get(getIndex())));
                editarBtn.setOnAction(e -> abrirModal(getTableView().getItems().get(getIndex())));
                duplicarBtn.setOnAction(e -> duplicarReceta(getTableView().getItems().get(getIndex())));
                eliminarBtn.setOnAction(e -> eliminarReceta(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void setupListeners() {
        buscarField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().isEmpty()) {
                if (categoriaFilter.getValue() == null || "Todas".equals(categoriaFilter.getValue())) {
                    cargarRecetas();
                } else {
                    cargarPorCategoria(categoriaFilter.getValue());
                }
            } else {
                listaRecetas.setAll(recetaDAO.buscarRecetas(n.trim()));
                recetasTable.setItems(listaRecetas);
                totalLabel.setText("Total: " + listaRecetas.size() + " recetas");
            }
        });
        categoriaFilter.setOnAction(e -> {
            String cat = categoriaFilter.getValue();
            if (cat == null || "Todas".equals(cat)) {
                cargarRecetas();
            } else {
                cargarPorCategoria(cat);
            }
        });
    }

    private void cargarCategorias() {
        ObservableList<String> cats = FXCollections.observableArrayList("Todas");
        cats.addAll(recetaDAO.obtenerCategorias());
        categoriaFilter.setItems(cats);
        categoriaFilter.getSelectionModel().selectFirst();
    }

    private void cargarRecetas() {
        listaRecetas.setAll(recetaDAO.listarTodas());
        recetasTable.setItems(listaRecetas);
        totalLabel.setText("Total: " + listaRecetas.size() + " recetas");
    }

    private void cargarPorCategoria(String categoria) {
        listaRecetas.setAll(recetaDAO.listarPorCategoria(categoria));
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
            recargar();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
        }
    }

    private void verReceta(Receta receta) {
        try {
            Receta completa = recetaDAO.obtenerPorId(receta.getId());
            if (completa == null) {
                mostrarError("Error", "No se pudo cargar la receta.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaViewer.fxml"));
            Parent root = loader.load();
            RecetaViewerController controller = loader.getController();
            controller.setReceta(completa);

            Stage stage = new Stage();
            stage.setTitle("Receta: " + completa.getNombreReceta());
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al ver receta: {0}", e.getMessage());
        }
    }

    private void duplicarReceta(Receta receta) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Duplicar Receta");
        alert.setHeaderText("Duplicar: " + receta.getNombreReceta());
        alert.setContentText("Se creara una copia con todos los ingredientes y pasos.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int id = recetaDAO.duplicarReceta(receta.getId());
            if (id > 0) {
                mostrarMensaje("Receta duplicada", "Copia creada correctamente (ID: " + id + ").");
                recargar();
            } else {
                mostrarError("Error", "No se pudo duplicar la receta.");
            }
        }
    }

    private void eliminarReceta(Receta receta) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminacion");
        alert.setHeaderText("Eliminar receta de: " + receta.getNombreProducto() + "?");
        alert.setContentText("La receta pasara a estado Inactivo.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (recetaDAO.eliminar(receta.getId())) {
                recargar();
                mostrarMensaje("Eliminado", "Receta eliminada correctamente.");
            }
        }
    }

    private void recargar() {
        String q = buscarField.getText();
        if (q != null && !q.trim().isEmpty()) {
            listaRecetas.setAll(recetaDAO.buscarRecetas(q.trim()));
        } else {
            String cat = categoriaFilter.getValue();
            if (cat != null && !"Todas".equals(cat)) {
                listaRecetas.setAll(recetaDAO.listarPorCategoria(cat));
            } else {
                cargarRecetas();
                return;
            }
        }
        recetasTable.setItems(listaRecetas);
        totalLabel.setText("Total: " + listaRecetas.size() + " recetas");
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(mensaje); a.showAndWait();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(mensaje); a.showAndWait();
    }
}
