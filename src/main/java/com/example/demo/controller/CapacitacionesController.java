package com.example.demo.controller;
import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CapacitacionesController {

    private static final Logger LOGGER = Logger.getLogger(CapacitacionesController.class.getName());

    private static final String SQL_CAPACITACIONES =
        "SELECT c.id_capacitacion, e.nombre as empleado, c.tema, c.fecha, c.duracion, c.capacitador, c.usuario_registra " +
        "FROM capacitaciones c INNER JOIN empleados e ON c.id_empleado = e.id_empleado ORDER BY c.fecha DESC";

    @FXML private TableView<Capacitacion> capacitacionesTable;
    @FXML private TableColumn<Capacitacion, Integer> colId;
    @FXML private TableColumn<Capacitacion, String> colEmpleado;
    @FXML private TableColumn<Capacitacion, String> colTema;
    @FXML private TableColumn<Capacitacion, String> colFecha;
    @FXML private TableColumn<Capacitacion, Integer> colDuracion;
    @FXML private TableColumn<Capacitacion, String> colCapacitador;
    @FXML private TableColumn<Capacitacion, String> colRegistra;
    @FXML private Label totalLabel;
    @FXML private TextField buscarField;

    private SessionManager sessionManager;
    private DatabaseConnection dbConnection;

    @FXML
    public void initialize() {
        sessionManager = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        if (!sessionManager.tienePermiso(Permiso.CAPACITACIONES_LEER)) {
            mostrarError("Acceso Denegado", "No tienes permiso para ver capacitaciones.");
            return;
        }

        configurarTabla();
        cargarCapacitaciones();

        buscarField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                cargarCapacitaciones();
            } else {
                buscarCapacitaciones(newVal);
            }
        });
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmpleado.setCellValueFactory(new PropertyValueFactory<>("empleado"));
        colTema.setCellValueFactory(new PropertyValueFactory<>("tema"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colDuracion.setCellValueFactory(new PropertyValueFactory<>("duracion"));
        colCapacitador.setCellValueFactory(new PropertyValueFactory<>("capacitador"));
        colRegistra.setCellValueFactory(new PropertyValueFactory<>("usuarioRegistra"));
    }

    private void cargarCapacitaciones() {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CAPACITACIONES);
             ResultSet rs = stmt.executeQuery()) {
            ObservableList<Capacitacion> list = FXCollections.observableArrayList();
            while (rs.next()) {
                list.add(new Capacitacion(
                    rs.getInt("id_capacitacion"),
                    rs.getString("empleado"),
                    rs.getString("tema"),
                    rs.getString("fecha") != null ? rs.getString("fecha") : "",
                    rs.getInt("duracion"),
                    rs.getString("capacitador"),
                    rs.getString("usuario_registra")
                ));
            }
            capacitacionesTable.setItems(list);
            totalLabel.setText("Total: " + list.size() + " capacitaciones");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar capacitaciones: {0}", e.getMessage());
            mostrarError("Error", "No se pudieron cargar las capacitaciones.");
        }
    }

    private void buscarCapacitaciones(String texto) {
        String sql = "SELECT c.id_capacitacion, e.nombre as empleado, c.tema, c.fecha, c.duracion, c.capacitador, c.usuario_registra " +
                     "FROM capacitaciones c INNER JOIN empleados e ON c.id_empleado = e.id_empleado " +
                     "WHERE e.nombre LIKE ? OR c.tema LIKE ? OR c.capacitador LIKE ? ORDER BY c.fecha DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String busqueda = "%" + texto + "%";
            stmt.setString(1, busqueda);
            stmt.setString(2, busqueda);
            stmt.setString(3, busqueda);
            ObservableList<Capacitacion> list = FXCollections.observableArrayList();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Capacitacion(
                        rs.getInt("id_capacitacion"),
                        rs.getString("empleado"),
                        rs.getString("tema"),
                        rs.getString("fecha") != null ? rs.getString("fecha") : "",
                        rs.getInt("duracion"),
                        rs.getString("capacitador"),
                        rs.getString("usuario_registra")
                    ));
                }
            }
            capacitacionesTable.setItems(list);
            totalLabel.setText("Total: " + list.size() + " capacitaciones");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar capacitaciones: {0}", e.getMessage());
        }
    }

    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    public static class Capacitacion {
        private int id;
        private String empleado, tema, fecha, capacitador, usuarioRegistra;
        private int duracion;
        public Capacitacion(int id, String emp, String tema, String fecha, int dur, String cap, String reg) {
            this.id = id; this.empleado = emp; this.tema = tema; this.fecha = fecha;
            this.duracion = dur; this.capacitador = cap; this.usuarioRegistra = reg;
        }
        public int getId() { return id; }
        public String getEmpleado() { return empleado; }
        public String getTema() { return tema; }
        public String getFecha() { return fecha; }
        public int getDuracion() { return duracion; }
        public String getCapacitador() { return capacitador; }
        public String getUsuarioRegistra() { return usuarioRegistra; }
    }
}
