package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;
import com.example.demo.model.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PedidoDAO {

 private static final Logger LOGGER = Logger.getLogger(PedidoDAO.class.getName());
 private final DatabaseConnection dbConnection;

  private static final String SQL_CARGAR_PEDIDOS =
  "SELECT p.id_pedido, ISNULL(c.nombre + ' ' + c.apellido, p.username) as nombre_cliente, FORMAT(p.fecha_pedido, 'yyyy-MM-dd') as fecha_pedido, FORMAT(p.fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, ISNULL(pr.nombre, p.producto) as producto, p.libras, p.total, p.adelanto, p.estado, ISNULL(p.tipo_pago, 'Efectivo') as tipo_pago, ISNULL(p.estado_pago, 'Pendiente') as estado_pago FROM pedidos p LEFT JOIN clientes c ON p.id_cliente = c.id_cliente LEFT JOIN productos pr ON p.id_producto = pr.id_producto ORDER BY p.fecha_pedido DESC";

  private static final String SQL_BUSCAR_PEDIDOS =
  "SELECT p.id_pedido, ISNULL(c.nombre + ' ' + c.apellido, p.username) as nombre_cliente, FORMAT(p.fecha_pedido, 'yyyy-MM-dd') as fecha_pedido, FORMAT(p.fecha_entrega, 'yyyy-MM-dd') as fecha_entrega, ISNULL(pr.nombre, p.producto) as producto, p.libras, p.total, p.adelanto, p.estado, ISNULL(p.tipo_pago, 'Efectivo') as tipo_pago, ISNULL(p.estado_pago, 'Pendiente') as estado_pago FROM pedidos p LEFT JOIN clientes c ON p.id_cliente = c.id_cliente LEFT JOIN productos pr ON p.id_producto = pr.id_producto WHERE CAST(p.id_pedido AS VARCHAR) LIKE ? OR ISNULL(c.nombre, p.username) LIKE ? OR ISNULL(c.apellido, '') LIKE ? OR ISNULL(pr.nombre, p.producto) LIKE ? ORDER BY p.fecha_pedido DESC";

 public PedidoDAO() {
 this.dbConnection = DatabaseConnection.getInstance();
 }

 public List<Pedido> obtenerTodosLosPedidos() {
 List<Pedido> pedidos = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_PEDIDOS);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 pedidos.add(mapearPedido(rs));
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar pedidos: {0}", e.getMessage());
 }
 return pedidos;
 }

 public List<Pedido> buscarPedidos(String textoBusqueda) {
 List<Pedido> pedidos = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_PEDIDOS)) {

 String busqueda = "%" + textoBusqueda + "%";
 stmt.setString(1, busqueda);
 stmt.setString(2, busqueda);
 stmt.setString(3, busqueda);
 stmt.setString(4, busqueda);

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 pedidos.add(mapearPedido(rs));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al buscar pedidos: {0}", e.getMessage());
 }
 return pedidos;
 }

 public List<Pedido> aplicarFiltros(String consultaSQL, Object... parametros) {
 List<Pedido> pedidos = new ArrayList<>();
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(consultaSQL)) {

 for (int i = 0; i < parametros.length; i++) {
 stmt.setObject(i + 1, parametros[i]);
 }

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 pedidos.add(mapearPedido(rs));
 }
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
 }
 return pedidos;
 }

    public boolean actualizarEstadoPago(int idPedido, String estadoPago) {
        String sql = "UPDATE pedidos SET estado_pago = ? WHERE id_pedido = ?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estadoPago);
            stmt.setInt(2, idPedido);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar estado_pago: {0}", e.getMessage());
            return false;
        }
    }

    public boolean actualizarAdelantoYEstadoPago(int idPedido, double adelanto, String estadoPago) {
        String sql = "UPDATE pedidos SET adelanto = ?, estado_pago = ? WHERE id_pedido = ?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, adelanto);
            stmt.setString(2, estadoPago);
            stmt.setInt(3, idPedido);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar adelanto/estado_pago: {0}", e.getMessage());
            return false;
        }
    }

    private Pedido mapearPedido(ResultSet rs) throws SQLException {
 return new Pedido(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("fecha_pedido"),
 rs.getString("fecha_entrega"),
 rs.getString("producto"),
 rs.getDouble("libras"),
 rs.getDouble("total"),
 rs.getDouble("adelanto"),
 rs.getString("estado"),
 rs.getString("tipo_pago"),
 rs.getString("estado_pago")
 );
 }
}
