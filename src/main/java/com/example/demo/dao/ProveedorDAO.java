package com.example.demo.dao;

import com.example.demo.model.Proveedor;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProveedorDAO {

 private static final Logger LOGGER = Logger.getLogger(ProveedorDAO.class.getName());
 private final DatabaseConnection dbConnection;

 public ProveedorDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public List<Proveedor> listarTodos() {
 List<Proveedor> lista = new ArrayList<>();
 String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, direccion, estado FROM proveedores ORDER BY nombre";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 lista.add(new Proveedor(rs.getInt("id_proveedor"), rs.getString("nombre"),
 rs.getString("contacto"), rs.getString("telefono"),
 rs.getString("email"), rs.getString("direccion"), rs.getString("estado")));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al listar proveedores: {0}", e.getMessage());
 }
 return lista;
 }

 public Proveedor obtenerPorId(int id) {
 String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, direccion, estado FROM proveedores WHERE id_proveedor=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, id);
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 return new Proveedor(rs.getInt("id_proveedor"), rs.getString("nombre"),
 rs.getString("contacto"), rs.getString("telefono"),
 rs.getString("email"), rs.getString("direccion"), rs.getString("estado"));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al obtener proveedor: {0}", e.getMessage());
 }
 return null;
 }

 public boolean insertar(Proveedor p) {
 String sql = "INSERT INTO proveedores (nombre, contacto, telefono, email, direccion) VALUES (?, ?, ?, ?, ?)";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, p.getNombre());
 stmt.setString(2, p.getContacto());
 stmt.setString(3, p.getTelefono());
 stmt.setString(4, p.getEmail());
 stmt.setString(5, p.getDireccion());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al insertar proveedor: {0}", e.getMessage());
 return false;
 }
 }

 public boolean actualizar(Proveedor p) {
 String sql = "UPDATE proveedores SET nombre=?, contacto=?, telefono=?, email=?, direccion=? WHERE id_proveedor=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, p.getNombre());
 stmt.setString(2, p.getContacto());
 stmt.setString(3, p.getTelefono());
 stmt.setString(4, p.getEmail());
 stmt.setString(5, p.getDireccion());
 stmt.setInt(6, p.getId());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al actualizar proveedor: {0}", e.getMessage());
 return false;
 }
 }

 public boolean toggleEstado(int id) {
 String sql = "UPDATE proveedores SET estado = CASE WHEN estado='Activo' THEN 'Inactivo' ELSE 'Activo' END WHERE id_proveedor=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, id);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cambiar estado proveedor: {0}", e.getMessage());
 return false;
 }
 }
}
