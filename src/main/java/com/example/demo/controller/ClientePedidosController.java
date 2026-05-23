package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientePedidosController {

    private static final Logger LOGGER = Logger.getLogger(ClientePedidosController.class.getName());

    // SQL filtered by username
    private static final String SQL_MIS_PEDIDOS =
        "SELECT id_pedido, producto, libras, FORMAT(fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, total, estado FROM pedidos WHERE username = ? ORDER BY id_pedido DESC";

    @FXML private Label totalLabel;
    @FXML private TableView<PedidoCliente> pedidosTable;
    @FXML private TableColumn<PedidoCliente, Integer> idColumn;
    @FXML private TableColumn<PedidoCliente, String> productoColumn;
    @FXML private TableColumn<PedidoCliente, Double> librasColumn;
    @FXML private TableColumn<PedidoCliente, String> fechaColumn;
    @FXML private TableColumn<PedidoCliente, Double> totalColumn;
    @FXML private TableColumn<PedidoCliente, String> estadoColumn;

    private SessionManager session;
    private DatabaseConnection dbConnection;

    @FXML
    public void initialize() {
        session = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();
        configurarTabla();
        cargarPedidos();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
        fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void cargarPedidos() {
        String username = session.getUsuarioActual();
        ObservableList<PedidoCliente> list = FXCollections.observableArrayList();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_MIS_PEDIDOS)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new PedidoCliente(
                        rs.getInt("id_pedido"),
                        rs.getString("producto"),
                        rs.getDouble("libras"),
                        rs.getString("fecha_entrega"),
                        rs.getDouble("total"),
                        rs.getString("estado")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error cargar pedidos: {0}", e.getMessage());
        }
        pedidosTable.setItems(list);
        totalLabel.setText("Total: " + list.size() + " pedido(s)");
    }

    @FXML
    private void nuevoPedido(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ClientePedidoForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 750));
            stage.setTitle("Nuevo Pedido - Pastel Personalizado");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarPedidos();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir formulario: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir el formulario de pedido");
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class PedidoCliente {
        private int id;
        private String producto;
        private double libras;
        private String fechaEntrega;
        private double total;
        private String estado;

        public PedidoCliente(int id, String producto, double libras, String fechaEntrega, double total, String estado) {
            this.id = id;
            this.producto = producto;
            this.libras = libras;
            this.fechaEntrega = fechaEntrega;
            this.total = total;
            this.estado = estado;
        }

        public int getId() { return id; }
        public String getProducto() { return producto; }
        public double getLibras() { return libras; }
        public String getFechaEntrega() { return fechaEntrega; }
        public double getTotal() { return total; }
        public String getEstado() { return estado; }
    }
}
