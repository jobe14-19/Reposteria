package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompraModalController {

    private static final Logger LOGGER = Logger.getLogger(CompraModalController.class.getName());

    // Constantes SQL (sin Text Blocks)
    private static final String SQL_CARGAR_PROVEEDORES = "SELECT id_proveedor, nombre FROM proveedores ORDER BY nombre";
    private static final String SQL_CARGAR_PRODUCTOS = "SELECT id_producto, nombre FROM productos ORDER BY nombre";
    private static final String SQL_OBTENER_PRECIO_PRODUCTO = "SELECT precio_unitario FROM productos WHERE nombre = ?";
    private static final String SQL_OBTENER_ID_PROVEEDOR = "SELECT id_proveedor FROM proveedores WHERE nombre = ?";
    private static final String SQL_OBTENER_ID_PRODUCTO = "SELECT id_producto FROM productos WHERE nombre = ?";
    private static final String SQL_INSERT_COMPRA =
            "INSERT INTO compras (id_proveedor, fecha_compra, usuario_registra, total) VALUES (?, GETDATE(), ?, ?)";
    private static final String SQL_INSERT_COMPRA_DETALLE =
            "INSERT INTO compra_detalles (id_compra, id_producto, cantidad, precio_unitario, descuento, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR_STOCK = "UPDATE ingredientes SET stock_actual = stock_actual + ? WHERE nombre = ?";
    private static final String SQL_REGISTRAR_ACTIVIDAD =
            "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    // Estilos CSS
    private static final String BTN_ELIMINAR_STYLE =
            "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

    // UI Components
    @FXML private Label tituloLabel;
    @FXML private ComboBox<String> proveedorComboBox;
    @FXML private DatePicker fechaCompraPicker;
    @FXML private TableView<ProductoCompra> productosTable;
    @FXML private TableColumn<ProductoCompra, String> productoColumn;
    @FXML private TableColumn<ProductoCompra, Double> cantidadColumn;
    @FXML private TableColumn<ProductoCompra, Double> precioUnitarioColumn;
    @FXML private TableColumn<ProductoCompra, Double> descuentoColumn;
    @FXML private TableColumn<ProductoCompra, Double> subtotalColumn;
    @FXML private TableColumn<ProductoCompra, Void> accionesColumn;
    @FXML private ComboBox<String> productoComboBox;
    @FXML private TextField cantidadField;
    @FXML private TextField precioUnitarioField;
    @FXML private TextField descuentoField;
    @FXML private CheckBox productoDelicadoCheckBox;
    @FXML private CheckBox requiereRefrigeracionCheckBox;
    @FXML private TextField totalField;
    @FXML private Button cancelarButton;
    @FXML private Button guardarResultado;

    // Services and Managers
    private DatabaseConnection dbConnection;
    private SessionManager sessionManager;
    private ObservableList<ProductoCompra> productosList;
    private String nuevoProductoSeleccionado;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        productosList = FXCollections.observableArrayList();
        productosTable.setItems(productosList);

        initializeCombos();
        configurarTabla();
        setupEventHandlers();

        fechaCompraPicker.setValue(java.time.LocalDate.now());
    }

    private void initializeCombos() {
        cargarProveedores();
        cargarProductos();
        proveedorComboBox.getSelectionModel().selectFirst();
    }

    private void cargarProveedores() {
        proveedorComboBox.getItems().clear();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_PROVEEDORES);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                proveedorComboBox.getItems().add(rs.getString("nombre"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar proveedores: {0}", e.getMessage());
        }
    }

    private void cargarProductos() {
        productoComboBox.getItems().clear();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_PRODUCTOS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                productoComboBox.getItems().add(rs.getString("nombre"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar productos: {0}", e.getMessage());
        }
    }

    private void configurarTabla() {
        productoColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        cantidadColumn.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        precioUnitarioColumn.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        descuentoColumn.setCellValueFactory(new PropertyValueFactory<>("descuento"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        accionesColumn.setCellFactory(param -> new TableCell<ProductoCompra, Void>() {
            private final Button eliminarButton = crearBotonEliminar();
            private final HBox hbox = new HBox(5);

            {
                eliminarButton.setOnAction(event -> {
                    ProductoCompra producto = getTableView().getItems().get(getIndex());
                    eliminarProducto(producto);
                });
                hbox.getChildren().setAll(eliminarButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private Button crearBotonEliminar() {
        Button button = new Button("🗑️");
        button.setStyle(BTN_ELIMINAR_STYLE);
        return button;
    }

    private void setupEventHandlers() {
        productoComboBox.setOnAction(event -> seleccionarProducto());

        cantidadField.textProperty().addListener((obs, oldVal, newVal) -> actualizarSubtotal());
        precioUnitarioField.textProperty().addListener((obs, oldVal, newVal) -> actualizarSubtotal());
        descuentoField.textProperty().addListener((obs, oldVal, newVal) -> actualizarSubtotal());
    }

    private void seleccionarProducto() {
        String productoSeleccionado = productoComboBox.getSelectionModel().getSelectedItem();

        if (productoSeleccionado != null) {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PRECIO_PRODUCTO)) {

                stmt.setString(1, productoSeleccionado);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double precioUnitario = rs.getDouble("precio_unitario");
                        precioUnitarioField.setText(String.format("%.2f", precioUnitario));
                        cantidadField.setText("1");
                        descuentoField.setText("0");
                        actualizarSubtotal();
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al obtener precio del producto: {0}", e.getMessage());
            }
        }
    }

    private void actualizarSubtotal() {
        try {
            double cantidad = Double.parseDouble(obtenerTextoODefault(cantidadField, "0"));
            double precioUnitario = Double.parseDouble(obtenerTextoODefault(precioUnitarioField, "0"));
            double descuento = Double.parseDouble(obtenerTextoODefault(descuentoField, "0"));

            double subtotal = (cantidad * precioUnitario) - descuento;
            totalField.setText(String.format("%.2f", subtotal));

            if (nuevoProductoSeleccionado != null) {
                actualizarOAgregarProducto(cantidad, precioUnitario, descuento, subtotal);
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error en formato de números: {0}", e.getMessage());
        }
    }

    private String obtenerTextoODefault(TextField field, String defaultValue) {
        String text = field.getText();
        return (text == null || text.trim().isEmpty()) ? defaultValue : text;
    }

    private void actualizarOAgregarProducto(double cantidad, double precioUnitario, double descuento, double subtotal) {
        Optional<ProductoCompra> productoExistente = productosList.stream()
                .filter(p -> p.getNombre().equals(nuevoProductoSeleccionado))
                .findFirst();

        if (productoExistente.isPresent()) {
            ProductoCompra producto = productoExistente.get();
            ProductoCompra productoActualizado = new ProductoCompra(
                    producto.getNombre(),
                    cantidad,
                    precioUnitario,
                    descuento,
                    subtotal
            );
            int index = productosList.indexOf(producto);
            productosList.set(index, productoActualizado);
        } else {
            ProductoCompra nuevoProducto = new ProductoCompra(
                    nuevoProductoSeleccionado,
                    cantidad,
                    precioUnitario,
                    descuento,
                    subtotal
            );
            productosList.add(nuevoProducto);
        }

        productosTable.refresh();
        calcularTotalCompra();
    }

    @FXML
    private void agregarProducto(ActionEvent event) {
        String producto = productoComboBox.getSelectionModel().getSelectedItem();

        if (producto == null) {
            mostrarError("Producto Requerido", "Por favor seleccione un producto.");
            return;
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PRECIO_PRODUCTO)) {

            stmt.setString(1, producto);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double precioUnitario = rs.getDouble("precio_unitario");

                    ProductoCompra nuevoProducto = new ProductoCompra(
                            producto,
                            1.0,
                            precioUnitario,
                            0.0,
                            precioUnitario
                    );

                    productosList.add(nuevoProducto);

                    productoComboBox.getSelectionModel().select(producto);
                    nuevoProductoSeleccionado = producto;

                    cantidadField.setText("1");
                    precioUnitarioField.setText(String.format("%.2f", precioUnitario));
                    descuentoField.setText("0");
                    actualizarSubtotal();
                    calcularTotalCompra();
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al agregar producto: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo agregar el producto: " + e.getMessage());
        }
    }

    private void eliminarProducto(ProductoCompra producto) {
        if (producto != null) {
            productosList.remove(producto);
            productosTable.refresh();
            calcularTotalCompra();
        }
    }

    @FXML
    private void confirmarAgregarProducto(ActionEvent event) {
        agregarProducto(event);
    }

    @FXML
    private void cancelarResultado(ActionEvent event) {
        cerrarModal();
    }

    @FXML
    private void limpiarCampos(ActionEvent event) {
        proveedorComboBox.getSelectionModel().selectFirst();
        fechaCompraPicker.setValue(java.time.LocalDate.now());
        productoComboBox.getSelectionModel().clearSelection();
        cantidadField.clear();
        precioUnitarioField.clear();
        descuentoField.clear();
        totalField.clear();
        productoDelicadoCheckBox.setSelected(false);
        requiereRefrigeracionCheckBox.setSelected(false);
        productosList.clear();
        productosTable.refresh();
    }

    @FXML
    private void guardarResultado(ActionEvent event) {
        if (productosList.isEmpty()) {
            mostrarError("Sin Productos", "No hay productos para registrar la compra.");
            return;
        }

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            int idCompra = insertarCompra(conn);

            if (idCompra > 0) {
                insertarDetallesCompra(conn, idCompra);
                actualizarStocks();
                registrarActividad("REGISTRAR COMPRA", "Compra registrada: " + productosList.size() + " productos");

                conn.commit();

                mostrarMensaje("Compra Registrada", "La compra ha sido registrada correctamente.");
                cerrarModal();
            }

        } catch (SQLException e) {
            realizarRollback(conn);
            LOGGER.log(Level.SEVERE, "Error al registrar compra: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo registrar la compra: " + e.getMessage());
        }
    }

    private int insertarCompra(Connection conn) throws SQLException {
        String proveedor = proveedorComboBox.getSelectionModel().getSelectedItem();
        int idProveedor = obtenerIdProveedor(proveedor);
        double total = calcularTotalCompra();

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_COMPRA, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idProveedor);
            stmt.setString(2, sessionManager.getUsuarioActual());
            stmt.setDouble(3, total);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                return generatedKeys.next() ? generatedKeys.getInt(1) : 0;
            }
        }
    }

    private void insertarDetallesCompra(Connection conn, int idCompra) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_COMPRA_DETALLE)) {
            for (ProductoCompra producto : productosList) {
                stmt.setInt(1, idCompra);
                stmt.setInt(2, obtenerIdProducto(producto.getNombre()));
                stmt.setDouble(3, producto.getCantidad());
                stmt.setDouble(4, producto.getPrecioUnitario());
                stmt.setDouble(5, producto.getDescuento());
                stmt.setDouble(6, producto.getSubtotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void actualizarStocks() {
        for (ProductoCompra producto : productosList) {
            actualizarStockProducto(producto.getNombre(), producto.getCantidad());
        }
    }

    private int obtenerIdProveedor(String nombreProveedor) {
        if (nombreProveedor == null || nombreProveedor.trim().isEmpty()) {
            return 0;
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_PROVEEDOR)) {

            stmt.setString(1, nombreProveedor);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id_proveedor") : 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener ID de proveedor: {0}", e.getMessage());
            return 0;
        }
    }

    private int obtenerIdProducto(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
            return 0;
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_PRODUCTO)) {

            stmt.setString(1, nombreProducto);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id_producto") : 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener ID de producto: {0}", e.getMessage());
            return 0;
        }
    }

    private void actualizarStockProducto(String nombreProducto, double cantidad) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_STOCK)) {

            stmt.setDouble(1, cantidad);
            stmt.setString(2, nombreProducto);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al actualizar stock: {0}", e.getMessage());
        }
    }

    private double calcularTotalCompra() {
        double total = 0.0;
        for (ProductoCompra producto : productosList) {
            total += producto.getSubtotal();
        }
        return total;
    }

    private void realizarRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error en rollback: {0}", e.getMessage());
            }
        }
    }

    private void cerrarModal() {
        Stage stage = (Stage) guardarResultado.getScene().getWindow();
        stage.close();
    }

    private void registrarActividad(String accion, String detalle) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {

            stmt.setString(1, sessionManager.getUsuarioActual());
            stmt.setString(2, accion);
            stmt.setString(3, detalle);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.ERROR, titulo, mensaje);
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * CLASE TRADICIONAL para ProductoCompra (NO Record)
     */
    public static class ProductoCompra {
        private String nombre;
        private double cantidad;
        private double precioUnitario;
        private double descuento;
        private double subtotal;

        public ProductoCompra(String nombre, double cantidad, double precioUnitario, double descuento, double subtotal) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.descuento = descuento;
            this.subtotal = subtotal;
        }

        public String getNombre() { return nombre; }
        public double getCantidad() { return cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public double getDescuento() { return descuento; }
        public double getSubtotal() { return subtotal; }

        public void setNombre(String nombre) { this.nombre = nombre; }
        public void setCantidad(double cantidad) { this.cantidad = cantidad; }
        public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
        public void setDescuento(double descuento) { this.descuento = descuento; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    }
}