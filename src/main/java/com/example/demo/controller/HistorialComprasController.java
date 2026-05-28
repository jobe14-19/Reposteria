package com.example.demo.controller;

import com.example.demo.model.CompraHistorial;
import com.example.demo.model.CompraHistorial.CompraDetalle;
import com.example.demo.util.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistorialComprasController {

    private static final Logger LOGGER = Logger.getLogger(HistorialComprasController.class.getName());
    private static final int PAGE_SIZE = 15;

    @FXML private TableView<CompraHistorial> comprasTable;
    @FXML private TableColumn<CompraHistorial, Integer> idColumn;
    @FXML private TableColumn<CompraHistorial, String> proveedorColumn;
    @FXML private TableColumn<CompraHistorial, String> fechaColumn;
    @FXML private TableColumn<CompraHistorial, Double> totalColumn;
    @FXML private TableColumn<CompraHistorial, Integer> productosColumn;
    @FXML private TableColumn<CompraHistorial, String> usuarioColumn;
    @FXML private TableColumn<CompraHistorial, Void> accionesColumn;
    @FXML private TextField buscarField;
    @FXML private DatePicker fechaDesdePicker;
    @FXML private DatePicker fechaHastaPicker;
    @FXML private Button filtrarButton;
    @FXML private Button refrescarButton;
    @FXML private Button anteriorButton;
    @FXML private Button siguienteButton;
    @FXML private Label paginaLabel;
    @FXML private Label totalLabel;

    private DatabaseConnection dbConnection;
    private ObservableList<CompraHistorial> comprasList;
    private int paginaActual = 1;
    private int totalPaginas = 1;
    private int totalCompras = 0;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        comprasList = FXCollections.observableArrayList();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("idCompra"));
        proveedorColumn.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        productosColumn.setCellValueFactory(new PropertyValueFactory<>("totalProductos"));
        usuarioColumn.setCellValueFactory(new PropertyValueFactory<>("usuarioRegistra"));

        totalColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("$%.2f", total));
            }
        });

        accionesColumn.setCellFactory(col -> new TableCell<>() {
            private final Button verBtn = new Button("Ver Detalle");
            { verBtn.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
              verBtn.setOnAction(e -> verDetalle(getTableView().getItems().get(getIndex()))); }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : verBtn);
            }
        });

        filtrarButton.setOnAction(e -> { paginaActual = 1; cargarCompras(); });
        refrescarButton.setOnAction(e -> {
            buscarField.clear();
            fechaDesdePicker.setValue(null);
            fechaHastaPicker.setValue(null);
            paginaActual = 1;
            cargarCompras();
        });
        anteriorButton.setOnAction(e -> { if (paginaActual > 1) { paginaActual--; cargarCompras(); } });
        siguienteButton.setOnAction(e -> { if (paginaActual < totalPaginas) { paginaActual++; cargarCompras(); } });

        buscarField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) { paginaActual = 1; cargarCompras(); }
        });

        cargarCompras();
    }

    private void cargarCompras() {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.id_compra, pv.nombre as proveedor, FORMAT(c.fecha_compra, 'yyyy-MM-dd HH:mm') as fecha_compra, "
            + "c.total, c.usuario_registra, "
            + "(SELECT COUNT(*) FROM compra_detalles cd WHERE cd.id_compra = c.id_compra) as total_productos "
            + "FROM compras c INNER JOIN proveedores pv ON c.id_proveedor = pv.id_proveedor WHERE 1=1 ");

        String busqueda = buscarField.getText();
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append("AND (pv.nombre LIKE ? OR CAST(c.id_compra AS VARCHAR) LIKE ? OR c.usuario_registra LIKE ?) ");
            String like = "%" + busqueda.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (fechaDesdePicker.getValue() != null) {
            sql.append("AND c.fecha_compra >= ? ");
            params.add(Date.valueOf(fechaDesdePicker.getValue()));
        }
        if (fechaHastaPicker.getValue() != null) {
            sql.append("AND c.fecha_compra <= ? ");
            params.add(Date.valueOf(fechaHastaPicker.getValue().plusDays(1)));
        }

        String countSql = sql.toString().replaceFirst("SELECT.*FROM", "SELECT COUNT(*) FROM");
        countSql = countSql.substring(0, countSql.indexOf("ORDER BY") > 0 ? countSql.indexOf("ORDER BY") : countSql.length());

        try (Connection conn = dbConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
                try (ResultSet rs = stmt.executeQuery()) {
                    totalCompras = rs.next() ? rs.getInt(1) : 0;
                }
            }

            totalPaginas = Math.max(1, (int) Math.ceil((double) totalCompras / PAGE_SIZE));
            if (paginaActual > totalPaginas) paginaActual = totalPaginas;

            sql.append(" ORDER BY c.fecha_compra DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
            params.add((paginaActual - 1) * PAGE_SIZE);
            params.add(PAGE_SIZE);

            comprasList.clear();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        comprasList.add(new CompraHistorial(
                            rs.getInt("id_compra"), rs.getString("proveedor"),
                            rs.getString("fecha_compra"), rs.getDouble("total"),
                            rs.getString("usuario_registra"), rs.getInt("total_productos"), "Completado"));
                    }
                }
            }

            comprasTable.setItems(comprasList);
            paginaLabel.setText("Pagina " + paginaActual + " de " + totalPaginas);
            totalLabel.setText("Total: " + totalCompras + " compras");
            anteriorButton.setDisable(paginaActual <= 1);
            siguienteButton.setDisable(paginaActual >= totalPaginas);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar historial: {0}", e.getMessage());
        }
    }

    private void verDetalle(CompraHistorial compra) {
        try (Connection conn = dbConnection.getConnection()) {
            String sql = "SELECT p.nombre as producto, cd.cantidad, '' as unidad, "
                + "cd.precio_unitario, cd.descuento, cd.subtotal "
                + "FROM compra_detalles cd INNER JOIN productos p ON cd.id_producto = p.id_producto "
                + "WHERE cd.id_compra = ?";
            List<CompraDetalle> detalles = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, compra.getIdCompra());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new CompraDetalle(rs.getString("producto"),
                            rs.getDouble("cantidad"), rs.getString("unidad"),
                            rs.getDouble("precio_unitario"), rs.getDouble("descuento"),
                            rs.getDouble("subtotal")));
                    }
                }
            }
            compra.setDetalles(detalles);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar detalles: {0}", e.getMessage());
        }
        mostrarDetalleCompra(compra);
    }

    private void mostrarDetalleCompra(CompraHistorial compra) {
        Stage stage = new Stage();
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("bg-primary");

        Label title = new Label("Detalle de Compra #" + compra.getIdCompra());
        title.getStyleClass().add("text-heading-lg");
        title.setStyle("-fx-font-size: 20px;");

        GridPane info = new GridPane();
        info.setHgap(15); info.setVgap(8);
        info.addRow(0, new Label("Proveedor:"), new Label(compra.getProveedor()));
        info.addRow(1, new Label("Fecha:"), new Label(compra.getFechaCompra()));
        info.addRow(2, new Label("Total:"), new Label(String.format("$%.2f", compra.getTotal())));
        info.addRow(3, new Label("Registrado por:"), new Label(compra.getUsuarioRegistra()));

        TableView<CompraDetalle> table = new TableView<>();
        TableColumn<CompraDetalle, String> prodCol = new TableColumn<>("Producto");
        prodCol.setPrefWidth(200);
        prodCol.setCellValueFactory(new PropertyValueFactory<>("producto"));
        TableColumn<CompraDetalle, Double> cantCol = new TableColumn<>("Cantidad");
        cantCol.setPrefWidth(80);
        cantCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        TableColumn<CompraDetalle, Double> puCol = new TableColumn<>("Precio Unit.");
        puCol.setPrefWidth(100);
        puCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        puCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });
        TableColumn<CompraDetalle, Double> subCol = new TableColumn<>("Subtotal");
        subCol.setPrefWidth(100);
        subCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        subCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });
        table.getColumns().addAll(prodCol, cantCol, puCol, subCol);
        table.setItems(FXCollections.observableArrayList(compra.getDetalles()));
        table.setPrefHeight(200);

        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().addAll("btn-primary", "btn-sm");
        cerrar.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, info, table, cerrar);
        stage.setScene(new Scene(root, 600, 450));
        stage.setTitle("Detalle Compa #" + compra.getIdCompra());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }
}
