package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;
import com.example.demo.model.Ingrediente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventarioDAO {

 private static final Logger LOGGER = Logger.getLogger(InventarioDAO.class.getName());
 private final DatabaseConnection dbConnection;

 private static final String SQL_CARGAR_INVENTARIO = 
 "SELECT i.id_ingrediente, i.nombre, i.categoria, i.unidad, i.stock_actual, i.stock_minimo, " + 
 "CASE WHEN i.stock_actual < i.stock_minimo THEN 'Crítico' " + 
 "WHEN i.stock_actual <= i.stock_minimo * 1.2 THEN 'Bajo' " + 
 "ELSE 'Normal' END as estado " + 
"FROM ingredientes i WHERE i.estado = 'Activo' ORDER BY i.nombre";

private static final String SQL_CARGAR_ALERTAS =
 "SELECT i.nombre + ' - Stock: ' + CAST(i.stock_actual AS VARCHAR) + ' / ' + " + 
 "CAST(i.stock_minimo AS VARCHAR) + ' (' + i.unidad + ')' as alerta " + 
 "FROM ingredientes i WHERE i.estado = 'Activo' AND i.stock_actual <= i.stock_minimo * 1.2 " + 
 "ORDER BY i.stock_actual";

 private static final String SQL_ELIMINAR_INGREDIENTE = "UPDATE ingredientes SET estado = 'Inactivo' WHERE id_ingrediente = ?";

public InventarioDAO() {
  this.dbConnection = DatabaseConnection.getInstance();
  asegurarColumnaEstado();
  }

  private void asegurarColumnaEstado() {
  String sql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('ingredientes') AND name = 'estado') ALTER TABLE ingredientes ADD estado NVARCHAR(10) DEFAULT 'Activo'";
  try (Connection conn = dbConnection.getConnection();
  Statement stmt = conn.createStatement()) {
  stmt.execute(sql);
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "No se pudo agregar columna estado a ingredientes: {0}", e.getMessage());
  }
  }

 public List<Ingrediente> obtenerInventario() {
 List<Ingrediente> ingredientes = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_INVENTARIO);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 ingredientes.add(new Ingrediente(
 rs.getInt("id_ingrediente"),
 rs.getString("nombre"),
 rs.getString("categoria"),
 rs.getString("unidad"),
 rs.getDouble("stock_actual"),
 rs.getDouble("stock_minimo"),
 rs.getString("estado")
 ));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar inventario: {0}", e.getMessage());
 }
 return ingredientes;
 }

 public List<String> obtenerAlertas() {
 List<String> alertas = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_ALERTAS);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 alertas.add(rs.getString("alerta"));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al cargar alertas: {0}", e.getMessage());
 }
 return alertas;
 }

 public boolean eliminarIngrediente(int idIngrediente) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_INGREDIENTE)) {
 stmt.setInt(1, idIngrediente);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al eliminar ingrediente: {0}", e.getMessage());
 return false;
 }
 }
}
