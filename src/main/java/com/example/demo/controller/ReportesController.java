package com.example.demo.controller;
import com.example.demo.service.ReportService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
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
 @FXML private DatePicker fechaInicioPicker;
 @FXML private DatePicker fechaFinPicker;

 private ReportService reportService;
 private JasperPrint currentPrint;

 @FXML
 public void initialize() {
 reportService = new ReportService();
 reporteComboBox.getItems().addAll(
 "Inventario",
 "Reporte de Ventas",
 "Factura de Pedido"
 );
 reporteComboBox.getSelectionModel().selectFirst();
 actualizarParametros();
 reporteComboBox.setOnAction(e -> actualizarParametros());
 }

 private void actualizarParametros() {
 String seleccion = reporteComboBox.getValue();
 parametroField1.setVisible(false);
 fechaInicioPicker.setVisible(false);
 fechaFinPicker.setVisible(false);
 parametrosLabel.setVisible(true);

 if ("Factura de Pedido".equals(seleccion)) {
 parametroField1.setVisible(true);
 parametroField1.setPromptText("Ej: 1");
 parametrosLabel.setText("Ingrese el ID de la factura:");
 } else if ("Reporte de Ventas".equals(seleccion)) {
 fechaInicioPicker.setVisible(true);
 fechaFinPicker.setVisible(true);
 fechaInicioPicker.setValue(LocalDate.now().withDayOfMonth(1));
 fechaFinPicker.setValue(LocalDate.now());
 parametrosLabel.setText("Seleccione el rango de fechas:");
 } else {
 parametrosLabel.setText("Sin parámetros — reporte completo.");
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
 javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
 alert.setTitle("Error al generar reporte");
 alert.setHeaderText("No se pudo generar el reporte");
 alert.setContentText(e.getMessage() + "\n\nVerifique que las tablas y datos existan en la base de datos.");
 alert.showAndWait();
 }
 }

 private String mapearReporte(String seleccion) {
 switch (seleccion) {
 case "Inventario": return "Reporte_Inventario.jrxml";
 case "Reporte de Ventas": return "Reporte de Ventas.jrxml";
 case "Factura de Pedido": return "Reporte_Factura.jrxml";
 default: return "Reporte_Inventario.jrxml";
 }
 }

 private void cargarParametros(Map<String, Object> params) {
 String seleccion = reporteComboBox.getValue();
 if ("Factura de Pedido".equals(seleccion)) {
 try {
 params.put("ID_FACTURA", Integer.parseInt(parametroField1.getText()));
 } catch (NumberFormatException e) {
 params.put("ID_FACTURA", 1);
 }
 } else if ("Reporte de Ventas".equals(seleccion)) {
 LocalDate inicio = fechaInicioPicker.getValue();
 LocalDate fin = fechaFinPicker.getValue();
 if (inicio != null) {
 params.put("fechaInicio", Date.valueOf(inicio));
 }
 if (fin != null) {
 params.put("fechaFin", Date.valueOf(fin));
 }
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
