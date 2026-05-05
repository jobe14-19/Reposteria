package com.example.demo;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventarioController {

    private static final Logger LOGGER = Logger.getLogger(InventarioController.class.getName());

    // Constantes SQL - CORREGIDO para SQLite
    private static final String SQL_CARGAR_INVENTARIO = """
        SELECT i.id_ingrediente, i.nombre, i.categoria, i.unidad,
               i.stock_actual, i.stock_minimo,
               CASE WHEN i.stock_actual < i.stock_minimo THEN 'Crítico'
                    WHEN i.stock_actual <= i.stock_minimo * 1.2 THEN 'Bajo'
                    ELSE 'Normal' END as estado
        FROM ingredientes i
        ORDER BY i.nombre
        """;

    private static final String SQL_CARGAR_ALERTAS = """
        SELECT i.nombre || ' - Stock: ' || CAST(i.stock_actual AS TEXT) || ' / ' ||
               CAST(i.stock_minimo AS TEXT) || ' (' || i.unidad || ')' as alerta
        FROM ingredientes i
        WHERE i.stock_actual <= i.stock_minimo * 1.2
        ORDER BY i.stock_actual
        LIMIT 10
        """;

    private static final String SQL_ELIMINAR_INGREDIENTE = "DELETE FROM ingredientes WHERE id_ingrediente = ?";

    private static final String SQL_REGISTRAR_ACTIVIDAD = """
        INSERT INTO actividad (fecha_hora, usuario, accion, detalle)
        VALUES (CURRENT_TIMESTAMP, ?, ?, ?)
        """;

    // Constantes para estilos
    private static final String BUTTON_EDITAR_STYLE = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BUTTON_ELIMINAR_STYLE = "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

    private static final String BADGE_STYLE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
    private static final String BADGE_CRITICO_STYLE = "-fx-background-color: #DC3545;";
    private static final String BADGE_BAJO_STYLE = "-fx-background-color: #FF9800;";
    private static final String BADGE_NORMAL_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_DEFAULT_STYLE = "-fx-background-color: #666666;";

    // UI Components
    @FXML private Button registrarCompraButton;
    @FXML private Button verHistorialButton;
    @FXML private Button actualizarStockButton;
    @FXML private Button actualizarAlertasButton;
    @FXML private ListView<String> alertasListView;
    @FXML private TableView<Ingrediente> ingredientesTable;
    @FXML private TableColumn<Ingrediente, Integer> idColumn;
    @FXML private TableColumn<Ingrediente, String> nombreColumn;
    @FXML private TableColumn<Ingrediente, String> categoriaColumn;
    @FXML private TableColumn<Ingrediente, String> unidadColumn;
    @FXML private TableColumn<Ingrediente, Double> stockActualColumn;
    @FXML private TableColumn<Ingrediente, Double> stockMinimoColumn;
    @FXML private TableColumn<Ingrediente, String> estadoColumn;
    @FXML private TableColumn<Ingrediente, Void> accionesColumn;
    @FXML private Label totalLabel;

    // Services and Managers
    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private ObservableList<Ingrediente> ingredientesList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        configurarTabla();
        cargarInventario();
        cargarAlertas();
        actualizarTotal();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        unidadColumn.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        stockActualColumn.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        stockMinimoColumn.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        // Status Column with badge
        estadoColumn.setCellFactory(param -> new TableCell<Ingrediente, String>() {
            private final HBox hbox = new HBox(5);
            private final Label badge = new Label();

            {
                badge.setStyle(BADGE_STYLE_BASE);
                hbox.getChildren().setAll(badge);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                } else {
                    String estilo = obtenerEstiloEstado(estado);
                    badge.setStyle(BADGE_STYLE_BASE + estilo);
                    badge.setText(estado.toUpperCase());
                    setGraphic(hbox);
                }
            }
        });

        // Actions Column with buttons
        accionesColumn.setCellFactory(param -> new TableCell<Ingrediente, Void>() {
            private final Button editarButton = new Button("✏️");
            private final Button eliminarButton = new Button("🗑️");
            private final HBox hbox = new HBox(5);

            {
                editarButton.setStyle(BUTTON_EDITAR_STYLE);
                eliminarButton.setStyle(BUTTON_ELIMINAR_STYLE);
                hbox.getChildren().setAll(editarButton, eliminarButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Ingrediente ingrediente = getTableView().getItems().get(getIndex());
                    editarButton.setOnAction(e -> editarIngrediente(ingrediente));
                    eliminarButton.setOnAction(e -> eliminarIngrediente(ingrediente));
                    setGraphic(hbox);
                }
            }
        });
    }

    private String obtenerEstiloEstado(String estado) {
        if (estado == null) return BADGE_DEFAULT_STYLE;

        switch (estado.toLowerCase()) {
            case "crítico": return BADGE_CRITICO_STYLE;
            case "bajo": return BADGE_BAJO_STYLE;
            case "normal": return BADGE_NORMAL_STYLE;
            default: return BADGE_DEFAULT_STYLE;
        }
    }

    private void cargarInventario() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_INVENTARIO);
             ResultSet rs = stmt.executeQuery()) {

            ingredientesList = FXCollections.observableArrayList();

            while (rs.next()) {
                ingredientesList.add(new Ingrediente(
                        rs.getInt("id_ingrediente"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getString("unidad"),
                        rs.getDouble("stock_actual"),
                        rs.getDouble("stock_minimo"),
                        rs.getString("estado")
                ));
            }

            ingredientesTable.setItems(ingredientesList);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar inventario: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo cargar el inventario: " + e.getMessage());
        }
    }

    private void cargarAlertas() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_ALERTAS);
             ResultSet rs = stmt.executeQuery()) {

            ObservableList<String> alertas = FXCollections.observableArrayList();

            while (rs.next()) {
                alertas.add(rs.getString("alerta"));
            }

            alertasListView.setItems(alertas);

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al cargar alertas: {0}", e.getMessage());
        }
    }

    @FXML
    private void registrarCompra(ActionEvent event) {
        abrirModalCompra();
    }

    @FXML
    private void verHistorial(ActionEvent event) {
        mostrarMensaje("Historial de Compras", "Función de historial de compras en desarrollo.");
    }

    @FXML
    private void actualizarStock(ActionEvent event) {
        mostrarMensaje("Actualizar Stock", "Función de actualización de stock en desarrollo.");
    }

    @FXML
    private void actualizarAlertas(ActionEvent event) {
        cargarAlertas();
        mostrarMensaje("Alertas Actualizadas", "Las alertas de inventario han sido actualizadas correctamente.");
    }

    private void abrirModalCompra() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CompraModal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.setTitle("🛒 Registrar Compra de Ingredientes");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarInventario();
            actualizarTotal();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de compra: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de compra: " + e.getMessage());
        }
    }

    private void editarIngrediente(Ingrediente ingrediente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("IngredienteModal.fxml"));
            Parent root = loader.load();

            IngredienteModalController controller = loader.getController();
            controller.setIngrediente(ingrediente);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 500, 400);
            stage.setScene(scene);
            stage.setTitle("✏️ Editar Ingrediente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarInventario();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de ingrediente: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de edición: " + e.getMessage());
        }
    }

    private void eliminarIngrediente(Ingrediente ingrediente) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Está seguro de eliminar este ingrediente?");
        alert.setContentText("Ingrediente: " + ingrediente.getNombre() +
                "\nCategoría: " + ingrediente.getCategoria() +
                "\nStock Actual: " + ingrediente.getStockActual() + " " + ingrediente.getUnidad() +
                "\n\nEsta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_INGREDIENTE)) {

                stmt.setInt(1, ingrediente.getId());
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas > 0) {
                    registrarActividad("ELIMINAR INGREDIENTE",
                            "Ingrediente eliminado: " + ingrediente.getNombre());
                    cargarInventario();
                    actualizarTotal();
                    mostrarMensaje("Ingrediente Eliminado", "El ingrediente ha sido eliminado correctamente.");
                } else {
                    mostrarError("Error al Eliminar", "No se pudo eliminar el ingrediente.");
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar ingrediente: {0}", e.getMessage());
                mostrarError("Error de Base de Datos", "No se pudo eliminar el ingrediente: " + e.getMessage());
            }
        }
    }

    private void actualizarTotal() {
        totalLabel.setText("Total: " + ingredientesList.size() + " ingredientes");
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
     * CLASE TRADICIONAL para Ingrediente (para compatibilidad con PropertyValueFactory)
     */
    public static class Ingrediente {
        private int id;
        private String nombre;
        private String categoria;
        private String unidad;
        private double stockActual;
        private double stockMinimo;
        private String estado;

        public Ingrediente(int id, String nombre, String categoria, String unidad,
                           double stockActual, double stockMinimo, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.categoria = categoria;
            this.unidad = unidad;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.estado = estado;
        }

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getCategoria() { return categoria; }
        public String getUnidad() { return unidad; }
        public double getStockActual() { return stockActual; }
        public double getStockMinimo() { return stockMinimo; }
        public String getEstado() { return estado; }

        public boolean isCritico() { return "Crítico".equalsIgnoreCase(estado); }
        public boolean isBajo() { return "Bajo".equalsIgnoreCase(estado); }
        public boolean isNormal() { return "Normal".equalsIgnoreCase(estado); }
        public double getDiferenciaStock() { return stockMinimo - stockActual; }
    }
}