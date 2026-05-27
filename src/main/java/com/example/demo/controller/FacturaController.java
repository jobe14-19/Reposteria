package com.example.demo.controller;

import com.example.demo.service.ReportService;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacturaController {

    private static final Logger LOGGER = Logger.getLogger(FacturaController.class.getName());

    private static final String SQL_FACTURAS =
        "SELECT f.id_factura, f.id_orden, f.cliente, f.telefono, f.direccion, f.fecha, " +
        "f.subtotal, f.costo_delivery, f.itbis, f.total, f.estado, " +
        "ISNULL(f.metodo_pago, 'Efectivo') as metodo_pago, ISNULL(f.pagado, 'NO') as pagado " +
        "FROM facturas f ORDER BY f.id_factura DESC";

    private static final String SQL_BUSCAR_ORDEN =
        "SELECT op.id_orden as id, op.numero_orden, op.cliente, op.telefono, " +
        "op.direccion, op.precio_venta, op.anticipo, op.libras, " +
        "op.categoria, op.decoracion, op.adornos, op.rellenos, " +
        "op.mensaje, op.costo_delivery, op.fecha_entrega " +
        "FROM ordenes_produccion op WHERE op.cliente LIKE ? OR op.numero_orden LIKE ?";

    private static final String SQL_BUSCAR_ORDEN_POR_ID =
        "SELECT op.id_orden as id, op.numero_orden, op.cliente, op.telefono, " +
        "op.direccion, op.precio_venta, op.anticipo, op.libras, " +
        "op.categoria, op.decoracion, op.adornos, op.rellenos, " +
        "op.mensaje, op.costo_delivery, op.fecha_entrega " +
        "FROM ordenes_produccion op WHERE op.id_orden = ?";

    private static final String SQL_INSERT_FACTURA =
        "INSERT INTO facturas (id_orden, cliente, telefono, direccion, " +
        "fecha, subtotal, costo_delivery, itbis, descuento, total, estado, " +
        "detalles, usuario_genera, fecha_generacion, metodo_pago, pagado) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDA', ?, ?, GETDATE(), ?, 'NO')";

    private static final String SQL_UPDATE_ESTADO =
        "UPDATE facturas SET estado = ? WHERE id_factura = ?";

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

    private static final String SQL_AGREGAR_METODO_PAGO =
        "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('facturas') AND name='metodo_pago') " +
        "ALTER TABLE facturas ADD metodo_pago NVARCHAR(30) DEFAULT 'Efectivo'";

    private static final String SQL_AGREGAR_PAGADO =
        "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('facturas') AND name='pagado') " +
        "ALTER TABLE facturas ADD pagado NVARCHAR(2) DEFAULT 'NO'";

    private static final String SQL_UPDATE_PAGADO_CLIENTE =
        "UPDATE facturas SET estado = 'PAGADA', pagado = 'SI' WHERE cliente = ? AND estado != 'PAGADA' AND estado != 'ANULADA' AND estado != 'CANCELADA'";

    @FXML private Button generarBtn, imprimirBtn, buscarBtn, estadoBtn;
    @FXML private TextField buscarField;
    @FXML private Label ordenInfoLabel;
    @FXML private VBox detalleOrdenPanel;
    @FXML private Label detClienteLabel, detTelefonoLabel, detDireccionLabel, detTotalLabel;
    @FXML private ComboBox<String> metodoPagoCombo;

    @FXML private TableView<Factura> facturasTable;
    @FXML private TableColumn<Factura, Integer> factIdColumn;
    @FXML private TableColumn<Factura, String> factClienteColumn;
    @FXML private TableColumn<Factura, String> factTelefonoColumn;
    @FXML private TableColumn<Factura, String> factDireccionColumn;
    @FXML private TableColumn<Factura, String> factFechaColumn;
    @FXML private TableColumn<Factura, Double> factSubtotalColumn;
    @FXML private TableColumn<Factura, Double> factDeliveryColumn;
    @FXML private TableColumn<Factura, Double> factImpuestoColumn;
    @FXML private TableColumn<Factura, Double> factTotalColumn;
    @FXML private TableColumn<Factura, String> factEstadoColumn;
    @FXML private TableColumn<Factura, String> factMetodoColumn;
    @FXML private TableColumn<Factura, String> factPagadoColumn;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private Integer ordenSeleccionadaId;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        factIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        factClienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        factTelefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        factDireccionColumn.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        factFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        factSubtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        factDeliveryColumn.setCellValueFactory(new PropertyValueFactory<>("costoDelivery"));
        factImpuestoColumn.setCellValueFactory(new PropertyValueFactory<>("itbis"));
        factTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        factEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        factMetodoColumn.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        factPagadoColumn.setCellValueFactory(new PropertyValueFactory<>("pagado"));
        factPagadoColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            private final HBox hbox = new HBox(5);
            { badge.setStyle("-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;"); hbox.getChildren().add(badge); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String color = "SI".equals(item) ? "#28A745" : "#FF9800";
                badge.setStyle("-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + color + ";");
                badge.setText("SI".equals(item) ? "PAGADO" : "PENDIENTE");
                setGraphic(hbox);
            }
        });
        factEstadoColumn.setCellFactory(col -> new TableCell<>() {
 private final Label badge = new Label();
 private final HBox hbox = new HBox(5);
 { badge.setStyle("-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;"); hbox.getChildren().add(badge); }
 @Override protected void updateItem(String item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) { setGraphic(null); return; }
 String color = "EMITIDA".equals(item) ? "#28A745" : "PAGADA".equals(item) ? "#007BFF" : "#FF9800";
 badge.setStyle("-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + color + ";");
 badge.setText(item);
 setGraphic(hbox);
 }
 });

        metodoPagoCombo.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Cheque", "Transferencia"));
        metodoPagoCombo.setValue("Efectivo");
        detalleOrdenPanel.setVisible(false);
        detalleOrdenPanel.setManaged(false);

        asegurarTabla();
        cargarFacturas();

        buscarBtn.setOnAction(e -> buscarOrden());
        generarBtn.setOnAction(e -> generarFactura());
        estadoBtn.setOnAction(e -> cambiarEstado());
        imprimirBtn.setOnAction(e -> imprimir());

        facturasTable.setRowFactory(tv -> {
            TableRow<Factura> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    Factura f = row.getItem();
                    ContextMenu cm = new ContextMenu();
                    MenuItem cambiar = new MenuItem("Cambiar Estado");
                    cambiar.setOnAction(ev -> {
                        facturasTable.getSelectionModel().select(f);
                        cambiarEstado();
                    });
                    MenuItem imprimirItem = new MenuItem("Imprimir");
                    imprimirItem.setOnAction(ev -> {
                        facturasTable.getSelectionModel().select(f);
                        imprimir();
                    });
                    cm.getItems().addAll(cambiar, imprimirItem);
                    cm.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            return row;
        });
    }

    private void asegurarTabla() {
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(SQL_CREAR_TABLA);
            stmt.execute(SQL_AGREGAR_METODO_PAGO);
            stmt.execute(SQL_AGREGAR_PAGADO);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error creando/actualizando tabla facturas: {0}", e.getMessage());
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
                    rs.getString("telefono") != null ? rs.getString("telefono") : "",
                    rs.getString("direccion") != null ? rs.getString("direccion") : "",
                    rs.getString("fecha") != null ? rs.getString("fecha") : "",
                    rs.getDouble("subtotal"),
                    rs.getDouble("costo_delivery"),
                    rs.getDouble("itbis"),
                    rs.getDouble("total"),
                    rs.getString("estado"),
                    rs.getString("metodo_pago"),
                    rs.getString("pagado")));
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
            String like = "%" + texto + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ordenSeleccionadaId = rs.getInt("id");
                    String cliente = rs.getString("cliente");
                    String telefono = rs.getString("telefono");
                    String direccion = rs.getString("direccion") != null ? rs.getString("direccion") : "Sin dirección";
                    double precio = rs.getDouble("precio_venta");
                    String fechaEntrega = rs.getDate("fecha_entrega") != null ? rs.getDate("fecha_entrega").toString() : "";

                    String info = String.format("Orden #%s | %s | $%.2f | %s",
                        rs.getString("numero_orden"), cliente, precio, fechaEntrega);
                    ordenInfoLabel.setText(info);
                    ordenInfoLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");

                    detClienteLabel.setText(cliente);
                    detTelefonoLabel.setText(telefono != null ? telefono : "");
                    detDireccionLabel.setText(direccion);
                    detTotalLabel.setText("RD$" + String.format("%.2f", precio));
                    detalleOrdenPanel.setVisible(true);
                    detalleOrdenPanel.setManaged(true);
                } else {
                    ordenInfoLabel.setText("No se encontr\u00f3 ninguna orden con ese criterio.");
                    ordenInfoLabel.setStyle("-fx-text-fill: #c62828;");
                    ordenSeleccionadaId = null;
                    detalleOrdenPanel.setVisible(false);
                    detalleOrdenPanel.setManaged(false);
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
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ORDEN_POR_ID)) {
            stmt.setInt(1, ordenSeleccionadaId);
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

                    String metodoPago = metodoPagoCombo.getValue();
                    if (metodoPago == null || metodoPago.isEmpty()) {
                        metodoPago = "Efectivo";
                    }

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
                        insert.setString(13, metodoPago);
                        insert.executeUpdate();
                    }

                    mostrarAlerta("Factura generada exitosamente para " + cliente + " | Total: $" + String.format("%.2f", total));
                    cargarFacturas();
                    ordenSeleccionadaId = null;
                    ordenInfoLabel.setText("Seleccione una orden para facturar");
                    ordenInfoLabel.setStyle("");
                    detalleOrdenPanel.setVisible(false);
                    detalleOrdenPanel.setManaged(false);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generando factura: {0}", e.getMessage());
            mostrarAlerta("Error al generar factura: " + e.getMessage());
        }
    }

    private void cambiarEstado() {
        Factura seleccionada = facturasTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Seleccione una factura para cambiar su estado.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(seleccionada.getEstado(), "EMITIDA", "PAGADA", "ANULADA", "CANCELADA");
        dialog.setTitle("Cambiar Estado");
        dialog.setHeaderText("Factura #" + seleccionada.getId() + " - " + seleccionada.getCliente());
        dialog.setContentText("Nuevo estado:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().equals(seleccionada.getEstado())) {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_ESTADO)) {
                stmt.setString(1, result.get());
                stmt.setInt(2, seleccionada.getId());
                stmt.executeUpdate();

                if ("PAGADA".equals(result.get())) {
                    try (PreparedStatement updCliente = conn.prepareStatement(SQL_UPDATE_PAGADO_CLIENTE)) {
                        updCliente.setString(1, seleccionada.getCliente());
                        int afectadas = updCliente.executeUpdate();
                        if (afectadas > 1) {
                            mostrarAlerta("Factura #" + seleccionada.getId() + " marcada como PAGADA. Se actualizaron " + (afectadas - 1) + " factura(s) adicional(es) del mismo cliente.");
                        } else {
                            mostrarAlerta("Estado actualizado: " + seleccionada.getEstado() + " \u2192 " + result.get());
                        }
                    }
                } else {
                    mostrarAlerta("Estado actualizado: " + seleccionada.getEstado() + " \u2192 " + result.get());
                }
                cargarFacturas();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error actualizando estado: {0}", e.getMessage());
                mostrarAlerta("Error al actualizar estado: " + e.getMessage());
            }
        }
    }

    private void imprimir() {
        Factura seleccionada = facturasTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Seleccione una factura para imprimir.");
            return;
        }
        try {
            ReportService rs = new ReportService();
            Map<String, Object> params = new HashMap<>();
            params.put("ID_FACTURA", seleccionada.getId());
            rs.generateAndShow("/reportes/Reporte_Factura_Simple.jrxml", params);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al imprimir factura: {0}", e.getMessage());
            mostrarAlerta("Error al imprimir: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class Factura {
        private int id;
        private String cliente, telefono, direccion, fecha;
        private double subtotal, costoDelivery, itbis, total;
        private String estado, metodoPago, pagado;

        public Factura(int id, String c, String telf, String dir, String f, double s, double d, double i, double tot, String e, String m, String p) {
            this.id = id; this.cliente = c; this.telefono = telf; this.direccion = dir; this.fecha = f;
            this.subtotal = s; this.costoDelivery = d; this.itbis = i;
            this.total = tot; this.estado = e; this.metodoPago = m; this.pagado = p;
        }
        public int getId() { return id; }
        public String getCliente() { return cliente; }
        public String getTelefono() { return telefono; }
        public String getDireccion() { return direccion; }
        public String getFecha() { return fecha; }
        public double getSubtotal() { return subtotal; }
        public double getCostoDelivery() { return costoDelivery; }
        public double getItbis() { return itbis; }
        public double getTotal() { return total; }
        public String getEstado() { return estado; }
        public String getMetodoPago() { return metodoPago; }
        public String getPagado() { return pagado; }
    }
}
