package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LimpiezaDAO {

 private static final Logger LOGGER = Logger.getLogger(LimpiezaDAO.class.getName());
 private final DatabaseConnection dbConnection;

 private static final String SQL_REGISTRAR_LIMPIEZA =
 "INSERT INTO limpieza (area, descripcion, responsable, fecha_limpieza) VALUES (?, ?, ?, ?)";

 public LimpiezaDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public boolean registrarLimpieza(String area, String descripcion, String responsable, java.time.LocalDate fechaLimpieza) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_LIMPIEZA)) {
 
 stmt.setString(1, area);
 stmt.setString(2, descripcion);
 stmt.setString(3, responsable);
 stmt.setDate(4, java.sql.Date.valueOf(fechaLimpieza));
 
 return stmt.executeUpdate() > 0;
 
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al registrar limpieza: {0}", e.getMessage());
 return false;
 }
 }
}
