package com.example.demo.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.view.JasperViewer;

import com.example.demo.util.DatabaseConnection;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportService {

    private static final Logger LOGGER = Logger.getLogger(ReportService.class.getName());

    private final DatabaseConnection dbConnection;

    public ReportService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public JasperReport compileReport(String jrxmlResource) throws JRException {
        InputStream stream = getClass().getResourceAsStream(jrxmlResource);
        if (stream == null) {
            throw new JRException("Report not found: " + jrxmlResource);
        }
        return JasperCompileManager.compileReport(stream);
    }

    public JasperPrint fillReport(JasperReport report, Map<String, Object> params) throws JRException {
        try {
            Connection conn = dbConnection.getConnection();
            return JasperFillManager.fillReport(report, params, conn);
        } catch (java.sql.SQLException e) {
            throw new JRException("Error getting database connection", e);
        }
    }

    public JasperPrint fillReport(JasperReport report, Map<String, Object> params, Connection conn) throws JRException {
        return JasperFillManager.fillReport(report, params, conn);
    }

    public void showReport(JasperPrint jasperPrint) {
        JasperViewer.viewReport(jasperPrint, false);
    }

    public void exportToPdf(JasperPrint jasperPrint, String outputPath) throws JRException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        exporter.exportReport();
        LOGGER.log(Level.INFO, "PDF exported: {0}", outputPath);
    }

    public void exportToXlsx(JasperPrint jasperPrint, String outputPath) throws JRException {
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
        exporter.exportReport();
        LOGGER.log(Level.INFO, "XLSX exported: {0}", outputPath);
    }

    public JasperPrint generateAndShow(String jrxmlPath, Map<String, Object> params) {
        try {
            JasperReport report = compileReport(jrxmlPath);
            JasperPrint print = fillReport(report, params);
            showReport(print);
            return print;
        } catch (JRException e) {
            LOGGER.log(Level.SEVERE, "Error generating report: {0}", e.getMessage());
            return null;
        }
    }

    public void generateAndExport(String jrxmlPath, Map<String, Object> params, String outputPath, boolean isPdf) {
        try {
            JasperReport report = compileReport(jrxmlPath);
            JasperPrint print = fillReport(report, params);
            if (isPdf) {
                exportToPdf(print, outputPath);
            } else {
                exportToXlsx(print, outputPath);
            }
        } catch (JRException e) {
            LOGGER.log(Level.SEVERE, "Error exporting report: {0}", e.getMessage());
        }
    }
}
