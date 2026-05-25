package com.example.demo.controller;

import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacturaController {

    private static final Logger LOGGER = Logger.getLogger(FacturaController.class.getName());

    private static final String SQL_FACTURAS =
        "SELECT f.id_factura, f.id_orden, f.cliente, f.fecha, " +
        "f.subtotal, f.costo_delivery, f.itbis, f.total, f.estado " +
        "FROM facturas f ORDER BY f.id_factura DESC";

    private static final String SQL_BUSCAR_ORDEN =
        "SELECT op.id, op.numero_orden, op.cliente, op.telefono, " +
        "op.direccion, op.precio_venta, op.anticipo, op.libras, " +
        "op.categoria, op.decoracion, op.adornos, op.rellenos, " +
        "op.mensaje, op.costo_delivery, op.fecha_entrega " +
        "FROM ordenes_produccion op WHERE op.id = ? OR op.cliente LIKE ?";

    private static final String SQL_INSERT_FACTURA =
        "INSERT INTO facturas (id_orden, cliente, telefono, direccion, " +
        "fecha, subtotal, costo_delivery, itbis, descuento, total, estado, " +
        "detalles, usuario_genera, fecha_generacion) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDA', ?, ?, GETDATE())";

    private static final String SQL_CREAR_TABLA =
        "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'facturas') " +
        "CREATE TABLE facturas (" +
        "id_factura INT IDENTITY(1,1) PRIMARY KEY, " +
        "id_orden INT, cliente NVARCHAR(200), telefono NVARCHAR(20), " +
        "direccion NVARCHAR(500), fecha DATE, subtotal DECIMAL(12,2), " +
        "costo_delivery DECIMAL(12,2) DEFAULT 0, " +
        "itbis DECIMAL(12,2) DEFAULT 0, " +
        "descuento DECIMAL(12,2) DEFAULT 0, " +
        "total DECIMAL(12,2), estado NVARCHAR(20) DEFAULT 'EMITIDA', " +
        "detalles NVARCHAR(MAX), usuario_genera NVARCHAR(100), " +
        "fecha_generacion DATETIME DEFAULT GETDATE())";

    @FXML private Button generarBtn, imprimirBtn, buscarBtn;
    @FXML private TextField buscarField;
    @FXML private Label ordenInfoLabel;

    @FXML private TableView<Factura> facturasTable;
    @FXML private TableColumn<Factura, Integer> factIdColumn;
    @FXML private TableColumn<Factura, String> factClienteColumn;
    @FXML private TableColumn<Factura, String> factFechaColumn;
    @FXML private TableColumn<Factura, Double> factSubtotalColumn;
    @FXML private TableColumn<Factura, Double> factDeliveryColumn;
    @FXML private TableColumn<Factura, Double> factImpuestoColumn;
    @FXML private TableColumn<Factura, Double> factTotalColumn;
    @FXML private TableColumn<Factura, String> factEstadoColumn;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Integer ordenSeleccionadaId;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        factIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        factClienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        factFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        factSubtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        factDeliveryColumn.setCellValueFactory(new PropertyValueFactory<>("costoDelivery"));
        factImpuestoColumn.setCellValueFactory(new PropertyValueFactory<>("itbis"));
        factTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        factEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));

        asegurarTabla();
        cargarFacturas();

        buscarBtn.setOnAction(e -> buscarOrden());
        generarBtn.setOnAction(e -> generarFactura());
        imprimirBtn.setOnAction(e -> imprimir());
    }

    private void asegurarTabla() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CREAR_TABLA)) {
            stmt.execute();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error creando tabla facturas: {0}", e.getMessage());
        }
    }

    private void cargarFacturas() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FACTURAS);
             ResultSet rs = stmt.executeQuery()) {
            ObservableList<Factura> list = FXCollections.observableArrayList();
            while (rs.next()) {
                list.add(new Factura(
                    rs.getInt("id_factura"),
                    rs.getString("cliente"),
                    rs.getString("fecha") != null ? rs.getString("fecha") : "",
                    rs.getDouble("subtotal"),
                    rs.getDouble("costo_delivery"),
                    rs.getDouble("itbis"),
                    rs.getDouble("total"),
                    rs.getString("estado")));
            }
            facturasTable.setItems(list);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error cargando facturas: {0}", e.getMessage());
            facturasTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void buscarOrden() {
        String texto = buscarField.getText().trim();
        if (texto.isEmpty()) { ordenInfoLabel.setText("Ingrese un n\u00famero de orden o cliente."); return; }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ORDEN)) {
            stmt.setString(1, texto);
            stmt.setString(2, "%" + texto + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ordenSeleccionadaId = rs.getInt("id");
                    String info = String.format("Orden #%s | %s | $%.2f | %s",
                        rs.getString("numero_orden"),
                        rs.getString("cliente"),
                        rs.getDouble("precio_venta"),
                        rs.getDate("fecha_entrega") != null ? rs.getDate("fecha_entrega").toString() : "");
                    ordenInfoLabel.setText(info);
                    ordenInfoLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                } else {
                    ordenInfoLabel.setText("No se encontr\u00f3 ninguna orden con ese criterio.");
                    ordenInfoLabel.setStyle("-fx-text-fill: #c62828;");
                    ordenSeleccionadaId = null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error buscando orden: {0}", e.getMessage());
            ordenInfoLabel.setText("Error al buscar orden.");
        }
    }

    private void generarFactura() {
        if (ordenSeleccionadaId == null) {
            mostrarAlerta("Seleccione una orden primero.");
            return;
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ORDEN)) {
            stmt.setInt(1, ordenSeleccionadaId);
            stmt.setString(2, "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String cliente = rs.getString("cliente");
                    String telefono = rs.getString("telefono");
                    String direccion = rs.getString("direccion");
                    double subtotal = rs.getDouble("precio_venta");
                    double delivery = rs.getDouble("costo_delivery");
                    double itbis = (subtotal + delivery) * 0.18;
                    double total = subtotal + delivery + itbis;

                    String categoria = rs.getString("categoria");
                    String decoracion = rs.getString("decoracion");
                    String adornos = rs.getString("adornos");
                    String rellenos = rs.getString("rellenos");
                    String mensaje = rs.getString("mensaje");
                    double libras = rs.getDouble("libras");

                    String detalles = String.format(
                        "Categor\u00eda: %s | Libras: %.1f | Decoraci\u00f3n: %s | Adornos: %s | Rellenos: %s | Mensaje: %s",
                        categoria != null ? categoria : "", libras,
                        decoracion != null ? decoracion : "",
                        adornos != null ? adornos : "",
                        rellenos != null ? rellenos : "",
                        mensaje != null ? mensaje : "");

                    try (PreparedStatement insert = conn.prepareStatement(SQL_INSERT_FACTURA)) {
                        insert.setInt(1, ordenSeleccionadaId);
                        insert.setString(2, cliente);
                        insert.setString(3, telefono);
                        insert.setString(4, direccion);
                        insert.setDate(5, Date.valueOf(LocalDate.now()));
                        insert.setDouble(6, subtotal);
                        insert.setDouble(7, delivery);
                        insert.setDouble(8, itbis);
                        insert.setDouble(9, 0);
                        insert.setDouble(10, total);
                        insert.setString(11, detalles);
                        insert.setString(12, sessionManager.getUsuarioActual());
                        insert.executeUpdate();
                    }

                    mostrarAlerta("Factura generada exitosamente para " + cliente + " | Total: $" + String.format("%.2f", total));
                    cargarFacturas();
                    ordenSeleccionadaId = null;
                    ordenInfoLabel.setText("Seleccione una orden para facturar");
                    ordenInfoLabel.setStyle("");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generando factura: {0}", e.getMessage());
            mostrarAlerta("Error al generar factura: " + e.getMessage());
        }
    }

    private void imprimir() {
        Factura seleccionada = facturasTable.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            mostrarAlerta("Imprimiendo factura #" + seleccionada.getId() + " ...\n(Funci\u00f3n de impresi\u00f3n pendiente de implementar con JasperReports)");
        } else {
            mostrarAlerta("Seleccione una factura para imprimir.");
        }
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class Factura {
        private int id;
        private String cliente, fecha;
        private double subtotal, costoDelivery, itbis, total;
        private String estado;

        public Factura(int id, String c, String f, double s, double d, double i, double t, String e) {
            this.id = id; this.cliente = c; this.fecha = f;
            this.subtotal = s; this.costoDelivery = d; this.itbis = i;
            this.total = t; this.estado = e;
        }
        public int getId() { return id; }
        public String getCliente() { return cliente; }
        public String getFecha() { return fecha; }
        public double getSubtotal() { return subtotal; }
        public double getCostoDelivery() { return costoDelivery; }
        public double getItbis() { return itbis; }
        public double getTotal() { return total; }
        public String getEstado() { return estado; }
    }
}
