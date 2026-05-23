package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;
import com.example.demo.model.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PersonalDAO {

 private static final Logger LOGGER = Logger.getLogger(PersonalDAO.class.getName());
 private final DatabaseConnection dbConnection;

 private static final String SQL_CARGAR_EMPLEADOS =
 "SELECT e.id_empleado, e.nombre, e.cedula, e.telefono, e.area, e.estado, " + 
 "CASE WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) >= 3 THEN 'Completo' " + 
 "WHEN (SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = e.id_empleado) > 0 THEN 'Parcial' " + 
 "ELSE 'Pendiente' END as capacitacion " + 
  "FROM empleados e " + 
  "WHERE e.estado = 'Activo' " + 
  "ORDER BY e.nombre";

 private static final String SQL_ELIMINAR_EMPLEADO = "UPDATE empleados SET estado = 'Inactivo' WHERE id_empleado = ?";

 public PersonalDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public List<Empleado> obtenerTodosLosEmpleados() {
 List<Empleado> empleados = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_EMPLEADOS);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 empleados.add(new Empleado(
 rs.getInt("id_empleado"),
 rs.getString("nombre"),
 rs.getString("cedula"),
 rs.getString("telefono"),
 rs.getString("area"),
 rs.getString("estado"),
 rs.getString("capacitacion")
 ));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar empleados: {0}", e.getMessage());
 }
 return empleados;
 }

 public List<Empleado> aplicarFiltrosYBusqueda(String consultaSQL, Object... parametros) {
 List<Empleado> empleados = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(consultaSQL)) {

 for (int i = 0; i < parametros.length; i++) {
 stmt.setObject(i + 1, parametros[i]);
 }

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 empleados.add(new Empleado(
 rs.getInt("id_empleado"),
 rs.getString("nombre"),
 rs.getString("cedula"),
 rs.getString("telefono"),
 rs.getString("area"),
 rs.getString("estado"),
 rs.getString("capacitacion")
 ));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al aplicar filtros o búsqueda de empleados: {0}", e.getMessage());
 }
 return empleados;
 }

 public boolean eliminarEmpleado(int idEmpleado) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_EMPLEADO)) {
 stmt.setInt(1, idEmpleado);
 return stmt.executeUpdate() > 0;
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al eliminar empleado: {0}", e.getMessage());
 return false;
 }
 }
}
