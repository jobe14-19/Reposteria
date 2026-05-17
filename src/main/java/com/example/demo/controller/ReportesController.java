package com.example.demo.controller;
import com.example.demo.service.ReportService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportesController {

    private static final Logger LOGGER = Logger.getLogger(ReportesController.class.getName());

    private static final String RUTA_REPORTES = "/reportes/";

    @FXML private ComboBox<String> reporteComboBox;
    @FXML private Button vistaPreviaButton;
    @FXML private Button exportarPdfButton;
    @FXML private Button exportarXlsxButton;
    @FXML private Label statusLabel;
    @FXML private Label parametrosLabel;
    @FXML private TextField parametroField1;
    @FXML private TextField parametroField2;
    @FXML private ComboBox<String> parametroCombo;
    @FXML private Label lblParam1;
    @FXML private Label lblParam2;

    private ReportService reportService;
    private JasperPrint currentPrint;

    @FXML
    public void initialize() {
        reportService = new ReportService();
        reporteComboBox.getItems().addAll(
            "Dashboard de Ventas",
            "Inventario",
            "Pedidos",
            "Personal",
            "Limpieza",
            "Factura de Pedido"
        );
        reporteComboBox.getSelectionModel().selectFirst();
        actualizarParametros();
        reporteComboBox.setOnAction(e -> actualizarParametros());
    }

    private void actualizarParametros() {
        String seleccion = reporteComboBox.getValue();
        parametroField1.setVisible(false);
        parametroField2.setVisible(false);
        parametroCombo.setVisible(false);
        lblParam1.setVisible(false);
        lblParam2.setVisible(false);
        parametrosLabel.setVisible(true);

        if ("Factura de Pedido".equals(seleccion)) {
            lblParam1.setVisible(true);
            lblParam1.setText("ID Pedido:");
            parametroField1.setVisible(true);
            parametroField1.setPromptText("Ej: 1");
            parametrosLabel.setText("Parámetros: ID del pedido a facturar");
        } else if ("Pedidos".equals(seleccion)) {
            lblParam1.setVisible(true);
            lblParam1.setText("Estado:");
            parametroCombo.setVisible(true);
            parametroCombo.getItems().setAll("", "Pendiente", "Confirmado", "En producción", "Entregado", "Cancelado");
            parametroCombo.getSelectionModel().selectFirst();
            parametrosLabel.setText("Parámetros: Filtrar por estado (opcional)");
        } else if ("Personal".equals(seleccion)) {
            lblParam1.setVisible(true);
            lblParam1.setText("Área:");
            parametroCombo.setVisible(true);
            parametroCombo.getItems().setAll("", "Producción", "Decoración", "Delivery", "Ventas", "Atención al Cliente", "Limpieza", "Administración");
            parametroCombo.getSelectionModel().selectFirst();
            parametrosLabel.setText("Parámetros: Filtrar por área (opcional)");
        } else if ("Limpieza".equals(seleccion)) {
            parametrosLabel.setText("Parámetros: Sin filtros - muestra todas las limpiezas");
        } else if ("Inventario".equals(seleccion)) {
            parametrosLabel.setText("Parámetros: Sin filtros - inventario completo");
        } else if ("Dashboard de Ventas".equals(seleccion)) {
            parametrosLabel.setText("Parámetros: Datos agrupados por mes");
        }
    }

    @FXML
    private void vistaPrevia(ActionEvent event) {
        ejecutarReporte(true, null);
    }

    @FXML
    private void exportarPdf(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName(reporteComboBox.getValue().replace(" ", "_") + ".pdf");
        File file = fc.showSaveDialog(vistaPreviaButton.getScene().getWindow());
        if (file != null) {
            ejecutarReporte(false, file.getAbsolutePath());
        }
    }

    @FXML
    private void exportarXlsx(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName(reporteComboBox.getValue().replace(" ", "_") + ".xlsx");
        File file = fc.showSaveDialog(vistaPreviaButton.getScene().getWindow());
        if (file != null) {
            ejecutarReporte(false, file.getAbsolutePath());
        }
    }

    private void ejecutarReporte(boolean mostrarVista, String exportPath) {
        String seleccion = reporteComboBox.getValue();
        String jrxmlPath = RUTA_REPORTES + mapearReporte(seleccion);
        Map<String, Object> params = new HashMap<>();
        cargarParametros(params);

        try {
            net.sf.jasperreports.engine.JasperReport report = reportService.compileReport(jrxmlPath);
            currentPrint = reportService.fillReport(report, params);

            if (mostrarVista) {
                reportService.showReport(currentPrint);
                setStatus("Vista previa generada");
            } else if (exportPath != null) {
                if (exportPath.endsWith(".pdf")) {
                    reportService.exportToPdf(currentPrint, exportPath);
                } else {
                    reportService.exportToXlsx(currentPrint, exportPath);
                }
                setStatus("Exportado: " + exportPath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en reporte: {0}", e.getMessage());
            setStatus("Error: " + e.getMessage());
        }
    }

    private String mapearReporte(String seleccion) {
        switch (seleccion) {
            case "Dashboard de Ventas": return "DashboardVentas.jrxml";
            case "Inventario": return "Inventario.jrxml";
            case "Pedidos": return "Pedidos.jrxml";
            case "Personal": return "Personal.jrxml";
            case "Limpieza": return "Limpieza.jrxml";
            case "Factura de Pedido": return "FacturaPedido.jrxml";
            default: return "DashboardVentas.jrxml";
        }
    }

    private void cargarParametros(Map<String, Object> params) {
        String seleccion = reporteComboBox.getValue();
        if ("Factura de Pedido".equals(seleccion)) {
            try {
                params.put("idPedido", Integer.parseInt(parametroField1.getText()));
            } catch (NumberFormatException e) {
                params.put("idPedido", 1);
            }
        } else if ("Pedidos".equals(seleccion)) {
            String estado = parametroCombo.getValue();
            params.put("estado", (estado == null || estado.isEmpty()) ? null : estado);
            params.put("fechaDesde", null);
            params.put("fechaHasta", null);
        } else if ("Personal".equals(seleccion)) {
            String area = parametroCombo.getValue();
            params.put("area", (area == null || area.isEmpty()) ? null : area);
        } else if ("Limpieza".equals(seleccion)) {
            params.put("fechaDesde", null);
            params.put("fechaHasta", null);
        }
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    @FXML
    private void cerrar(ActionEvent event) {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
