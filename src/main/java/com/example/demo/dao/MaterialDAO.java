package com.example.demo.dao;

import com.example.demo.model.Material;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialDAO {

 private static final Logger LOGGER = Logger.getLogger(MaterialDAO.class.getName());
 private final DatabaseConnection dbConnection;

 public MaterialDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public List<Material> obtenerTodos() {
 List<Material> materiales = new ArrayList<>();
 String sql = "SELECT id_material, nombre, unidad, stock_actual, stock_minimo FROM materiales WHERE estado = 'Activo' ORDER BY nombre";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 materiales.add(new Material(
 rs.getInt("id_material"),
 rs.getString("nombre"),
 rs.getString("unidad"),
 rs.getInt("stock_actual"),
 rs.getInt("stock_minimo")
 ));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al obtener materiales: {0}", e.getMessage());
 }
 return materiales;
 }

 public boolean insertar(Material material) {
 String sql = "INSERT INTO materiales (nombre, unidad, stock_actual, stock_minimo) VALUES (?, ?, ?, ?)";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, material.getNombre());
 stmt.setString(2, material.getUnidad());
 stmt.setInt(3, material.getStockActual());
 stmt.setInt(4, material.getStockMinimo());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al insertar material: {0}", e.getMessage());
 return false;
 }
 }

 public boolean actualizar(Material material) {
 String sql = "UPDATE materiales SET nombre = ?, unidad = ?, stock_actual = ?, stock_minimo = ? WHERE id_material = ?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, material.getNombre());
 stmt.setString(2, material.getUnidad());
 stmt.setInt(3, material.getStockActual());
 stmt.setInt(4, material.getStockMinimo());
 stmt.setInt(5, material.getId());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al actualizar material: {0}", e.getMessage());
 return false;
 }
 }

 public boolean eliminar(int id) {
 String sql = "UPDATE materiales SET estado = 'Inactivo' WHERE id_material = ?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, id);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al eliminar material: {0}", e.getMessage());
 return false;
 }
 }

 public void crearTablaSiNoExiste() {
 String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='materiales' AND xtype='U') " + 
"CREATE TABLE materiales (" + 
"id_material INT IDENTITY(1,1) PRIMARY KEY, " + 
"nombre VARCHAR(100) NOT NULL, " + 
"unidad VARCHAR(50) DEFAULT 'unidad', " + 
"stock_actual INT DEFAULT 0, " + 
"stock_minimo INT DEFAULT 1, " + 
"estado VARCHAR(10) DEFAULT 'Activo')";
 try (Connection conn = dbConnection.getConnection();
 Statement stmt = conn.createStatement()) {
stmt.execute(sql);
  String alterSql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('materiales') AND name = 'estado') ALTER TABLE materiales ADD estado VARCHAR(10) DEFAULT 'Activo'";
  try { stmt.execute(alterSql); } catch (SQLException ignored) {}
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "No se pudo crear tabla materiales: {0}", e.getMessage());
  }
  }
}
