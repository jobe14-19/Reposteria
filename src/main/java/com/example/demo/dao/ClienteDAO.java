package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;
import com.example.demo.service.SessionManager;
import com.example.demo.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteDAO {

 private static final Logger LOGGER = Logger.getLogger(ClienteDAO.class.getName());
 private final DatabaseConnection dbConnection;

 // Consultas SQL
 private static final String SQL_CARGAR_CLIENTES =
 "SELECT c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(COUNT(p.id_pedido), 0) as total_pedidos FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente WHERE c.estado = 'Activo' GROUP BY c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, c.fecha_registro ORDER BY c.nombre, c.apellido";

 private static final String SQL_BUSCAR_CLIENTES =
 "SELECT c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(COUNT(p.id_pedido), 0) as total_pedidos FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente WHERE c.estado = 'Activo' AND (c.nombre LIKE ? OR c.apellido LIKE ? OR c.telefono LIKE ?) GROUP BY c.id_cliente, c.nombre, c.apellido, c.telefono, c.email, c.fecha_registro ORDER BY c.nombre, c.apellido";

 private static final String SQL_ELIMINAR_CLIENTE = "UPDATE clientes SET estado = 'Inactivo' WHERE id_cliente = ?";

 private static final String SQL_UPDATE_CLIENTE =
 "UPDATE clientes SET nombre = ?, apellido = ?, telefono = ?, email = ?, direccion = ?, rnc = ?, usuario = ?, contrasena = ?, fecha_modificacion = GETDATE() WHERE id_cliente = ?";

 private static final String SQL_INSERT_CLIENTE =
 "INSERT INTO clientes (nombre, apellido, telefono, email, direccion, rnc, usuario, contrasena, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";

 private static final String SQL_VALIDAR_CLIENTE =
 "SELECT id_cliente, nombre, apellido FROM clientes WHERE usuario = ? AND contrasena = ?";

 public ClienteDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

  public Optional<DatabaseConnection.Usuario> validarCredenciales(String usuario, String contrasena) {
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(SQL_VALIDAR_CLIENTE)) {

  stmt.setString(1, usuario);
  stmt.setString(2, contrasena);

  try (ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) {
  String nombreCompleto = rs.getString("nombre") + " " + rs.getString("apellido");
  return Optional.of(new DatabaseConnection.Usuario(
  rs.getInt("id_cliente"),
  nombreCompleto.trim(),
  SessionManager.PERFIL_CLIENTE
  ));
  }
  }
  } catch (SQLException e) {
  LOGGER.log(Level.INFO, "BD no disponible, usando validación offline: {0}", e.getMessage());
  }

  // Fallback a validación offline
  return dbConnection.getUsuarioPorCredenciales(usuario, contrasena);
  }

 public List<Cliente> obtenerTodosLosClientes() {
 List<Cliente> clientes = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_CLIENTES);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 clientes.add(mapearClienteDesdeResultSetResumen(rs));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar clientes: {0}", e.getMessage());
 }
 return clientes;
 }

 public List<Cliente> buscarClientes(String textoBusqueda) {
 List<Cliente> clientes = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_CLIENTES)) {

 String busqueda = "%" + textoBusqueda + "%";
 stmt.setString(1, busqueda);
 stmt.setString(2, busqueda);
 stmt.setString(3, busqueda);

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 clientes.add(mapearClienteDesdeResultSetResumen(rs));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al buscar clientes: {0}", e.getMessage());
 }
 return clientes;
 }
 
 public List<Cliente> aplicarFiltros(String consultaSQL, Object... parametros) {
 List<Cliente> clientes = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(consultaSQL)) {

 for (int i = 0; i < parametros.length; i++) {
 stmt.setObject(i + 1, parametros[i]);
 }

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 clientes.add(mapearClienteDesdeResultSetResumen(rs));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
 }
 return clientes;
 }

 public boolean eliminarCliente(int idCliente) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_CLIENTE)) {
 stmt.setInt(1, idCliente);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al eliminar cliente: {0}", e.getMessage());
 return false;
 }
 }

 public boolean insertarCliente(Cliente cliente) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_CLIENTE)) {
 stmt.setString(1, cliente.getNombre());
 stmt.setString(2, cliente.getApellido());
 stmt.setString(3, cliente.getTelefono());
 stmt.setString(4, cliente.getEmail());
 stmt.setString(5, cliente.getDireccion());
 stmt.setString(6, cliente.getRnc());
 stmt.setString(7, cliente.getUsuario());
 stmt.setString(8, cliente.getContrasena());
 
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al insertar cliente: {0}", e.getMessage());
 return false;
 }
 }

 public boolean actualizarCliente(Cliente cliente) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_CLIENTE)) {
 stmt.setString(1, cliente.getNombre());
 stmt.setString(2, cliente.getApellido());
 stmt.setString(3, cliente.getTelefono());
 stmt.setString(4, cliente.getEmail());
 stmt.setString(5, cliente.getDireccion());
 stmt.setString(6, cliente.getRnc());
 stmt.setString(7, cliente.getUsuario());
 stmt.setString(8, cliente.getContrasena());
 stmt.setInt(9, cliente.getId());
 
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al actualizar cliente: {0}", e.getMessage());
 return false;
 }
 }

 private Cliente mapearClienteDesdeResultSetResumen(ResultSet rs) throws SQLException {
 return new Cliente(
 rs.getInt("id_cliente"),
 rs.getString("nombre"),
 rs.getString("apellido"),
 rs.getString("telefono"),
 rs.getString("email"),
 rs.getString("fecha_registro"),
 rs.getInt("total_pedidos")
 );
 }
}
