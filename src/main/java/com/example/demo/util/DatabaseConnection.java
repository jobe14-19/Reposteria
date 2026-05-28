package com.example.demo.util;

import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;

public class DatabaseConnection {

 private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

 // Singleton instance
 private static DatabaseConnection instance;

 // Database connection parameters
 private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
 // MODO OFFLINE ACTIVADO - permite usar la aplicaci\u00f3n sin conexi\u00f3n a BD
 private static final boolean OFFLINE_MODE = false;
 
 // Autenticación SQL Server
 private static final String URL = "jdbc:sqlserver://DESKTOP-6EJ5FRR\\JOBETTE:1433;databaseName=Reposteria;encrypt=true;trustServerCertificate=true";
 private static final String USER = "AnelizEr";
 private static final String PASSWORD = "12345678";
 private static final String LOG_FILE = "database_errors.log";
 private static final int CONNECTION_TIMEOUT_SECONDS = 5;

 // SQL Constants
 private static final String SQL_VALIDAR_USUARIO =
 "SELECT id_usuario, nombre, perfil FROM usuarios WHERE usuario = ? AND contrasena = ? AND estado = 'Activo'";

 private static final String SQL_OBTENER_CLIENTES =
 "SELECT id_cliente, nombre, telefono, email, direccion, usuario, estado FROM clientes ORDER BY nombre";

 private static final String SQL_INSERTAR_CLIENTE =
 "INSERT INTO clientes (nombre, telefono, email, direccion, usuario, contrasena, estado) VALUES (?, ?, ?, ?, ?, ?, 'Activo')";

 private static final String SQL_ACTUALIZAR_CLIENTE =
 "UPDATE clientes SET nombre = ?, telefono = ?, email = ?, direccion = ? WHERE id_cliente = ?";

 private static final String SQL_VERIFICAR_PEDIDOS_CLIENTE =
 "SELECT COUNT(*) as total FROM pedidos WHERE id_cliente = ?";

 private static final String SQL_ELIMINAR_CLIENTE = "DELETE FROM clientes WHERE id_cliente = ?";

