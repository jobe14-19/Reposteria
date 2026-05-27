package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MantenimientoDAO {

 private static final Logger LOGGER = Logger.getLogger(MantenimientoDAO.class.getName());
 private final DatabaseConnection dbConnection;

  private static final String SQL_REGISTRAR_MANTENIMIENTO =
  "INSERT INTO mantenimiento (equipo, descripcion, tecnico, fecha_mantenimiento, proximo_mantenimiento) VALUES (?, ?, ?, ?, ?)";

  private static final String SQL_OBTENER_HISTORIAL =
  "SELECT id_mantenimiento, equipo, descripcion, tecnico, fecha_mantenimiento, proximo_mantenimiento FROM mantenimiento WHERE equipo = ? ORDER BY fecha_mantenimiento DESC";

 public MantenimientoDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

  public List<HistorialEntry> obtenerHistorial(String equipo) {
    List<HistorialEntry> historial = new ArrayList<>();
    try (Connection conn = dbConnection.getConnection();
    PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_HISTORIAL)) {
    stmt.setString(1, equipo);
    try (ResultSet rs = stmt.executeQuery()) {
    while (rs.next()) {
    historial.add(new HistorialEntry(
    rs.getInt("id_mantenimiento"),
    rs.getString("equipo"),
    rs.getString("descripcion"),
    rs.getString("tecnico"),
    rs.getString("fecha_mantenimiento"),
    rs.getString("proximo_mantenimiento")
    ));
    }
    }
    } catch (SQLException e) {
    LOGGER.log(Level.SEVERE, "Error al obtener historial de mantenimiento: {0}", e.getMessage());
    }
    return historial;
    }

  public boolean registrarMantenimiento(String equipo, String descripcion, String tecnico, java.time.LocalDate fechaMantenimiento, java.time.LocalDate proximoMantenimiento) {
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_MANTENIMIENTO)) {
  
  stmt.setString(1, equipo);
  stmt.setString(2, descripcion);
  stmt.setString(3, tecnico);
  stmt.setDate(4, java.sql.Date.valueOf(fechaMantenimiento));
  stmt.setDate(5, proximoMantenimiento != null ? java.sql.Date.valueOf(proximoMantenimiento) : null);
  
  return stmt.executeUpdate() > 0;
  
  } catch (SQLException e) {
  LOGGER.log(Level.SEVERE, "Error al registrar mantenimiento: {0}", e.getMessage());
  return false;
  }
  }

  public record HistorialEntry(int id, String equipo, String descripcion, String tecnico, String fecha, String proximo) {}
}
