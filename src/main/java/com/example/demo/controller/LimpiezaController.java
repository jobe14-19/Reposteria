package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import com.example.demo.service.ReportService;

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

public class LimpiezaController {

    private static final Logger LOGGER = Logger.getLogger(LimpiezaController.class.getName());

    @FXML private Button registrarLimpiezaButton;
    @FXML private Button exportarReporteButton;
    @FXML private Button actualizarMaterialesButton;
    @FXML private CheckBox guantesCheckBox;
    @FXML private CheckBox panosCheckBox;
    @FXML private CheckBox detergenteCheckBox;
    @FXML private CheckBox desinfectanteCheckBox;
    @FXML private CheckBox alcoholCheckBox;
    @FXML private CheckBox escobillonCheckBox;
    @FXML private TableView<LimpiezaRecord> limpiezaTable;
    @FXML private TableColumn<LimpiezaRecord, String> areaColumn;
    @FXML private TableColumn<LimpiezaRecord, String> ultimaLimpiezaColumn;
    @FXML private TableColumn<LimpiezaRecord, Long> diasSinLimpiezaColumn;
    @FXML private TableColumn<LimpiezaRecord, String> estadoColumn;
    @FXML private TableColumn<LimpiezaRecord, Void> accionColumn;
    @FXML private Label totalLabel;

    private DatabaseConnection dbConnection;
    private ObservableList<LimpiezaRecord> limpiezaList;

    @FXML
    public void initialize() {
        dbConnection = DatabaseConnection.getInstance();

        SessionManager session = SessionManager.getInstance();
        if (!session.isAdmin() && !session.isAreaLimpieza()) {
            mostrarError("Acceso Denegado", "Solo administradores y personal de limpieza pueden acceder a esta sección.");
            return;
        }

        configurarTabla();
        cargarDatosLimpieza();
        setupEvents();
    }

    private void configurarTabla() {
        areaColumn.setCellValueFactory(new PropertyValueFactory<>("area"));
        ultimaLimpiezaColumn.setCellValueFactory(new PropertyValueFactory<>("ultimaLimpieza"));
        diasSinLimpiezaColumn.setCellValueFactory(new PropertyValueFactory<>("diasSinLimpieza"));
        estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void setupEvents() {
        registrarLimpiezaButton.setOnAction(this::abrirModalLimpieza);
        exportarReporteButton.setOnAction(e -> {
            try {
                ReportService rs = new ReportService();
                net.sf.jasperreports.engine.JasperReport report = rs.compileReport("/reportes/Limpieza.jrxml");
                net.sf.jasperreports.engine.JasperPrint print = rs.fillReport(report, new java.util.HashMap<>());
                rs.showReport(print);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error al generar reporte: {0}", ex.getMessage());
                mostrarMensaje("Error", "No se pudo generar el reporte: " + ex.getMessage());
            }
        });
        actualizarMaterialesButton.setOnAction(e -> abrirGestionMateriales());
    }

    private void cargarDatosLimpieza() {
        limpiezaList = FXCollections.observableArrayList();
        // Agrupamos por área para obtener la última fecha de limpieza
        String sql = "SELECT area, MAX(fecha_limpieza) as ultima_fecha FROM limpieza GROUP BY area";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String area = rs.getString("area");
                String ultima = rs.getString("ultima_fecha");
                
                long dias = 0;
                String estado = "Al día";
                if (ultima != null) {
                    try {
                        LocalDate lastDate = LocalDate.parse(ultima);
                        dias = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
                        if (dias > 3) estado = "Pendiente";
                        if (dias > 7) estado = "Crítico";
                    } catch (Exception e) {
                        LOGGER.warning("Error parsing date: " + ultima);
                    }
                }

                limpiezaList.add(new LimpiezaRecord(area, ultima, dias, estado));
            }
            limpiezaTable.setItems(limpiezaList);
            totalLabel.setText("Total: " + limpiezaList.size() + " áreas");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos de limpieza: {0}", e.getMessage());
        }
    }

    private void abrirModalLimpieza(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/LimpiezaModal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Registro de Limpieza");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarDatosLimpieza();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir modal: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir el modal de limpieza");
        }
    }

    private void abrirGestionMateriales() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MaterialModal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Gestión de Materiales de Limpieza");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir gestión de materiales: {0}", e.getMessage());
            mostrarError("Error", "No se pudo abrir la gestión de materiales");
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

    public static class LimpiezaRecord {
        private String area;
        private String ultimaLimpieza;
        private long diasSinLimpieza;
        private String estado;

        public LimpiezaRecord(String area, String ultimaLimpieza, long diasSinLimpieza, String estado) {
            this.area = area;
            this.ultimaLimpieza = ultimaLimpieza;
            this.diasSinLimpieza = diasSinLimpieza;
            this.estado = estado;
        }

        public String getArea() { return area; }
        public String getUltimaLimpieza() { return ultimaLimpieza; }
        public long getDiasSinLimpieza() { return diasSinLimpieza; }
        public String getEstado() { return estado; }
    }
}