 // Private constructor for Singleton pattern
 private DatabaseConnection() {
 try {
 Class.forName(DRIVER);
 LOGGER.log(Level.INFO, "Driver SQL Server cargado correctamente");
 ejecutarMigraciones();
 } catch (ClassNotFoundException e) {
 String error = "Error al cargar el driver SQL Server: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Driver", error);
 }
 }

 private void ejecutarMigraciones() {
 String[] sqls = {
 "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='tipo_pago') " +
 "ALTER TABLE pedidos ADD tipo_pago NVARCHAR(50) DEFAULT 'Efectivo'",
 "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='estado_pago') " +
 "ALTER TABLE pedidos ADD estado_pago NVARCHAR(20) DEFAULT 'Pendiente'"
 };
 try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
 for (String sql : sqls) stmt.execute(sql);
 LOGGER.log(Level.INFO, "Migraciones de base de datos ejecutadas correctamente");
 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "No se pudieron ejecutar migraciones: {0}", e.getMessage());
 }
 }

 // Get Singleton instance
 public static synchronized DatabaseConnection getInstance() {
 if (instance == null) {
 instance = new DatabaseConnection();
 }
 return instance;
 }

 /**
 * Get database connection.
 * When OFFLINE_MODE is true, throws SQLException immediately without attempting connection.
 */
 public Connection getConnection() throws SQLException {
 if (OFFLINE_MODE) {
 throw new SQLException(
 "Aplicaci\u00f3n en modo offline (OFFLINE_MODE=true). " + 
 "Configure una base de datos o desactive el modo offline en DatabaseConnection.java"
 );
 }
 try {
 Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
 LOGGER.log(Level.INFO, "Conexi\u00f3n a la base de datos establecida correctamente");
 return conn;
 } catch (SQLException e) {
 String error = "Error al establecer la conexi\u00f3n a la base de datos: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 throw new SQLException(error, e);
 }
 }

 // Close specific connection
 public void closeConnection(Connection conn) {
 try {
 if (conn != null && !conn.isClosed()) {
 conn.close();
 LOGGER.log(Level.INFO, "Conexi\u00f3n espec\u00edfica cerrada correctamente");
 }
 } catch (SQLException e) {
 String error = "Error al cerrar la conexi\u00f3n espec\u00edfica: " + e.getMessage();
 LOGGER.log(Level.WARNING, error);
 logError(error);
 }
 }

 // Execute SELECT query with parameters
 public Optional<ResultSet> executeQuery(String sql, Object... params) {
 try {
 Connection conn = getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql);

 for (int i = 0; i < params.length; i++) {
 stmt.setObject(i + 1, params[i]);
 }

 return Optional.of(stmt.executeQuery());
 } catch (SQLException e) {
 String error = "Error al ejecutar query SELECT: " + e.getMessage() + "\nSQL: " + sql;
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Consulta", "No se pudo ejecutar la consulta: " + e.getMessage());
 return Optional.empty();
 }
 }

 // Execute INSERT/UPDATE/DELETE with parameters
 public int executeUpdate(String sql, Object... params) {
 try (Connection conn = getConnection();
 PreparedStatement stmt = conn.prepareStatement(sql)) {

 for (int i = 0; i < params.length; i++) {
 stmt.setObject(i + 1, params[i]);
 }

 int filasAfectadas = stmt.executeUpdate();
 LOGGER.log(Level.INFO, "Query ejecutado correctamente. Filas afectadas: {0}", filasAfectadas);
 return filasAfectadas;
 } catch (SQLException e) {
 String error = "Error al ejecutar query UPDATE/INSERT/DELETE: " + e.getMessage() + "\nSQL: " + sql;
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Actualizaci\u00f3n", "No se pudo ejecutar la actualizaci\u00f3n: " + e.getMessage());
 return -1;
 }
 }

  // Validate user credentials and return user info
  public Optional<Usuario> getUsuarioPorCredenciales(String usuario, String contrasena) {

  try (Connection conn = getConnection();
  PreparedStatement stmt = conn.prepareStatement(SQL_VALIDAR_USUARIO)) {

  stmt.setString(1, usuario);
  stmt.setString(2, contrasena);

  try (ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) {
  return Optional.of(new Usuario(
  rs.getInt("id_usuario"),
  rs.getString("nombre"),
  rs.getString("perfil")
  ));
  }
  }
  } catch (SQLException e) {
  String error = "Error al validar credenciales: " + e.getMessage();
  LOGGER.log(Level.INFO, "Base de datos no disponible, usando validación offline: {0}", error);
  }

  // Fallback a validación offline (hardcoded) cuando la BD no está disponible
  return validarUsuarioOffline(usuario, contrasena);
  }

 // Validaci\u00f3n de usuario en modo offline
 private Optional<Usuario> validarUsuarioOffline(String usuario, String contrasena) {
 if ("admin".equals(usuario) && "admin123".equals(contrasena)) {
 return Optional.of(new Usuario(1, "Administrador", "ADMIN"));
 }
 if ("recepcion".equals(usuario) && "rec123".equals(contrasena)) {
 return Optional.of(new Usuario(2, "Recepción", "RECEPCION"));
 }
 if ("planificador".equals(usuario) && "plan123".equals(contrasena)) {
 return Optional.of(new Usuario(3, "Planificador", "PLANIFICADOR"));
 }
 if ("almacen".equals(usuario) && "alm123".equals(contrasena)) {
 return Optional.of(new Usuario(4, "Almacén", "ALMACEN"));
 }
 if ("produccion".equals(usuario) && "prod123".equals(contrasena)) {
 return Optional.of(new Usuario(5, "Producción", "PRODUCCION"));
 }
 if ("decoracion".equals(usuario) && "dec123".equals(contrasena)) {
 return Optional.of(new Usuario(6, "Decoración", "DECORACION"));
 }
 if ("contabilidad".equals(usuario) && "cont123".equals(contrasena)) {
 return Optional.of(new Usuario(7, "Contabilidad", "CONTABILIDAD"));
 }
 if ("repartidor".equals(usuario) && "rep123".equals(contrasena)) {
 return Optional.of(new Usuario(8, "Repartidor", "REPARTIDOR"));
 }
 if ("rrhh".equals(usuario) && "rrhh123".equals(contrasena)) {
 return Optional.of(new Usuario(9, "RRHH", "RRHH"));
 }
 if ("auditor".equals(usuario) && "aud123".equals(contrasena)) {
 return Optional.of(new Usuario(10, "Auditor", "AUDITOR"));
 }
 if ("cliente".equals(usuario) && "cli123".equals(contrasena)) {
 return Optional.of(new Usuario(11, "Cliente", "CLIENTE"));
 }
 
 return Optional.empty();
 }

 // Get all clients
 public List<Cliente> obtenerClientes() {
 List<Cliente> clientes = new ArrayList<>();

 try (Connection conn = getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_CLIENTES);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 Cliente cliente = new Cliente(
 rs.getInt("id_cliente"),
 rs.getString("nombre"),
 rs.getString("telefono"),
 rs.getString("email"),
 rs.getString("direccion"),
 rs.getString("usuario"),
 "", // contrasena - no se carga por seguridad
 rs.getString("estado")
 );
 clientes.add(cliente);
 }

 LOGGER.log(Level.INFO, "Clientes cargados: {0}", clientes.size());
 } catch (SQLException e) {
 String error = "Error al obtener clientes: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Base de Datos", "No se pudieron cargar los clientes: " + e.getMessage());
 }

 return clientes;
 }

 // Insert new client and return generated ID
 public OptionalInt insertarCliente(Cliente cliente) {
 if (OFFLINE_MODE) {
 LOGGER.log(Level.INFO, "Modo OFFLINE: Simulando registro de cliente");
 return insertarClienteOffline(cliente);
 }

 try (Connection conn = getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_CLIENTE, Statement.RETURN_GENERATED_KEYS)) {

 stmt.setString(1, cliente.getNombre());
 stmt.setString(2, cliente.getTelefono());
 stmt.setString(3, cliente.getEmail());
 stmt.setString(4, cliente.getDireccion());
 stmt.setString(5, cliente.getUsuario());
 stmt.setString(6, cliente.getContrasena());

 int filasAfectadas = stmt.executeUpdate();

 if (filasAfectadas > 0) {
 try (ResultSet rs = stmt.getGeneratedKeys()) {
 if (rs.next()) {
 int idGenerado = rs.getInt(1);
 LOGGER.log(Level.INFO, "Cliente insertado con ID: {0}", idGenerado);
 return OptionalInt.of(idGenerado);
 }
 }
 }
 } catch (SQLException e) {
 String error = "Error al insertar cliente: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Base de Datos", "No se pudo insertar el cliente: " + e.getMessage());
 }

 return OptionalInt.empty();
 }

 // Inserci\u00f3n de cliente en modo offline
 private OptionalInt insertarClienteOffline(Cliente cliente) {
 // Simular ID generado
 int idSimulado = (int) (Math.random() * 1000) + 100;
 LOGGER.log(Level.INFO, "Modo OFFLINE: Cliente registrado localmente con ID simulado: {0}", idSimulado);
 return OptionalInt.of(idSimulado);
 }

 // Update client data
 public boolean actualizarCliente(Cliente cliente) {
 try (Connection conn = getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_CLIENTE)) {

 stmt.setString(1, cliente.getNombre());
 stmt.setString(2, cliente.getTelefono());
 stmt.setString(3, cliente.getEmail());
 stmt.setString(4, cliente.getDireccion());
 stmt.setInt(5, cliente.getId());

 int filasAfectadas = stmt.executeUpdate();

 if (filasAfectadas > 0) {
 LOGGER.log(Level.INFO, "Cliente actualizado correctamente");
 return true;
 }
 } catch (SQLException e) {
 String error = "Error al actualizar cliente: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Base de Datos", "No se pudo actualizar el cliente: " + e.getMessage());
 }

 return false;
 }

 // Delete client (only if no orders exist)
 public boolean eliminarCliente(int id) {
 try (Connection conn = getConnection();
 PreparedStatement checkStmt = conn.prepareStatement(SQL_VERIFICAR_PEDIDOS_CLIENTE)) {

 checkStmt.setInt(1, id);

 try (ResultSet rs = checkStmt.executeQuery()) {
 if (rs.next()) {
 int totalPedidos = rs.getInt("total");
 if (totalPedidos > 0) {
 mostrarAlertaError("No se puede eliminar",
 "El cliente tiene " + totalPedidos + " pedidos asociados. No se puede eliminar.");
 return false;
 }
 }
 }

 try (PreparedStatement deleteStmt = conn.prepareStatement(SQL_ELIMINAR_CLIENTE)) {
 deleteStmt.setInt(1, id);
 int filasAfectadas = deleteStmt.executeUpdate();

 if (filasAfectadas > 0) {
 LOGGER.log(Level.INFO, "Cliente eliminado correctamente");
 return true;
 }
 }
 } catch (SQLException e) {
 String error = "Error al eliminar cliente: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Base de Datos", "No se pudo eliminar el cliente: " + e.getMessage());
 }

 return false;
 }

 // Log error to file
 private void logError(String error) {
 try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
 String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
 writer.println("[" + timestamp + "] " + error);
 writer.flush();
 } catch (IOException e) {
 LOGGER.log(Level.SEVERE, "Error al escribir en el archivo de log: {0}", e.getMessage());
 }
 }

 // Show error alert
 private void mostrarAlertaError(String titulo, String mensaje) {
 try {
 Alert alert = new Alert(Alert.AlertType.ERROR);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al mostrar alerta: {0}", e.getMessage());
 }
 }

 // Test database connection
 public boolean testConnection() {
 if (OFFLINE_MODE) {
 LOGGER.log(Level.INFO, "Modo OFFLINE: Prueba de conexi\u00f3n omitida");
 return false;
 }
 boolean isConnected = false;
 try {
 Connection testConn = getConnection();
 if (testConn != null && !testConn.isClosed()) {
 isConnected = testConn.isValid(CONNECTION_TIMEOUT_SECONDS);
 if (isConnected) {
 LOGGER.log(Level.INFO, "Prueba de conexi\u00f3n exitosa a la base de datos Reposteria");
 } else {
 LOGGER.log(Level.WARNING, "La conexi\u00f3n no es v\u00e1lida");
 }
 }
 } catch (SQLException e) {
 String error = "Error en la prueba de conexi\u00f3n: " + e.getMessage();
 LOGGER.log(Level.SEVERE, error);
 logError(error);
 mostrarAlertaError("Error de Conexi\u00f3n", "No se pudo conectar a la base de datos:\n" + e.getMessage() + 
 "\n\nVerifique:\n1. Servidor: DESKTOP-6EJ5FRR\\JOBETTE\n2. Base de datos: Reposteria\n3. Usuario y contrase\u00f1a");
 }
 return isConnected;
 }

 // Public method to get connection info for debugging
 public String getConnectionInfo() {
 if (OFFLINE_MODE) {
 return "Modo OFFLINE activado. No hay conexi\u00f3n a base de datos.";
 }
 return String.format("Servidor: DESKTOP-6EJ5FRR\\JOBETTE\nBase de datos: Reposteria\nUsuario: %s", USER);
 }

 // Method to get connection status
 public boolean isConnectionActive() {
 if (OFFLINE_MODE) {
 return false;
 }
 try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
 return conn.isValid(CONNECTION_TIMEOUT_SECONDS);
 } catch (SQLException e) {
 return false;
 }
 }

 /**
 * CLASE TRADICIONAL para Usuario (NO Record)
 */
 public static class Usuario {
 private int id;
 private String nombre;
 private String perfil;

 public Usuario(int id, String nombre, String perfil) {
 this.id = id;
 this.nombre = nombre;
 this.perfil = perfil;
 }

 public int getId() { return id; }
 public String getNombre() { return nombre; }
 public String getPerfil() { return perfil; }

 public boolean esAdmin() { return "ADMIN".equalsIgnoreCase(perfil); }
 public boolean esEmpleado() { return "EMPLEADO".equalsIgnoreCase(perfil); }
 public boolean esCliente() { return "CLIENTE".equalsIgnoreCase(perfil); }
 }

 /**
 * CLASE TRADICIONAL para Cliente (NO Record)
 */
 public static class Cliente {
 private int id;
 private String nombre;
 private String telefono;
 private String email;
 private String direccion;
 private String usuario;
 private String contrasena;
 private String estado;

 // Constructor completo
 public Cliente(int id, String nombre, String telefono, String email, String direccion,
 String usuario, String contrasena, String estado) {
 this.id = id;
 this.nombre = nombre;
 this.telefono = telefono;
 this.email = email;
 this.direccion = direccion;
 this.usuario = usuario;
 this.contrasena = contrasena;
 this.estado = estado;
 }

 // Constructor para nuevo cliente (sin ID)
 public Cliente(String nombre, String telefono, String email, String direccion,
 String usuario, String contrasena) {
 this(0, nombre, telefono, email, direccion, usuario, contrasena, "Activo");
 }

 // Getters
 public int getId() { return id; }
 public String getNombre() { return nombre; }
 public String getTelefono() { return telefono; }
 public String getEmail() { return email; }
 public String getDireccion() { return direccion; }
 public String getUsuario() { return usuario; }
 public String getContrasena() { return contrasena; }
 public String getEstado() { return estado; }

 // Setters
 public void setId(int id) { this.id = id; }
 public void setNombre(String nombre) { this.nombre = nombre; }
 public void setTelefono(String telefono) { this.telefono = telefono; }
 public void setEmail(String email) { this.email = email; }
 public void setDireccion(String direccion) { this.direccion = direccion; }
 public void setUsuario(String usuario) { this.usuario = usuario; }
 public void setContrasena(String contrasena) { this.contrasena = contrasena; }
 public void setEstado(String estado) { this.estado = estado; }

 // M\u00e9todos de conveniencia
 public boolean isActivo() { return "Activo".equalsIgnoreCase(estado); }
 public boolean isInactivo() { return "Inactivo".equalsIgnoreCase(estado); }
 }
}
