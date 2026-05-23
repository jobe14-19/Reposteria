package com.example.demo.controller;

import com.example.demo.dao.StockMovimientoDAO;
import com.example.demo.model.StockMovimiento;
import com.example.demo.service.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StockMovimientosController {

    private static final Logger LOGGER = Logger.getLogger(StockMovimientosController.class.getName());
    private static final int PAGE_SIZE = 20;

    @FXML private TableView<StockMovimiento> movimientosTable;
    @FXML private TableColumn<StockMovimiento, String> fechaColumn;
    @FXML private TableColumn<StockMovimiento, String> ingredienteColumn;
    @FXML private TableColumn<StockMovimiento, String> tipoColumn;
    @FXML private TableColumn<StockMovimiento, String> cantidadColumn;
    @FXML private TableColumn<StockMovimiento, Double> stockAntColumn;
    @FXML private TableColumn<StockMovimiento, Double> stockNuevoColumn;
    @FXML private TableColumn<StockMovimiento, String> motivoColumn;
    @FXML private TableColumn<StockMovimiento, String> usuarioColumn;
    @FXML private TextField buscarField;
    @FXML private Button buscarButton;
    @FXML private Button refrescarButton;
    @FXML private Button anteriorButton;
    @FXML private Button siguienteButton;
    @FXML private Label paginaLabel;
    @FXML private Label totalLabel;

    private StockMovimientoDAO stockDAO;
    private ObservableList<StockMovimiento> movimientosList;
    private int paginaActual = 1;
    private int totalPaginas = 1;
    private int totalMovimientos = 0;
    private String filtroActual = "";

    @FXML
    public void initialize() {
        stockDAO = new StockMovimientoDAO();
        movimientosList = FXCollections.observableArrayList();

        configurarTabla();
        cargarMovimientos();
        setupHandlers();
    }

    private void configurarTabla() {
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaHoraStr"));
        ingredienteColumn.setCellValueFactory(new PropertyValueFactory<>("nombreIngrediente"));
        tipoColumn.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        cantidadColumn.setCellValueFactory(new PropertyValueFactory<>("stockDiffStr"));
        stockAntColumn.setCellValueFactory(new PropertyValueFactory<>("stockAnterior"));
        stockNuevoColumn.setCellValueFactory(new PropertyValueFactory<>("stockNuevo"));
        motivoColumn.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        usuarioColumn.setCellValueFactory(new PropertyValueFactory<>("usuarioRegistra"));

        tipoColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) { setText(null); setStyle(""); return; }
                setText(tipo);
                switch (tipo.toUpperCase()) {
                    case "ENTRADA":
                        setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
                        break;
                    case "SALIDA":
                        setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                        break;
                    case "AJUSTE":
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                        break;
                    default:
                        setStyle("");
                }
            }
        });

        cantidadColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String diff, boolean empty) {
                super.updateItem(diff, empty);
                if (empty || diff == null) { setText(null); setStyle(""); return; }
                setText(diff);
                if (diff.startsWith("+")) setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
                else if (diff.startsWith("-")) setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
                else setStyle("");
            }
        });
    }

    private void setupHandlers() {
        buscarButton.setOnAction(e -> {
            filtroActual = buscarField.getText();
            paginaActual = 1;
            cargarMovimientos();
        });
        buscarField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                filtroActual = "";
                paginaActual = 1;
                cargarMovimientos();
            }
        });
        refrescarButton.setOnAction(e -> {
            filtroActual = "";
            buscarField.clear();
            paginaActual = 1;
            cargarMovimientos();
        });
        anteriorButton.setOnAction(e -> {
            if (paginaActual > 1) { paginaActual--; cargarMovimientos(); }
        });
        siguienteButton.setOnAction(e -> {
            if (paginaActual < totalPaginas) { paginaActual++; cargarMovimientos(); }
        });
    }

    private void cargarMovimientos() {
        try {
            totalMovimientos = stockDAO.listarMovimientosCount(filtroActual);
            totalPaginas = Math.max(1, (int) Math.ceil((double) totalMovimientos / PAGE_SIZE));
            if (paginaActual > totalPaginas) paginaActual = totalPaginas;

            int offset = (paginaActual - 1) * PAGE_SIZE;
            movimientosList.setAll(stockDAO.listarMovimientos(filtroActual, offset, PAGE_SIZE));
            movimientosTable.setItems(movimientosList);

            paginaLabel.setText("Página " + paginaActual + " de " + totalPaginas);
            totalLabel.setText("Total: " + totalMovimientos + " movimientos");
            anteriorButton.setDisable(paginaActual <= 1);
            siguienteButton.setDisable(paginaActual >= totalPaginas);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar movimientos: {0}", e.getMessage());
        }
    }
}
