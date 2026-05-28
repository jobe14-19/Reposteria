package com.example.demo.controller;

import com.example.demo.dao.ProductoDAO;
import com.example.demo.model.Producto;

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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductosController {

    private static final Logger LOGGER = Logger.getLogger(ProductosController.class.getName());

    @FXML private TextField buscarField;
    @FXML private TableView<Producto> productosTable;
    @FXML private TableColumn<Producto, Integer> idColumn;
    @FXML private TableColumn<Producto, String> nombreColumn;
    @FXML private TableColumn<Producto, String> descripcionColumn;
    @FXML private TableColumn<Producto, Double> precioUnitarioColumn;
    @FXML private TableColumn<Producto, Double> costoDisenioColumn;
    @FXML private TableColumn<Producto, Integer> recetasColumn;
    @FXML private TableColumn<Producto, String> estadoColumn;
    @FXML private TableColumn<Producto, Void> accionesColumn;
    @FXML private Label totalLabel;

    private ProductoDAO productoDAO;
    private ObservableList<Producto> listaProductos;

    private static final String BADGE_BASE = "-fx-background-radius: 10; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";

    @FXML
    public void initialize() {
        productoDAO = new ProductoDAO();
        listaProductos = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        descripcionColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.length() > 60 ? item.substring(0, 60) + "..." : item);
            }
        });
        precioUnitarioColumn.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        costoDisenioColumn.setCellValueFactory(new PropertyValueFactory<>("costoDisenio"));
        recetasColumn.setCellValueFactory(new PropertyValueFactory<>("totalRecetas"));

        precioUnitarioColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        costoDisenioColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        estadoColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            private final HBox hbox = new HBox(5, badge);
            { badge.setStyle(BADGE_BASE); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String color = "Activo".equalsIgnoreCase(item) ? "#28A745" : "#DC3545";
                badge.setStyle(BADGE_BASE + "-fx-background-color: " + color + ";");
                badge.setText(item.toUpperCase());
                setGraphic(hbox);
            }
        });

        configurarColumnaAcciones();
        setupListeners();
        cargarProductos();
    }

    private void setupListeners() {
        buscarField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().isEmpty()) {
                cargarProductos();
            } else {
                String q = n.trim().toLowerCase();
                List<Producto> filtrados = listaProductos.stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(q)
                        || (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(q)))
                    .toList();
                productosTable.setItems(FXCollections.observableArrayList(filtrados));
                totalLabel.setText("Total: " + filtrados.size() + " productos");
            }
        });
    }

    private void configurarColumnaAcciones() {
        accionesColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editarBtn = new Button("Editar");
            private final Button toggleBtn = new Button();
            private final HBox hbox = new HBox(5, editarBtn, toggleBtn);
            {
                editarBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                editarBtn.setOnAction(e -> abrirModal(getTableView().getItems().get(getIndex())));
                toggleBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                toggleBtn.setOnAction(e -> toggleEstado(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                if (empty) { setGraphic(null); return; }
                Producto p = getTableView().getItems().get(getIndex());
                toggleBtn.setText("Activo".equalsIgnoreCase(p.getEstado()) ? "Desactivar" : "Activar");
                toggleBtn.setStyle("-fx-background-color: " + ("Activo".equalsIgnoreCase(p.getEstado()) ? "#f0ad4e" : "#28A745") + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
                setGraphic(hbox);
            }
        });
    }

    private void cargarProductos() {
        listaProductos.setAll(productoDAO.listarTodosIncluyendoInactivos());
        productosTable.setItems(listaProductos);
        totalLabel.setText("Total: " + listaProductos.size() + " productos");
    }

    private void toggleEstado(Producto producto) {
        String nuevo = "Activo".equalsIgnoreCase(producto.getEstado()) ? "Inactivo" : "Activo";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cambiar Estado");
        alert.setHeaderText(producto.getNombre());
        alert.setContentText("Cambiar estado a: " + nuevo + "?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (productoDAO.toggleEstado(producto.getId())) {
                cargarProductos();
            }
        }
    }

    @FXML
    private void nuevoProducto(ActionEvent event) {
        abrirModal(null);
    }

    private void abrirModal(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ProductoModal.fxml"));
            Parent root = loader.load();
            ProductoModalController controller = loader.getController();
            controller.setProductoDAO(productoDAO);
            if (producto != null) controller.setProducto(producto);

            Stage stage = new Stage();
            stage.setTitle(producto == null ? "Nuevo Producto" : "Editar Producto");
            stage.setScene(new Scene(root, 550, 400));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarProductos();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
        }
    }
}
