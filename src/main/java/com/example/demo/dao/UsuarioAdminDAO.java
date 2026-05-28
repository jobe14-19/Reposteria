package com.example.demo.dao;

import com.example.demo.model.UsuarioAdmin;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioAdminDAO {

 private static final Logger LOGGER = Logger.getLogger(UsuarioAdminDAO.class.getName());
 private final DatabaseConnection dbConnection;

 public UsuarioAdminDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public List<UsuarioAdmin> listarTodos() {
 List<UsuarioAdmin> lista = new ArrayList<>();
 String sql = "SELECT id_usuario, usuario, nombre, perfil, estado, CONVERT(VARCHAR, fecha_registro, 120) as fecha_registro FROM usuarios ORDER BY nombre";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 lista.add(new UsuarioAdmin(rs.getInt("id_usuario"), rs.getString("usuario"),
 rs.getString("nombre"), rs.getString("perfil"),
 rs.getString("estado"), rs.getString("fecha_registro")));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al listar usuarios: {0}", e.getMessage());
 }
 return lista;
 }

 public boolean insertar(UsuarioAdmin u) {
 String sql = "INSERT INTO usuarios (usuario, contrasena, nombre, perfil) VALUES (?, ?, ?, ?)";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, u.getUsuario());
 stmt.setString(2, u.getContrasena());
 stmt.setString(3, u.getNombre());
 stmt.setString(4, u.getPerfil());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al insertar usuario: {0}", e.getMessage());
 return false;
 }
 }

 public boolean actualizar(UsuarioAdmin u) {
 String sql = "UPDATE usuarios SET usuario=?, nombre=?, perfil=? WHERE id_usuario=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, u.getUsuario());
 stmt.setString(2, u.getNombre());
 stmt.setString(3, u.getPerfil());
 stmt.setInt(4, u.getId());
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al actualizar usuario: {0}", e.getMessage());
 return false;
 }
 }

 public boolean actualizarContrasena(int id, String nuevaContrasena) {
 String sql = "UPDATE usuarios SET contrasena=? WHERE id_usuario=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, nuevaContrasena);
 stmt.setInt(2, id);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al actualizar contrasena: {0}", e.getMessage());
 return false;
 }
 }

 public boolean toggleEstado(int id) {
 String sql = "UPDATE usuarios SET estado = CASE WHEN estado='Activo' THEN 'Inactivo' ELSE 'Activo' END WHERE id_usuario=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, id);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cambiar estado usuario: {0}", e.getMessage());
 return false;
 }
 }

 public boolean existeUsuario(String usuario) {
 String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario=?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setString(1, usuario);
 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() && rs.getInt(1) > 0;
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al verificar usuario: {0}", e.getMessage());
 return false;
 }
 }
}
