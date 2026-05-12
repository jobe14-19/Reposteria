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

public class PersonalController {

    private static final Logger LOGGER = Logger.getLogger(PersonalController.class.getName());

    // Constantes SQL (sin Text Blocks)
    private static final String SQL_CARGAR_EMPLEADOS =
            "SELECT e.id_empleado, e.nombre, e.cedula, e.telefono, e.area, e.estado, " +
                    "CASE WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) >= 3 THEN 'Completo' " +
                    "WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) > 0 THEN 'Parcial' " +
                    "ELSE 'Pendiente' END as capacitacion " +
                    "FROM empleados e " +
                    "ORDER BY e.nombre";

    private static final String SQL_ELIMINAR_EMPLEADO = "DELETE FROM empleados WHERE id_empleado = ?";

    private static final String SQL_REGISTRAR_ACTIVIDAD =
            "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    // Constantes para estilos
    private static final String BUTTON_EDITAR_STYLE = "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BUTTON_CAPACITACION_STYLE = "-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
    private static final String BUTTON_ELIMINAR_STYLE = "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

    private static final String BADGE_STYLE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
    private static final String BADGE_ACTIVO_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_INACTIVO_STYLE = "-fx-background-color: #DC3545;";
    private static final String BADGE_VACACIONES_STYLE = "-fx-background-color: #FF9800;";
    private static final String BADGE_DEFAULT_STYLE = "-fx-background-color: #666666;";
    private static final String BADGE_COMPLETO_STYLE = "-fx-background-color: #28A745;";
    private static final String BADGE_PARCIAL_STYLE = "-fx-background-color: #FF9800;";
    private static final String BADGE_PENDIENTE_STYLE = "-fx-background-color: #DC3545;";

    private static final String ESTADO_TODOS = "Todos";
    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_INACTIVO = "Inactivo";
    private static final String ESTADO_VACACIONES = "Vacaciones";
    private static final String AREA_TODOS = "Todos";
    private static final String AREA_PRODUCCION = "Producción";
    private static final String AREA_DECORACION = "Decoración";
    private static final String AREA_DELIVERY = "Delivery";
    private static final String AREA_LIMPIEZA = "Limpieza";

    private static final int CAPACITACIONES_COMPLETAS = 3;

    // UI Components
    @FXML private Button contratarButton;
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> areaFilter;
    @FXML private ComboBox<String> estadoFilter;
    @FXML private TableView<Empleado> empleadosTable;
    @FXML private TableColumn<Empleado, Integer> idColumn;
    @FXML private TableColumn<Empleado, String> nombreColumn;
    @FXML private TableColumn<Empleado, String> cedulaColumn;
    @FXML private TableColumn<Empleado, String> telefonoColumn;
    @FXML private TableColumn<Empleado, String> areaColumn;
    @FXML private TableColumn<Empleado, String> estadoColumn;
    @FXML private TableColumn<Empleado, String> capacitacionColumn;
    @FXML private TableColumn<Empleado, Void> accionesColumn;
    @FXML private Label totalLabel;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;
    private ObservableList<Empleado> empleadosList;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        if (!"ADMIN".equals(sessionManager.getPerfilActual())) {
            mostrarError("Acceso Denegado", "Esta función solo está disponible para administradores.");
            return;
        }

