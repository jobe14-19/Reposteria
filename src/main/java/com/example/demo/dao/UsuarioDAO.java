package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioDAO {

 private static final Logger LOGGER = Logger.getLogger(UsuarioDAO.class.getName());
 private final DatabaseConnection dbConnection;

 private static final String SQL_VALIDAR_USUARIO =
 "SELECT id_usuario, nombre, perfil FROM usuarios WHERE usuario = ? AND contrasena = ? AND estado = 'Activo'";

 public UsuarioDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public Optional<DatabaseConnection.Usuario> validarCredenciales(String usuario, String contrasena) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_VALIDAR_USUARIO)) {

 stmt.setString(1, usuario);
 stmt.setString(2, contrasena);

 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 return Optional.of(new DatabaseConnection.Usuario(
 rs.getInt("id_usuario"),
 rs.getString("nombre"),
 rs.getString("perfil")
 ));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: validando usuario localmente");
 return dbConnection.getUsuarioPorCredenciales(usuario, contrasena);
 }

 return Optional.empty();
 }

 public String obtenerAreaEmpleado(int idUsuario) {
 String sql = "SELECT area FROM empleados WHERE id_empleado = ?";
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, idUsuario);
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 return rs.getString("area");
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al obtener área del empleado: {0}", e.getMessage());
 }
 return "";
 }
}
