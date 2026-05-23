package com.example.demo.controller;

import com.example.demo.dao.ProductoDAO;
import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Producto;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.PasoReceta;
import com.example.demo.model.Receta.RecetaIngrediente;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RecetaWizardController {

    private static final Logger LOGGER = Logger.getLogger(RecetaWizardController.class.getName());
    private static final String[] STEP_TITLES = {"Informacion", "Ingredientes", "Procedimiento", "Costos", "Confirmacion"};
    private static final String[] CATEGORIAS = {"Pasteles", "Galletas", "Postres", "Panaderia", "Reposteria Fina", "Decoracion", "Otros"};

    @FXML private Label tituloLabel;
    @FXML private HBox stepperBar;
    @FXML private StackPane contenidoStack;
    @FXML private Button atrasButton, siguienteButton, guardarButton, cancelarButton, agregarPasoButton;

    // Step 1
    @FXML private TextField nombreRecetaField;
    @FXML private ComboBox<Producto> productoComboBox;
    @FXML private ComboBox<String> categoriaComboBox;
    @FXML private Spinner<Integer> tiempoSpinner;
    @FXML private Spinner<Double> cantidadSpinner;
    @FXML private TextArea descripcionArea;

    // Step 2
    @FXML private TableView<IngredienteItem> ingredientesTable;
    @FXML private TableColumn<IngredienteItem, Boolean> selColumn;
    @FXML private TableColumn<IngredienteItem, String> ingNombreColumn;
    @FXML private TableColumn<IngredienteItem, Double> ingCantidadColumn;
    @FXML private TableColumn<IngredienteItem, String> ingUnidadColumn;
    @FXML private TableColumn<IngredienteItem, String> stockColumn;

    // Step 3
    @FXML private VBox pasosContainer;

    // Step 4
    @FXML private TextField costoField;
    @FXML private Spinner<Double> rendimientoSpinner;
    @FXML private Spinner<Double> desperdicioSpinner;
    @FXML private Label costoResumenLabel, rendimientoResumenLabel;

    // Step 5
    @FXML private Label resumenNombreLabel, resumenProductoLabel, resumenCategoriaLabel;
    @FXML private Label resumenTiempoLabel, resumenCantidadLabel, resumenCostoLabel;
    @FXML private Label resumenIngredientesLabel, resumenPasosLabel, erroresLabel;

    private RecetaDAO recetaDAO;
    private Receta recetaEdicion;
    private int pasoActual = 0;
    private final int totalPasos = 5;
    private ObservableList<IngredienteItem> ingredientesList;
    private final List<Circle> stepCircles = new ArrayList<>();
    private final List<Label> stepLabels = new ArrayList<>();

    public void setRecetaDAO(RecetaDAO dao) { this.recetaDAO = dao; }

    public void setReceta(Receta r) {
        this.recetaEdicion = r;
        if (r != null) {
            tituloLabel.setText("Editar Receta");
            nombreRecetaField.setText(r.getNombreReceta());
            descripcionArea.setText(r.getDescripcion());
            if (r.getCategoria() != null) categoriaComboBox.getSelectionModel().select(r.getCategoria());
            tiempoSpinner.getValueFactory().setValue(r.getTiempoPreparacion());
            cantidadSpinner.getValueFactory().setValue(r.getCantidadProducida());
            costoField.setText(String.format("%.2f", r.getCostoEstimado()));
            rendimientoSpinner.getValueFactory().setValue(r.getRendimiento());
            desperdicioSpinner.getValueFactory().setValue(r.getDesperdicio());
        }
    }

    @FXML
    public void initialize() {
        if (recetaDAO == null) recetaDAO = new RecetaDAO();
        ingredientesList = FXCollections.observableArrayList();

        configurarCombos();
        configurarSpinners();
        configurarTablaIngredientes();
        construirStepper();
        agregarPasoButton.setOnAction(e -> agregarPaso());
        cargarIngredientesDisponibles();
        seleccionarProductoEdicion();
        marcarIngredientesEdicion();
        cargarPasosEdicion();
        mostrarPaso(0);
    }

    private void configurarCombos() {
        ProductoDAO productoDAO = new ProductoDAO();
        productoComboBox.setItems(FXCollections.observableArrayList(productoDAO.listarTodos()));
        productoComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Producto p) { return p == null ? "" : p.getNombre(); }
            @Override public Producto fromString(String s) { return null; }
        });

        categoriaComboBox.getItems().addAll(CATEGORIAS);
        categoriaComboBox.getSelectionModel().selectFirst();
    }

    private void configurarSpinners() {
        tiempoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1440, 30, 5));
        tiempoSpinner.setEditable(true);
        cantidadSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 1000.0, 1.0, 0.5));
        cantidadSpinner.setEditable(true);
        rendimientoSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 95, 5));
        rendimientoSpinner.setEditable(true);
        desperdicioSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 5, 1));
        desperdicioSpinner.setEditable(true);

        costoField.textProperty().addListener((obs, o, n) -> actualizarResumenCostos());
        rendimientoSpinner.valueProperty().addListener((obs, o, n) -> actualizarResumenCostos());
        desperdicioSpinner.valueProperty().addListener((obs, o, n) -> actualizarResumenCostos());
    }

    private void configurarTablaIngredientes() {
        ingNombreColumn.setCellValueFactory(d -> d.getValue().nombreProperty());
        ingUnidadColumn.setCellValueFactory(d -> d.getValue().unidadProperty());
        stockColumn.setCellValueFactory(d -> d.getValue().stockStrProperty());

        selColumn.setCellValueFactory(d -> d.getValue().seleccionadoProperty());
        selColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selColumn));
        selColumn.setEditable(true);
        ingredientesTable.setEditable(true);

        ingCantidadColumn.setCellValueFactory(d -> d.getValue().cantidadProperty().asObject());
        ingCantidadColumn.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Double> spinner = new Spinner<>(0.0, 99999.0, 0.0, 0.5);
            { spinner.setEditable(true);
              spinner.valueProperty().addListener((obs, old, val) -> {
                  int idx = getIndex();
                  if (idx >= 0 && idx < getTableView().getItems().size())
                      getTableView().getItems().get(idx).setCantidad(val);
              }); }
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                int idx = getIndex();
                IngredienteItem i = getTableView().getItems().get(idx);
                spinner.getValueFactory().setValue(i.getCantidad());
                setGraphic(spinner);
            }
        });
        ingredientesTable.setItems(ingredientesList);
    }

    private void construirStepper() {
        stepperBar.getChildren().clear();
        for (int i = 0; i < totalPasos; i++) {
            Circle circle = new Circle(14);
            circle.setStroke(Color.valueOf("#8B5E3C"));
            circle.setStrokeWidth(2);
            circle.setFill(Color.valueOf("#EEEEEE"));

            Label numLabel = new Label(String.valueOf(i + 1));
            numLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            numLabel.setTextFill(Color.valueOf("#999999"));

            StackPane circleContainer = new StackPane(circle, numLabel);

            Label textLabel = new Label(STEP_TITLES[i]);
            textLabel.setFont(Font.font("System", 10));
            textLabel.setTextFill(Color.valueOf("#999999"));

            VBox stepBox = new VBox(4, circleContainer, textLabel);
            stepBox.setAlignment(Pos.CENTER);
            stepBox.setPrefWidth(100);

            stepCircles.add(circle);
            stepLabels.add(textLabel);

            stepperBar.getChildren().add(stepBox);

            if (i < totalPasos - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setPrefWidth(40);
                line.setStyle("-fx-background-color: #DDDDDD; -fx-margin: 0 0 16 0;");
                stepperBar.getChildren().add(line);
            }
        }
    }

    private void mostrarPaso(int paso) {
        pasoActual = paso;
        for (int i = 0; i < contenidoStack.getChildren().size(); i++) {
            contenidoStack.getChildren().get(i).setVisible(i == paso);
            contenidoStack.getChildren().get(i).setManaged(i == paso);
        }

        for (int i = 0; i < stepCircles.size(); i++) {
            Circle c = stepCircles.get(i);
            Label l = stepLabels.get(i);
            if (i < paso) {
                c.setFill(Color.valueOf("#8B5E3C"));
                c.setStroke(Color.valueOf("#8B5E3C"));
                l.setTextFill(Color.valueOf("#8B5E3C"));
            } else if (i == paso) {
                c.setFill(Color.valueOf("#f55580"));
                c.setStroke(Color.valueOf("#f55580"));
                l.setTextFill(Color.valueOf("#f55580"));
            } else {
                c.setFill(Color.valueOf("#EEEEEE"));
                c.setStroke(Color.valueOf("#CCCCCC"));
                l.setTextFill(Color.valueOf("#999999"));
            }
        }

        atrasButton.setDisable(paso == 0);
        siguienteButton.setVisible(paso < totalPasos - 1);
        guardarButton.setVisible(paso == totalPasos - 1);

        if (paso == totalPasos - 1) actualizarResumen();
    }

    @FXML private void irAtras(ActionEvent e) { if (pasoActual > 0) mostrarPaso(pasoActual - 1); }

    @FXML private void irSiguiente(ActionEvent e) {
        if (pasoActual < totalPasos - 1) {
            if (!validarPaso(pasoActual)) return;
            if (pasoActual == 4) {
                if (recetaEdicion != null) actualizarResumen(); else actualizarResumen();
            }
            if (pasoActual == 1) cargarIngredientesEnResumen();
            if (pasoActual == 2) cargarPasosEnResumen();
            mostrarPaso(pasoActual + 1);
        }
    }

    private boolean validarPaso(int paso) {
        switch (paso) {
            case 0:
                if (nombreRecetaField.getText() == null || nombreRecetaField.getText().trim().isEmpty()) {
                    mostrarError("Campo requerido", "Ingrese el nombre de la receta."); return false;
                }
                if (productoComboBox.getValue() == null) {
                    mostrarError("Campo requerido", "Seleccione un producto asociado."); return false;
                }
                return true;
            case 1:
                long seleccionados = ingredientesList.stream().filter(IngredienteItem::isSeleccionado).filter(i -> i.getCantidad() > 0).count();
                if (seleccionados == 0) {
                    mostrarError("Sin ingredientes", "Debe seleccionar al menos un ingrediente con cantidad mayor a 0."); return false;
                }
                return true;
            case 2:
                int totalPasosProc = pasosContainer.getChildren().size();
                if (totalPasosProc == 0) {
                    mostrarError("Sin pasos", "Agregue al menos un paso al procedimiento."); return false;
                }
                for (javafx.scene.Node node : pasosContainer.getChildren()) {
                    if (node instanceof VBox) {
                        VBox pasoBox = (VBox) node;
                        TextField titleField = (TextField) ((HBox) ((VBox) pasoBox.getChildren().get(0)).getChildren().get(0)).getChildren().get(1);
                        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
                            mostrarError("Campo requerido", "Cada paso debe tener un titulo."); return false;
                        }
                    }
                }
                return true;
            case 3:
                try {
                    double costo = Double.parseDouble(costoField.getText().trim());
                    if (costo < 0) { mostrarError("Valor invalido", "El costo debe ser mayor o igual a 0."); return false; }
                } catch (NumberFormatException ex) {
                    mostrarError("Valor invalido", "Ingrese un valor numerico valido para el costo."); return false;
                }
                return true;
            default:
                return true;
        }
    }

    private void actualizarResumenCostos() {
        try {
            double costo = Double.parseDouble(costoField.getText().trim());
            double rend = rendimientoSpinner.getValue();
            double desp = desperdicioSpinner.getValue();
            double cantidad = cantidadSpinner.getValue();
            double costoUnidad = cantidad > 0 ? costo / cantidad : 0;
            costoResumenLabel.setText(String.format("Costo total: $%.2f  |  Costo por unidad: $%.2f", costo, costoUnidad));
            rendimientoResumenLabel.setText(String.format("Rendimiento: %.0f%%  |  Desperdicio: %.0f%%  |  Prod. util: %.0f%%", rend, desp, rend - desp));
        } catch (NumberFormatException e) {
            costoResumenLabel.setText("Costo total: $0.00  |  Costo por unidad: $0.00");
        }
    }

    private void cargarIngredientesDisponibles() {
        ingredientesList.clear();
        List<RecetaIngrediente> disponibles = recetaDAO.obtenerIngredientesDisponibles();
        for (RecetaIngrediente ri : disponibles) {
            double stock = obtenerStockIngrediente(ri.getIdIngrediente());
            ingredientesList.add(new IngredienteItem(ri.getIdIngrediente(),
                ri.getNombreIngrediente(), 0.0, ri.getUnidad(), false, stock));
        }
        ingredientesTable.setItems(ingredientesList);
    }

    private double obtenerStockIngrediente(int idIngrediente) {
        try {
            return new com.example.demo.dao.StockMovimientoDAO().getStockActual(idIngrediente);
        } catch (Exception e) { return 0; }
    }

    private void seleccionarProductoEdicion() {
        if (recetaEdicion != null) {
            for (Producto p : productoComboBox.getItems()) {
                if (p.getId() == recetaEdicion.getIdProducto()) {
                    productoComboBox.getSelectionModel().select(p); break;
                }
            }
        }
    }

    private void marcarIngredientesEdicion() {
        if (recetaEdicion != null && recetaEdicion.getIngredientes() != null) {
            for (RecetaIngrediente ri : recetaEdicion.getIngredientes()) {
                for (IngredienteItem item : ingredientesList) {
                    if (item.getIdIngrediente() == ri.getIdIngrediente()) {
                        item.setSeleccionado(true);
                        item.setCantidad(ri.getCantidad());
                        break;
                    }
                }
            }
        }
    }

    private void cargarPasosEdicion() {
        if (recetaEdicion != null && recetaEdicion.getPasos() != null) {
            for (PasoReceta paso : recetaEdicion.getPasos()) {
                agregarPasoUI(paso.getTitulo(), paso.getDescripcion(), paso.getTiempoEstimado());
            }
        }
    }

    @FXML private void guardarReceta(ActionEvent e) {
        if (!validarPaso(0) || !validarPaso(1) || !validarPaso(2) || !validarPaso(3)) return;
        String errores = validarCompleto();
        if (!errores.isEmpty()) {
            erroresLabel.setText(errores);
            mostrarError("Errores de validacion", errores);
            return;
        }

        Producto producto = productoComboBox.getValue();
        List<RecetaIngrediente> ingredientes = ingredientesList.stream()
            .filter(IngredienteItem::isSeleccionado).filter(i -> i.getCantidad() > 0)
            .map(i -> new RecetaIngrediente(i.getIdIngrediente(), i.getNombre(), i.getCantidad(), i.getUnidad()))
            .collect(Collectors.toList());
        List<PasoReceta> pasos = new ArrayList<>();
        int numPaso = 1;
        for (javafx.scene.Node node : pasosContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox pasoBox = (VBox) node;
                TextField titleField = (TextField) ((HBox) ((VBox) pasoBox.getChildren().get(0)).getChildren().get(0)).getChildren().get(1);
                TextArea descArea = (TextArea) pasoBox.getChildren().get(1);
                Spinner<Integer> timeSpin = (Spinner<Integer>) ((HBox) pasoBox.getChildren().get(2)).getChildren().get(1);
                pasos.add(new PasoReceta(numPaso++, titleField.getText().trim(), descArea.getText().trim(), timeSpin.getValue(), ""));
            }
        }

        double costo;
        try { costo = Double.parseDouble(costoField.getText().trim()); } catch (NumberFormatException ex) { costo = 0; }

        if (recetaEdicion == null) {
            Receta nueva = new Receta(0, producto.getId(), producto.getNombre(),
                nombreRecetaField.getText().trim(), descripcionArea.getText().trim(),
                categoriaComboBox.getValue(), tiempoSpinner.getValue(), cantidadSpinner.getValue(),
                "", cantidadSpinner.getValue(), costo, rendimientoSpinner.getValue(),
                desperdicioSpinner.getValue(), "Activo");
            nueva.setIngredientes(ingredientes);
            nueva.setPasos(pasos);
            int id = recetaDAO.insertar(nueva, ingredientes, pasos);
            if (id > 0) { mostrarMensaje("Receta creada", "La receta se ha creado correctamente."); cerrar(); }
            else mostrarError("Error", "No se pudo crear la receta.");
        } else {
            recetaEdicion.setIdProducto(producto.getId());
            recetaEdicion.setNombreProducto(producto.getNombre());
            recetaEdicion.setNombreReceta(nombreRecetaField.getText().trim());
            recetaEdicion.setDescripcion(descripcionArea.getText().trim());
            recetaEdicion.setCategoria(categoriaComboBox.getValue());
            recetaEdicion.setTiempoPreparacion(tiempoSpinner.getValue());
            recetaEdicion.setCantidadProducida(cantidadSpinner.getValue());
            recetaEdicion.setPorciones(cantidadSpinner.getValue());
            recetaEdicion.setCostoEstimado(costo);
            recetaEdicion.setRendimiento(rendimientoSpinner.getValue());
            recetaEdicion.setDesperdicio(desperdicioSpinner.getValue());
            if (recetaDAO.actualizar(recetaEdicion, ingredientes, pasos)) {
                mostrarMensaje("Receta actualizada", "La receta se ha actualizado correctamente.");
                cerrar();
            } else mostrarError("Error", "No se pudo actualizar la receta.");
        }
    }

    private String validarCompleto() {
        StringBuilder sb = new StringBuilder();
        if (nombreRecetaField.getText() == null || nombreRecetaField.getText().trim().isEmpty()) sb.append("- Nombre de la receta\n");
        if (productoComboBox.getValue() == null) sb.append("- Producto asociado\n");
        long ingSel = ingredientesList.stream().filter(IngredienteItem::isSeleccionado).filter(i -> i.getCantidad() > 0).count();
        if (ingSel == 0) sb.append("- Al menos un ingrediente con cantidad\n");
        if (pasosContainer.getChildren().isEmpty()) sb.append("- Al menos un paso en el procedimiento\n");
        try { Double.parseDouble(costoField.getText().trim()); } catch (Exception e) { sb.append("- Costo estimado valido\n"); }
        String result = sb.toString();
        return result.isEmpty() ? "" : "Corrija los siguientes campos:\n" + result;
    }

    private void actualizarResumen() {
        resumenNombreLabel.setText("Nombre: " + nombreRecetaField.getText());
        Producto p = productoComboBox.getValue();
        resumenProductoLabel.setText("Producto: " + (p != null ? p.getNombre() : "-"));
        resumenCategoriaLabel.setText("Categoria: " + categoriaComboBox.getValue());
        resumenTiempoLabel.setText("Tiempo: " + tiempoSpinner.getValue() + " min");
        resumenCantidadLabel.setText("Cantidad: " + cantidadSpinner.getValue());
        resumenCostoLabel.setText("Costo: $" + costoField.getText());
        resumenIngredientesLabel.setText("Ingredientes: " + ingredientesList.stream().filter(IngredienteItem::isSeleccionado).filter(i -> i.getCantidad() > 0).count() + " seleccionados");
        resumenPasosLabel.setText("Pasos: " + pasosContainer.getChildren().size());
        erroresLabel.setText("");
    }

    private void cargarIngredientesEnResumen() { resumenIngredientesLabel.setText("Ingredientes: " + ingredientesList.stream().filter(IngredienteItem::isSeleccionado).count() + " seleccionados"); }
    private void cargarPasosEnResumen() { resumenPasosLabel.setText("Pasos: " + pasosContainer.getChildren().size()); }

    @FXML
    private void agregarPaso() {
        agregarPasoUI("", "", 5);
    }

    private void agregarPasoUI(String titulo, String descripcion, int tiempo) {
        int num = pasosContainer.getChildren().size() + 1;

        Label numLabel = new Label("Paso " + num);
        numLabel.getStyleClass().addAll("text-heading-sm");
        numLabel.setStyle("-fx-font-size: 13px;");

        Button eliminarBtn = new Button("X");
        eliminarBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 11px;");
        eliminarBtn.setOnAction(e -> { pasosContainer.getChildren().remove(((Button) e.getSource()).getParent().getParent().getParent()); renumerarPasos(); });

        TextField tituloField = new TextField(titulo);
        tituloField.setPromptText("Titulo del paso");
        tituloField.getStyleClass().addAll("bg-input", "border-light");
        tituloField.setStyle("-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 13px;");

        HBox headerBox = new HBox(10, numLabel, tituloField);
        HBox.setHgrow(tituloField, Priority.ALWAYS);

        VBox header = new VBox(5, headerBox, eliminarBtn);
        header.setAlignment(Pos.TOP_RIGHT);

        TextArea descArea = new TextArea(descripcion);
        descArea.setPromptText("Describa el paso detalladamente...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.getStyleClass().addAll("bg-input", "border-light");
        descArea.setStyle("-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12px;");

        Label tiempoLabel = new Label("Tiempo (min):");
        tiempoLabel.getStyleClass().add("text-muted-sm");
        tiempoLabel.setStyle("-fx-font-size: 11px;");
        Spinner<Integer> tiempoPasoSpin = new Spinner<>(0, 999, tiempo, 5);
        tiempoPasoSpin.setEditable(true);
        tiempoPasoSpin.setPrefWidth(100);
        tiempoPasoSpin.setStyle("-fx-font-size: 12px;");

        HBox tiempoBox = new HBox(10, tiempoLabel, tiempoPasoSpin);

        VBox pasoBox = new VBox(8, header, descArea, tiempoBox);
        pasoBox.getStyleClass().addAll("bg-card", "border-light");
        pasoBox.setStyle("-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        pasoBox.setMaxWidth(Double.MAX_VALUE);

        pasosContainer.getChildren().add(pasoBox);
    }

    private void renumerarPasos() {
        int num = 1;
        for (javafx.scene.Node node : pasosContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox pasoBox = (VBox) node;
                VBox header = (VBox) pasoBox.getChildren().get(0);
                HBox headerBox = (HBox) header.getChildren().get(0);
                Label numLbl = (Label) headerBox.getChildren().get(0);
                numLbl.setText("Paso " + num++);
            }
        }
    }

    @FXML private void cancelar(ActionEvent e) { cerrar(); }
    private void cerrar() { ((Stage) cancelarButton.getScene().getWindow()).close(); }

    private void mostrarError(String t, String m) { mostrarAlerta(Alert.AlertType.ERROR, t, m); }
    private void mostrarMensaje(String t, String m) { mostrarAlerta(Alert.AlertType.INFORMATION, t, m); }
    private void mostrarAlerta(Alert.AlertType tipo, String t, String m) {
        Alert a = new Alert(tipo); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    public static class IngredienteItem {
        private final int idIngrediente;
        private final SimpleStringProperty nombre, unidad, stockStr;
        private final SimpleDoubleProperty cantidad;
        private final SimpleBooleanProperty seleccionado;

        public IngredienteItem(int id, String nombre, double cant, String unid, boolean sel, double stock) {
            this.idIngrediente = id;
            this.nombre = new SimpleStringProperty(nombre);
            this.cantidad = new SimpleDoubleProperty(cant);
            this.unidad = new SimpleStringProperty(unid);
            this.seleccionado = new SimpleBooleanProperty(sel);
            this.stockStr = new SimpleStringProperty(stock > 0 ? String.format("%.1f %s", stock, unid) : "Sin stock");
        }
        public int getIdIngrediente() { return idIngrediente; }
        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public double getCantidad() { return cantidad.get(); }
        public SimpleDoubleProperty cantidadProperty() { return cantidad; }
        public void setCantidad(double c) { cantidad.set(c); }
        public String getUnidad() { return unidad.get(); }
        public SimpleStringProperty unidadProperty() { return unidad; }
        public boolean isSeleccionado() { return seleccionado.get(); }
        public SimpleBooleanProperty seleccionadoProperty() { return seleccionado; }
        public void setSeleccionado(boolean s) { seleccionado.set(s); }
        public String getStockStr() { return stockStr.get(); }
        public SimpleStringProperty stockStrProperty() { return stockStr; }
    }
}
