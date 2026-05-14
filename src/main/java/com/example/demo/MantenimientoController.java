package com.example.demo;

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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MantenimientoController {

    private static final Logger LOGGER = Logger.getLogger(MantenimientoController.class.getName());

    @FXML private Button registrarMantenimientoButton;
    @FXML private Button verHistorialButton;
    @FXML private Button cambiarEstadoButton;
    @FXML private Button actualizarAlertasButton;
    @FXML private ListView<String> alertasListView;
    @FXML private TableView<Maquina> maquinasTable;
    @FXML private TableColumn<Maquina, Integer> idColumn;
    @FXML private TableColumn<Maquina, String> nombreColumn;
    @FXML private TableColumn<Maquina, String> utilidadColumn;
    @FXML private TableColumn<Maquina, String> estadoColumn;
    @FXML private TableColumn<Maquina, String> ultimoMantenimientoColumn;
    @FXML private TableColumn<Maquina, String> proximoMantenimientoColumn;
    @FXML private TableColumn<Maquina, Long> diasRestantesColumn;
    @FXML private TableColumn<Maquina, Void> accionesColumn;
    @FXML private Label totalLabel;

    private DatabaseConnection dbConnection;
    private ObservableList<Maquina> maquinasList;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();
        configurarTabla();
        cargarMaquinas();
        cargarAlertas();
        setupEvents();
    }

    private void configurarTabla() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        utilidadColumn.setCellValueFactory(new PropertyValueFactory<>("utilidad"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
        ultimoMantenimientoColumn.setCellValueFactory(new PropertyValueFactory<>("ultimoMantenimiento"));
        proximoMantenimientoColumn.setCellValueFactory(new PropertyValueFactory<>("proximoMantenimiento"));
        diasRestantesColumn.setCellValueFactory(new PropertyValueFactory<>("diasRestantes"));

        // Acciones column could have buttons if needed, but for now we'll leave it empty or add a simple button
    }

    private void setupEvents() {
        registrarMantenimientoButton.setOnAction(this::abrirModalMantenimiento);
        verHistorialButton.setOnAction(e -> mostrarMensaje("Historial", "Función de historial en desarrollo"));
        cambiarEstadoButton.setOnAction(e -> mostrarMensaje("Estado", "Función de cambio de estado en desarrollo"));
        actualizarAlertasButton.setOnAction(e -> cargarAlertas());
    }

    private void cargarMaquinas() {
        maquinasList = FXCollections.observableArrayList();
        String sql = "SELECT id_maquina, nombre, utilidad, estado, ultimo_mantenimiento, proximo_mantenimiento FROM maquinas";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String ultimo = rs.getString("ultimo_mantenimiento");
                String proximo = rs.getString("proximo_mantenimiento");
                
                long dias = 0;
                if (proximo != null) {
                    try {
                        LocalDate proxDate = LocalDate.parse(proximo);
                        dias = ChronoUnit.DAYS.between(LocalDate.now(), proxDate);
                    } catch (Exception e) {
                        LOGGER.warning("Error parsing date: " + proximo);
                    }
                }

                maquinasList.add(new Maquina(
                        rs.getInt("id_maquina"),
                        rs.getString("nombre"),
                        rs.getString("utilidad"),
                        rs.getString("estado"),
                        ultimo,
                        proximo,
                        dias
                ));
            }
            maquinasTable.setItems(maquinasList);
            totalLabel.setText("Total: " + maquinasList.size() + " máquinas");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar máquinas: {0}", e.getMessage());
            // Si la tabla no existe, podríamos mostrar un mensaje amistoso
        }
    }

    private void cargarAlertas() {
        ObservableList<String> alertas = FXCollections.observableArrayList();
        // Simular alertas basadas en días restantes
        if (maquinasList != null) {
            for (Maquina m : maquinasList) {
                if (m.getDiasRestantes() <= 7) {
                    alertas.add("⚠️ " + m.getNombre() + " requiere mantenimiento en " + m.getDiasRestantes() + " días");
                }
            }
        }
        if (alertas.isEmpty()) {
            alertas.add("✅ Todos los equipos están al día");
        }
        alertasListView.setItems(alertas);
    }

    private void abrirModalMantenimiento(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MantenimientoModal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Registro de Mantenimiento");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarMaquinas();
            cargarAlertas();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir el modal de mantenimiento");
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class Maquina {
        private int id;
        private String nombre;
        private String utilidad;
        private String estado;
        private String ultimoMantenimiento;
        private String proximoMantenimiento;
        private long diasRestantes;

        public Maquina(int id, String nombre, String utilidad, String estado, String ultimoMantenimiento, String proximoMantenimiento, long diasRestantes) {
            this.id = id;
            this.nombre = nombre;
            this.utilidad = utilidad;
            this.estado = estado;
            this.ultimoMantenimiento = ultimoMantenimiento;
            this.proximoMantenimiento = proximoMantenimiento;
            this.diasRestantes = diasRestantes;
        }

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUtilidad() { return utilidad; }
        public String getEstado() { return estado; }
        public String getUltimoMantenimiento() { return ultimoMantenimiento; }
        public String getProximoMantenimiento() { return proximoMantenimiento; }
        public long getDiasRestantes() { return diasRestantes; }
    }
}