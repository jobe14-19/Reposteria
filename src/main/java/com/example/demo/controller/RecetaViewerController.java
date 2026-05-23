package com.example.demo.controller;

import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.PasoReceta;
import com.example.demo.model.Receta.RecetaIngrediente;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

public class RecetaViewerController {

    @FXML private Label tituloLabel, nombreLabel, categoriaLabel, tiempoLabel, cantidadLabel, costoLabel;
    @FXML private Label productoLabel, rendimientoLabel, desperdicioLabel;
    @FXML private TabPane tabPane;
    @FXML private Tab ingredientesTab, procedimientoTab, costosTab;
    @FXML private VBox ingredientesContainer, pasosContainer, costosContainer;
    @FXML private HBox navegacionBox;
    @FXML private Button anteriorPasoButton, siguientePasoButton, cerrarButton;
    @FXML private Label pasoContadorLabel, modoLabel;

    private Receta receta;
    private boolean modoCapacitacion;
    private int pasoActual = 0;

    public void setReceta(Receta r) { this.receta = r; }
    public void setModoCapacitacion(boolean b) { this.modoCapacitacion = b; }

    @FXML
    public void initialize() {
        if (receta == null) return;

        nombreLabel.setText(receta.getNombreReceta());
        productoLabel.setText("Producto: " + receta.getNombreProducto());
        categoriaLabel.setText("Categoria: " + (receta.getCategoria() != null ? receta.getCategoria() : "-"));
        tiempoLabel.setText("Tiempo: " + receta.getTiempoStr());
        cantidadLabel.setText("Cantidad: " + receta.getCantidadProducida() + " unidades");
        costoLabel.setText("Costo: $" + String.format("%.2f", receta.getCostoEstimado()));
        rendimientoLabel.setText("Rendimiento: " + receta.getRendimiento() + "%");
        desperdicioLabel.setText("Desperdicio: " + receta.getDesperdicio() + "%");

        cargarIngredientes();
        cargarPasos();
        cargarCostos();

        if (modoCapacitacion) {
            modoLabel.setText("MODO CAPACITACION");
            modoLabel.setStyle("-fx-background-color: #17A2B8; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            navegacionBox.setVisible(true);
            pasoActual = 0;
            mostrarPasoCapacitacion(0);
        } else {
            modoLabel.setText("MODO PRODUCCION");
            modoLabel.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            navegacionBox.setVisible(false);
            cargarPasosProduccion();
        }
    }

    private void cargarIngredientes() {
        ingredientesContainer.getChildren().clear();
        Label header = new Label("Ingredientes necesarios:");
        header.getStyleClass().add("text-heading-sm");
        header.setStyle("-fx-font-size: 14px;");
        ingredientesContainer.getChildren().add(header);

        if (receta.getIngredientes() == null || receta.getIngredientes().isEmpty()) {
            ingredientesContainer.getChildren().add(new Label("No hay ingredientes registrados."));
            return;
        }

        for (RecetaIngrediente ing : receta.getIngredientes()) {
            HBox row = new HBox(10);
            Label nombreLbl = new Label(ing.getNombreIngrediente());
            nombreLbl.setStyle("-fx-font-weight: bold; -fx-min-width: 180;");
            Label cantLbl = new Label(ing.getCantidad() + " " + ing.getUnidad());
            cantLbl.setStyle("-fx-text-fill: #555;");
            row.getChildren().addAll(nombreLbl, cantLbl);
            ingredientesContainer.getChildren().add(row);
        }
    }

    private void cargarPasos() {
        pasosContainer.getChildren().clear();
        if (receta.getPasos() == null || receta.getPasos().isEmpty()) {
            pasosContainer.getChildren().add(new Label("No hay pasos registrados."));
            return;
        }
    }

    private void cargarPasosProduccion() {
        pasosContainer.getChildren().clear();
        if (receta.getPasos() == null || receta.getPasos().isEmpty()) {
            pasosContainer.getChildren().add(new Label("No hay pasos registrados."));
            return;
        }
        for (PasoReceta paso : receta.getPasos()) {
            CheckBox cb = new CheckBox("Paso " + paso.getNumeroPaso() + ": " + paso.getTitulo());
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label descLbl = new Label(paso.getDescripcion());
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-padding: 0 0 0 25; -fx-text-fill: #666; -fx-font-size: 12px;");

            Label tiempoLbl = new Label("Tiempo: " + paso.getTiempoStr());
            tiempoLbl.setStyle("-fx-padding: 0 0 0 25; -fx-text-fill: #999; -fx-font-size: 11px; -fx-font-style: italic;");

            VBox pasoBox = new VBox(5, cb, descLbl, tiempoLbl);
            pasoBox.getStyleClass().addAll("bg-card", "border-light");
            pasoBox.setStyle("-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 3, 0, 0, 1);");
            pasoBox.setMaxWidth(Double.MAX_VALUE);

            pasosContainer.getChildren().add(pasoBox);
        }
    }

    private void mostrarPasoCapacitacion(int idx) {
        pasosContainer.getChildren().clear();
        List<PasoReceta> pasos = receta.getPasos();
        if (pasos == null || pasos.isEmpty() || idx < 0 || idx >= pasos.size()) return;

        PasoReceta paso = pasos.get(idx);
        pasoContadorLabel.setText("Paso " + (idx + 1) + " de " + pasos.size());
        anteriorPasoButton.setDisable(idx == 0);
        siguientePasoButton.setText(idx < pasos.size() - 1 ? "Siguiente ->" : "Finalizar");

        Label numLabel = new Label("Paso " + paso.getNumeroPaso());
        numLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f55580;");

        Label tituloLbl = new Label(paso.getTitulo());
        tituloLbl.getStyleClass().add("text-heading");
        tituloLbl.setStyle("-fx-font-size: 16px;");

        Label descLbl = new Label(paso.getDescripcion());
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #555; -fx-padding: 10 0;");

        Label tiempoLbl = new Label("Tiempo estimado: " + paso.getTiempoStr());
        tiempoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-font-style: italic;");

        VBox pasoBox = new VBox(15, numLabel, tituloLbl, descLbl, tiempoLbl);
        pasoBox.getStyleClass().addAll("bg-card", "border-light");
        pasoBox.setStyle("-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        pasoBox.setMaxWidth(Double.MAX_VALUE);
        pasoBox.setAlignment(Pos.TOP_CENTER);

        pasosContainer.getChildren().add(pasoBox);
    }

    @FXML
    private void anteriorPaso() {
        if (pasoActual > 0) {
            pasoActual--;
            mostrarPasoCapacitacion(pasoActual);
        }
    }

    @FXML
    private void siguientePaso() {
        List<PasoReceta> pasos = receta.getPasos();
        if (pasoActual < pasos.size() - 1) {
            pasoActual++;
            mostrarPasoCapacitacion(pasoActual);
        } else {
            cerrar();
        }
    }

    private void cargarCostos() {
        costosContainer.getChildren().clear();

        VBox card = new VBox(10);
        card.getStyleClass().addAll("bg-card", "border-light");
        card.setStyle("-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20;");

        Label titulo = new Label("Resumen de Costos y Produccion");
        titulo.getStyleClass().add("text-heading-sm");
        titulo.setStyle("-fx-font-size: 15px;");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(8);

        grid.add(new Label("Costo Estimado:"), 0, 0);
        grid.add(new Label("$" + String.format("%.2f", receta.getCostoEstimado())), 1, 0);
        grid.add(new Label("Rendimiento:"), 0, 1);
        grid.add(new Label(receta.getRendimiento() + "%"), 1, 1);
        grid.add(new Label("Desperdicio:"), 0, 2);
        grid.add(new Label(receta.getDesperdicio() + "%"), 1, 2);
        grid.add(new Label("Cantidad Producida:"), 0, 3);
        grid.add(new Label(receta.getCantidadProducida() + " unidades"), 1, 3);
        grid.add(new Label("Costo por Unidad:"), 0, 4);
        double costoUnidad = receta.getCantidadProducida() > 0 ? receta.getCostoEstimado() / receta.getCantidadProducida() : 0;
        grid.add(new Label("$" + String.format("%.2f", costoUnidad)), 1, 4);

        for (var node : grid.getChildren()) {
            if (node instanceof Label) {
                Label l = (Label) node;
                if (GridPane.getColumnIndex(l) == 0) l.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
                else l.setStyle("-fx-text-fill: #777;");
            }
        }

        card.getChildren().addAll(titulo, grid);
        costosContainer.getChildren().add(card);
    }

    @FXML private void cerrar() { ((Stage) cerrarButton.getScene().getWindow()).close(); }
}
