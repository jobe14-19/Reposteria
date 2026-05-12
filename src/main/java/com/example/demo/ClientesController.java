package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientesController {

    private static final Logger LOGGER = Logger.getLogger(ClientesController.class.getName());

    // Constantes SQL (sin Text Blocks)
    private static final String SQL_CARGAR_CLIENTES =
            "SELECT c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(COUNT(p.id_pedido), 0) as total_pedidos FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente GROUP BY c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, c.fecha_registro ORDER BY c.nombre, c.apellido";

    private static final String SQL_BUSCAR_CLIENTES =
            "SELECT c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(COUNT(p.id_pedido), 0) as total_pedidos FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente WHERE c.nombre LIKE ? OR c.apellido LIKE ? OR c.telefono LIKE ? GROUP BY c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, c.fecha_registro ORDER BY c.nombre, c.apellido";

    private static final String SQL_ELIMINAR_CLIENTE = "DELETE FROM clientes WHERE id_cliente = ?";

    private static final String SQL_REGISTRAR_ACTIVIDAD =
            "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    // Estilos CSS para botones
    private static final String BTN_EDITAR_STYLE = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5 10 5 10; -fx-background-radius: 4;";
    private static final String BTN_ELIMINAR_STYLE = "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5 10 5 10; -fx-background-radius: 4;";
    private static final String BTN_PEDIDOS_STYLE = "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5 10 5 10; -fx-background-radius: 4;";

    // UI Components
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> estadoFilter;
    @FXML private DatePicker fechaDesdePicker;
    @FXML private DatePicker fechaHastaPicker;
    @FXML private TableView<Cliente> clientesTable;
    @FXML private TableColumn<Cliente, Integer> idColumn;
    @FXML private TableColumn<Cliente, String> nombreColumn;
    @FXML private TableColumn<Cliente, String> apellidoColumn;
    @FXML private TableColumn<Cliente, String> telefonoColumn;
    @FXML private TableColumn<Cliente, String> emailColumn;
    @FXML private TableColumn<Cliente, String> fechaRegistroColumn;
    @FXML private TableColumn<Cliente, Integer> pedidosColumn;
    @FXML private TableColumn<Cliente, Void> accionesColumn;
    @FXML private Label totalLabel;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private ObservableList<Cliente> clientesList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        if (!sessionManager.isAdmin()) {
            mostrarError("Acceso Denegado", "Solo los administradores pueden acceder a la gestión de clientes.");
            return;
        }

        initializeFilters();
        configurarTabla();
        cargarClientes();
        setupListeners();
    }

    private void initializeFilters() {
        estadoFilter.getItems().addAll("Todos", "Con pedidos", "Sin pedidos");
        estadoFilter.getSelectionModel().selectFirst();
    }

    private void setupListeners() {
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                cargarClientes();
            } else {
                buscarClientes(newVal);
            }
        });

        estadoFilter.setOnAction(event -> aplicarFiltros());
        fechaDesdePicker.setOnAction(event -> aplicarFiltros());
        fechaHastaPicker.setOnAction(event -> aplicarFiltros());
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        apellidoColumn.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        telefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        fechaRegistroColumn.setCellValueFactory(new PropertyValueFactory<>("fechaRegistro"));
        pedidosColumn.setCellValueFactory(new PropertyValueFactory<>("totalPedidos"));

        accionesColumn.setCellFactory(param -> new TableCell<Cliente, Void>() {
            private final Button editarButton = crearBoton("✏️", BTN_EDITAR_STYLE);
            private final Button eliminarButton = crearBoton("🗑️", BTN_ELIMINAR_STYLE);
            private final Button verPedidosButton = crearBoton("📋", BTN_PEDIDOS_STYLE);
            private final HBox hbox = new HBox(5);

            {
                editarButton.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    editarCliente(cliente);
                });
                eliminarButton.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    eliminarCliente(cliente);
                });
                verPedidosButton.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    verPedidosCliente(cliente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    hbox.getChildren().setAll(editarButton, eliminarButton, verPedidosButton);
                    setGraphic(hbox);
                }
            }
        });
    }

    private Button crearBoton(String texto, String estilo) {
        Button button = new Button(texto);
        button.setStyle(estilo);
        return button;
    }

    private void cargarClientes() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_CLIENTES);
             ResultSet rs = stmt.executeQuery()) {

            clientesList = FXCollections.observableArrayList();

            while (rs.next()) {
                clientesList.add(crearClienteDesdeResultSet(rs));
            }

            clientesTable.setItems(clientesList);
            actualizarTotal(clientesList.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar clientes: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los clientes: " + e.getMessage());
        }
    }

    private void buscarClientes(String textoBusqueda) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_CLIENTES)) {

            String busqueda = "%" + textoBusqueda + "%";
            stmt.setString(1, busqueda);
            stmt.setString(2, busqueda);
            stmt.setString(3, busqueda);

            ObservableList<Cliente> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(crearClienteDesdeResultSet(rs));
                }
            }

            clientesTable.setItems(resultados);
            actualizarTotal(resultados.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar clientes: {0}", e.getMessage());
            mostrarError("Error de Búsqueda", "No se pudo realizar la búsqueda: " + e.getMessage());
        }
    }

    private void aplicarFiltros() {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(COUNT(p.id_pedido), 0) as total_pedidos FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente WHERE 1=1 ");

        if (fechaDesdePicker.getValue() != null) {
            sqlBuilder.append(" AND c.fecha_registro >= ? ");
        }
        if (fechaHastaPicker.getValue() != null) {
            sqlBuilder.append(" AND c.fecha_registro <= ? ");
        }

        String estadoSeleccionado = estadoFilter.getValue();
        if (!"Todos".equals(estadoSeleccionado)) {
            if ("Con pedidos".equals(estadoSeleccionado)) {
                sqlBuilder.append(" AND EXISTS (SELECT 1 FROM pedidos WHERE id_cliente = c.id_cliente) ");
            } else if ("Sin pedidos".equals(estadoSeleccionado)) {
                sqlBuilder.append(" AND NOT EXISTS (SELECT 1 FROM pedidos WHERE id_cliente = c.id_cliente) ");
            }
        }

        sqlBuilder.append(" GROUP BY c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, c.fecha_registro ORDER BY c.nombre, c.apellido");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;

            if (fechaDesdePicker.getValue() != null) {
                stmt.setDate(paramIndex++, java.sql.Date.valueOf(fechaDesdePicker.getValue()));
            }
            if (fechaHastaPicker.getValue() != null) {
                stmt.setDate(paramIndex++, java.sql.Date.valueOf(fechaHastaPicker.getValue()));
            }

            ObservableList<Cliente> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(crearClienteDesdeResultSet(rs));
                }
            }

            clientesTable.setItems(resultados);
            actualizarTotal(resultados.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
            mostrarError("Error de Filtros", "No se pudieron aplicar los filtros: " + e.getMessage());
        }
    }

    private Cliente crearClienteDesdeResultSet(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("fecha_registro"),
                rs.getInt("total_pedidos")
        );
    }

    @FXML
    private void nuevoCliente(ActionEvent event) {
        abrirModalCliente(null);
    }

    @FXML
    private void exportarLista(ActionEvent event) {
        mostrarMensaje("Exportar Lista", "Función de exportación en desarrollo");
    }

    @FXML
    private void actualizarDatos(ActionEvent event) {
        cargarClientes();
        mostrarMensaje("Datos Actualizados", "La lista de clientes ha sido actualizada correctamente.");
    }

    private void editarCliente(Cliente cliente) {
        abrirModalCliente(cliente);
    }

    private void eliminarCliente(Cliente cliente) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Está seguro de eliminar este cliente?");
        alert.setContentText("Cliente: " + cliente.nombreCompleto() + "\nTeléfono: " + cliente.getTelefono() + "\n\nEsta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_CLIENTE)) {

                stmt.setInt(1, cliente.getId());
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas > 0) {
                    registrarActividad("ELIMINAR CLIENTE", "Cliente eliminado: " + cliente.nombreCompleto());
                    cargarClientes();
                    mostrarMensaje("Cliente Eliminado", "El cliente ha sido eliminado correctamente.");
                } else {
                    mostrarError("Error al Eliminar", "No se pudo eliminar el cliente.");
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar cliente: {0}", e.getMessage());
                mostrarError("Error de Base de Datos", "No se pudo eliminar el cliente: " + e.getMessage());
            }
        }
    }

    private void verPedidosCliente(Cliente cliente) {
        mostrarMensaje("Pedidos del Cliente", "Ver pedidos de: " + cliente.nombreCompleto() + "\nFunción en desarrollo.");
    }

    private void abrirModalCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ClienteModal.fxml"));
            Parent root = loader.load();

            ClienteModalController controller = loader.getController();

            // Pasar null porque ClienteModalController espera su propio tipo de Cliente
            controller.setCliente(null);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 500, 600);
            stage.setScene(scene);
            stage.setTitle(cliente == null ? "👥 Nuevo Cliente" : "✏️ Editar Cliente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarClientes();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de cliente: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de cliente: " + e.getMessage());
        }
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

    private void actualizarTotal(int total) {
        totalLabel.setText("Total: " + total + " clientes");
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
     * CLASE TRADICIONAL para Cliente (NO Record)
     */
    public static class Cliente {
        private int id;
        private String nombre;
        private String apellido;
        private String telefono;
        private String email;
        private String fechaRegistro;
        private int totalPedidos;

        public Cliente(int id, String nombre, String apellido, String telefono, String email, String fechaRegistro, int totalPedidos) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
            this.telefono = telefono;
            this.email = email;
            this.fechaRegistro = fechaRegistro;
            this.totalPedidos = totalPedidos;
        }

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getTelefono() { return telefono; }
        public String getEmail() { return email; }
        public String getFechaRegistro() { return fechaRegistro; }
        public int getTotalPedidos() { return totalPedidos; }

        public String nombreCompleto() { return nombre + " " + apellido; }
    }
}