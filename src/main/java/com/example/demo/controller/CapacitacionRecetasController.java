package com.example.demo.controller;

import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Receta;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class CapacitacionRecetasController {

    @FXML private TextField buscarField;
    @FXML private VBox recetasContainer;
    @FXML private Label totalLabel;

    private RecetaDAO recetaDAO;
    private ObservableList<Receta> recetasList;

    @FXML
    public void initialize() {
        recetaDAO = new RecetaDAO();
        recetasList = FXCollections.observableArrayList();
        cargarRecetas();

        buscarField.textProperty().addListener((obs, old, val) -> filtrarRecetas());
    }

    private void cargarRecetas() {
        recetasList.setAll(recetaDAO.listarTodas());
        totalLabel.setText("Total: " + recetasList.size() + " recetas");
        mostrarRecetas(recetasList);
    }

    private void filtrarRecetas() {
        String filtro = buscarField.getText().toLowerCase().trim();
        if (filtro.isEmpty()) {
            mostrarRecetas(recetasList);
            return;
        }
        List<Receta> filtradas = recetasList.stream()
            .filter(r -> r.getNombreReceta().toLowerCase().contains(filtro)
                || (r.getCategoria() != null && r.getCategoria().toLowerCase().contains(filtro))
                || r.getNombreProducto().toLowerCase().contains(filtro))
            .collect(Collectors.toList());
        mostrarRecetas(FXCollections.observableArrayList(filtradas));
        totalLabel.setText("Total: " + filtradas.size() + " recetas");
    }

    private void mostrarRecetas(ObservableList<Receta> recetas) {
        recetasContainer.getChildren().clear();
        if (recetas.isEmpty()) {
            recetasContainer.getChildren().add(new Label("No se encontraron recetas."));
            return;
        }

        for (Receta r : recetas) {
            VBox card = crearTarjetaReceta(r);
            recetasContainer.getChildren().add(card);
        }
    }

    private VBox crearTarjetaReceta(Receta r) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("bg-card", "border-light");
        card.setStyle("-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(180);

        Label nombreLbl = new Label(r.getNombreReceta());
        nombreLbl.getStyleClass().add("text-heading");
        nombreLbl.setStyle("-fx-font-size: 16px;");

        HBox infoBox = new HBox(15);
        Label catLbl = new Label((r.getCategoria() != null ? r.getCategoria() : "General"));
        catLbl.setStyle("-fx-background-color: #FFF0E6; -fx-text-fill: #8B5E3C; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label tiempoLbl = new Label(r.getTiempoStr());
        tiempoLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        Label ingLbl = new Label(r.getTotalIngredientes() + " ingredientes");
        ingLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(catLbl, tiempoLbl, ingLbl);

        Label descLbl = new Label(r.getDescripcion() != null && r.getDescripcion().length() > 100
            ? r.getDescripcion().substring(0, 100) + "..." : r.getDescripcion());
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
        descLbl.setMaxHeight(40);

        HBox accionesBox = new HBox(10);
        accionesBox.setAlignment(Pos.CENTER_RIGHT);
        Button verBtn = new Button("Ver en Capacitacion");
        verBtn.setStyle("-fx-background-color: #17A2B8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        verBtn.setOnAction(e -> abrirEnCapacitacion(r));
        accionesBox.getChildren().add(verBtn);

        card.getChildren().addAll(nombreLbl, infoBox, descLbl, accionesBox);
        return card;
    }

    private void abrirEnCapacitacion(Receta receta) {
        try {
            Receta completa = recetaDAO.obtenerPorId(receta.getId());
            if (completa == null) { mostrarError("Error", "No se pudo cargar la receta."); return; }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaViewer.fxml"));
            Parent root = loader.load();
            RecetaViewerController viewer = loader.getController();
            viewer.setReceta(completa);
            viewer.setModoCapacitacion(true);
            viewer.initialize();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 600));
            stage.setTitle("Capacitacion: " + completa.getNombreReceta());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            mostrarError("Error", "No se pudo abrir la receta: " + e.getMessage());
        }
    }

    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
