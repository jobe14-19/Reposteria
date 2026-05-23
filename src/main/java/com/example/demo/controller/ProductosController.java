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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductosController {

    private static final Logger LOGGER = Logger.getLogger(ProductosController.class.getName());

    @FXML private TableView<Producto> productosTable;
    @FXML private TableColumn<Producto, Integer> idColumn;
    @FXML private TableColumn<Producto, String> nombreColumn;
    @FXML private TableColumn<Producto, Double> precioBaseColumn;
    @FXML private TableColumn<Producto, Double> precioUnitarioColumn;
    @FXML private TableColumn<Producto, Double> costoDisenioColumn;
    @FXML private TableColumn<Producto, Integer> recetasColumn;
    @FXML private TableColumn<Producto, Void> accionesColumn;
    @FXML private Label totalLabel;

    private ProductoDAO productoDAO;
    private ObservableList<Producto> listaProductos;

    @FXML
    public void initialize() {
        productoDAO = new ProductoDAO();
        listaProductos = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        precioBaseColumn.setCellValueFactory(new PropertyValueFactory<>("precioBase"));
        precioUnitarioColumn.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        costoDisenioColumn.setCellValueFactory(new PropertyValueFactory<>("costoDisenio"));
        recetasColumn.setCellValueFactory(new PropertyValueFactory<>("totalRecetas"));

        precioBaseColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        precioUnitarioColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        costoDisenioColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        configurarColumnaAcciones();
        cargarProductos();
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
                eliminarBtn.setOnAction(e -> eliminarProducto(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void cargarProductos() {
        listaProductos.setAll(productoDAO.listarTodos());
        productosTable.setItems(listaProductos);
        totalLabel.setText("Total: " + listaProductos.size() + " productos");
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

    private void eliminarProducto(Producto producto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminacion");
        alert.setHeaderText("Eliminar producto: " + producto.getNombre() + "?");
        alert.setContentText("El producto pasara a estado Inactivo.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (productoDAO.eliminar(producto.getId())) {
                cargarProductos();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Eliminado");
                info.setHeaderText("Producto eliminado");
                info.show();
            }
        }
    }
}