        initializeFilters();
        configurarTabla();
        cargarEmpleados();
        actualizarTotal();
        setupListeners();
    }

    private void initializeFilters() {
        areaFilter.getItems().addAll(AREA_TODOS, AREA_PRODUCCION, AREA_DECORACION, AREA_DELIVERY, AREA_LIMPIEZA);
        areaFilter.getSelectionModel().selectFirst();

        estadoFilter.getItems().addAll(ESTADO_TODOS, ESTADO_ACTIVO, ESTADO_INACTIVO, ESTADO_VACACIONES);
        estadoFilter.getSelectionModel().selectFirst();
    }

    private void setupListeners() {
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                cargarEmpleados();
            } else {
                buscarEmpleados(newVal);
            }
        });

        areaFilter.setOnAction(event -> aplicarFiltros());
        estadoFilter.setOnAction(event -> aplicarFiltros());
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        cedulaColumn.setCellValueFactory(new PropertyValueFactory<>("cedula"));
        telefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        areaColumn.setCellValueFactory(new PropertyValueFactory<>("area"));

        estadoColumn.setCellFactory(param -> new TableCell<Empleado, String>() {
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

        capacitacionColumn.setCellFactory(param -> new TableCell<Empleado, String>() {
            private final HBox hbox = new HBox(5);
            private final Label badge = new Label();

            {
                badge.setStyle(BADGE_STYLE_BASE);
                hbox.getChildren().setAll(badge);
            }

            @Override
            protected void updateItem(String capacitacion, boolean empty) {
                super.updateItem(capacitacion, empty);
                if (empty || capacitacion == null) {
                    setGraphic(null);
                } else {
                    String estilo = obtenerEstiloCapacitacion(capacitacion);
                    badge.setStyle(BADGE_STYLE_BASE + estilo);
                    badge.setText(capacitacion.toUpperCase());
                    setGraphic(hbox);
                }
            }
        });

        accionesColumn.setCellFactory(param -> new TableCell<Empleado, Void>() {
            private final Button editarButton = new Button("✏️");
            private final Button capacitacionButton = new Button("📚");
            private final Button eliminarButton = new Button("🗑️");
            private final HBox hbox = new HBox(5);

            {
                editarButton.setStyle(BUTTON_EDITAR_STYLE);
                capacitacionButton.setStyle(BUTTON_CAPACITACION_STYLE);
                eliminarButton.setStyle(BUTTON_ELIMINAR_STYLE);

                editarButton.setOnAction(event -> {
                    Empleado empleado = getTableView().getItems().get(getIndex());
                    editarEmpleado(empleado);
                });
                capacitacionButton.setOnAction(event -> {
                    Empleado empleado = getTableView().getItems().get(getIndex());
                    registrarCapacitacion(empleado);
                });
                eliminarButton.setOnAction(event -> {
                    Empleado empleado = getTableView().getItems().get(getIndex());
                    eliminarEmpleado(empleado);
                });

                hbox.getChildren().setAll(editarButton, capacitacionButton, eliminarButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private String obtenerEstiloEstado(String estado) {
        String estadoLower = estado.toLowerCase();
        if (estadoLower.equals("activo")) return BADGE_ACTIVO_STYLE;
        if (estadoLower.equals("inactivo")) return BADGE_INACTIVO_STYLE;
        if (estadoLower.equals("vacaciones")) return BADGE_VACACIONES_STYLE;
        return BADGE_DEFAULT_STYLE;
    }

    private String obtenerEstiloCapacitacion(String capacitacion) {
        String capLower = capacitacion.toLowerCase();
        if (capLower.equals("completo")) return BADGE_COMPLETO_STYLE;
        if (capLower.equals("parcial")) return BADGE_PARCIAL_STYLE;
        if (capLower.equals("pendiente")) return BADGE_PENDIENTE_STYLE;
        return BADGE_DEFAULT_STYLE;
    }

    private void cargarEmpleados() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_EMPLEADOS);
             ResultSet rs = stmt.executeQuery()) {

            empleadosList = FXCollections.observableArrayList();

            while (rs.next()) {
                Empleado empleado = new Empleado(
                        rs.getInt("id_empleado"),
                        rs.getString("nombre"),
                        rs.getString("cedula"),
                        rs.getString("telefono"),
                        rs.getString("area"),
                        rs.getString("estado"),
                        rs.getString("capacitacion")
                );
                empleadosList.add(empleado);
            }

            empleadosTable.setItems(empleadosList);
            actualizarTotal();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar empleados: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudieron cargar los empleados: " + e.getMessage());
        }
    }

    private void buscarEmpleados(String textoBusqueda) {
        String sql = "SELECT e.id_empleado, e.nombre, e.cedula, e.telefono, e.area, e.estado, " +
                "CASE WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) >= 3 THEN 'Completo' " +
                "WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) > 0 THEN 'Parcial' " +
                "ELSE 'Pendiente' END as capacitacion " +
                "FROM empleados e " +
                "WHERE e.nombre LIKE ? OR e.cedula LIKE ? OR e.telefono LIKE ? " +
                "ORDER BY e.nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String busqueda = "%" + textoBusqueda + "%";
            stmt.setString(1, busqueda);
            stmt.setString(2, busqueda);
            stmt.setString(3, busqueda);

            ObservableList<Empleado> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Empleado empleado = new Empleado(
                            rs.getInt("id_empleado"),
                            rs.getString("nombre"),
                            rs.getString("cedula"),
                            rs.getString("telefono"),
                            rs.getString("area"),
                            rs.getString("estado"),
                            rs.getString("capacitacion")
                    );
                    resultados.add(empleado);
                }
            }

            empleadosTable.setItems(resultados);
            actualizarTotal();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar empleados: {0}", e.getMessage());
            mostrarError("Error de Búsqueda", "No se pudo realizar la búsqueda: " + e.getMessage());
        }
    }

    private void aplicarFiltros() {
        String sql = SQL_CARGAR_EMPLEADOS;
        java.util.ArrayList<String> condiciones = new java.util.ArrayList<>();

        String areaSeleccionada = areaFilter.getValue();
        if (areaSeleccionada != null && !AREA_TODOS.equals(areaSeleccionada)) {
            condiciones.add("e.area = ?");
        }

        String estadoSeleccionado = estadoFilter.getValue();
        if (estadoSeleccionado != null && !ESTADO_TODOS.equals(estadoSeleccionado)) {
            condiciones.add("e.estado = ?");
        }

        if (!condiciones.isEmpty()) {
            String whereClause = "WHERE " + String.join(" AND ", condiciones) + " ";
            int orderByIndex = sql.indexOf("ORDER BY");
            sql = sql.substring(0, orderByIndex) + whereClause + sql.substring(orderByIndex);
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;

            if (areaSeleccionada != null && !AREA_TODOS.equals(areaSeleccionada)) {
                stmt.setString(paramIndex++, areaSeleccionada);
            }
            if (estadoSeleccionado != null && !ESTADO_TODOS.equals(estadoSeleccionado)) {
                stmt.setString(paramIndex++, estadoSeleccionado);
            }

            ObservableList<Empleado> resultados = FXCollections.observableArrayList();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Empleado empleado = new Empleado(
                            rs.getInt("id_empleado"),
                            rs.getString("nombre"),
                            rs.getString("cedula"),
                            rs.getString("telefono"),
                            rs.getString("area"),
                            rs.getString("estado"),
                            rs.getString("capacitacion")
                    );
                    resultados.add(empleado);
                }
            }

            empleadosTable.setItems(resultados);
            actualizarTotal();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
            mostrarError("Error de Filtros", "No se pudieron aplicar los filtros: " + e.getMessage());
        }
    }

    @FXML
    private void contratar(ActionEvent event) {
        abrirModalEmpleado(null);
    }

    private void editarEmpleado(Empleado empleado) {
        abrirModalEmpleado(empleado);
    }

    private void registrarCapacitacion(Empleado empleado) {
        abrirModalCapacitacion(empleado);
    }

    private void eliminarEmpleado(Empleado empleado) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Está seguro de eliminar este empleado?");
        alert.setContentText("Empleado: " + empleado.getNombre() + "\nCédula: " + empleado.getCedula() + "\nÁrea: " + empleado.getArea());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_EMPLEADO)) {

                stmt.setInt(1, empleado.getId());
                int filasAfectadas = stmt.executeUpdate();

                if (filasAfectadas > 0) {
                    registrarActividad("ELIMINAR EMPLEADO", "Empleado eliminado: " + empleado.getNombre());
                    cargarEmpleados();
                    actualizarTotal();
                    mostrarMensaje("Empleado Eliminado", "El empleado ha sido eliminado correctamente.");
                } else {
                    mostrarError("Error al Eliminar", "No se pudo eliminar el empleado.");
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar empleado: {0}", e.getMessage());
                mostrarError("Error de Base de Datos", "No se pudo eliminar el empleado: " + e.getMessage());
            }
        }
    }

    private void abrirModalEmpleado(Empleado empleado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/EmpleadoModal.fxml"));
            Parent root = loader.load();

            EmpleadoModalController controller = loader.getController();
            controller.setEmpleado(empleado);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.setTitle(empleado == null ? "➕ Contratar Empleado" : "✏️ Editar Empleado");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarEmpleados();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de empleado: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de empleado: " + e.getMessage());
        }
    }

    private void abrirModalCapacitacion(Empleado empleado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/CapacitacionModal.fxml"));
            Parent root = loader.load();

            CapacitacionModalController controller = loader.getController();
            controller.setEmpleado(empleado);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 500, 400);
            stage.setScene(scene);
            stage.setTitle("📚 Registrar Capacitación");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarEmpleados();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal de capacitación: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la ventana de capacitación: " + e.getMessage());
        }
    }

    private void actualizarTotal() {
        int total = empleadosTable.getItems() != null ? empleadosTable.getItems().size() : 0;
        totalLabel.setText("Total: " + total + " empleados");
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

    // ============================================================
    // CLASE EMPLEADO - AGREGADA AQUÍ
    // ============================================================
    public static class Empleado {
        private int id;
        private String nombre;
        private String cedula;
        private String telefono;
        private String area;
        private String estado;
        private String capacitacion;

        public Empleado(int id, String nombre, String cedula, String telefono,
                        String area, String estado, String capacitacion) {
            this.id = id;
            this.nombre = nombre;
            this.cedula = cedula;
            this.telefono = telefono;
            this.area = area;
            this.estado = estado;
            this.capacitacion = capacitacion;
        }

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getCedula() { return cedula; }
        public String getTelefono() { return telefono; }
        public String getArea() { return area; }
        public String getEstado() { return estado; }
        public String getCapacitacion() { return capacitacion; }

        public boolean esActivo() { return ESTADO_ACTIVO.equalsIgnoreCase(estado); }
        public boolean estaVacaciones() { return ESTADO_VACACIONES.equalsIgnoreCase(estado); }
        public boolean estaInactivo() { return ESTADO_INACTIVO.equalsIgnoreCase(estado); }
        public boolean tieneCapacitacionCompleta() { return "Completo".equalsIgnoreCase(capacitacion); }
        public boolean tieneCapacitacionParcial() { return "Parcial".equalsIgnoreCase(capacitacion); }
        public boolean tieneCapacitacionPendiente() { return "Pendiente".equalsIgnoreCase(capacitacion); }

        public int getCapacitacionesCompletadas() {
            String cap = capacitacion.toLowerCase();
            if (cap.equals("completo")) return CAPACITACIONES_COMPLETAS;
            if (cap.equals("parcial")) return 1;
            return 0;
        }
    }
}