package com.example.demo.dao;

import com.example.demo.util.DatabaseConnection;
import com.example.demo.model.EntregaHistorial;
import com.example.demo.model.PedidoPendiente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntregaDAO {

    private static final Logger LOGGER = Logger.getLogger(EntregaDAO.class.getName());
    private final DatabaseConnection dbConnection;

    private static final String SQL_PEDIDOS_PENDIENTES =
            "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
            "c.direccion, p.total, p.adelanto, " +
            "p.total - p.adelanto as saldo, " +
            "CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo " +
            "FROM pedidos p " +
            "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
            "WHERE p.estado = 'Listo para entregar' " +
            "AND (p.total - p.adelanto) > 0 " +
            "ORDER BY p.fecha_entrega";

    private static final String SQL_HISTORIAL_ENTREGAS =
            "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
            "CAST(p.fecha_entrega AS DATE) as fecha_entrega, " +
            "CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo, " +
            "p.total, " +
            "COALESCE((SELECT SUM(monto) FROM pagos WHERE id_pedido = p.id_pedido), 0) as pagado, " +
            "(SELECT TOP 1 metodo_pago FROM pagos WHERE id_pedido = p.id_pedido ORDER BY fecha_pago DESC) as metodo_pago " +
            "FROM pedidos p " +
            "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
            "WHERE p.estado = 'Entregado' " +
            "ORDER BY p.fecha_entrega DESC";

    public EntregaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<PedidoPendiente> obtenerPedidosPendientes() {
        List<PedidoPendiente> pendientes = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_PENDIENTES);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                pendientes.add(new PedidoPendiente(
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("direccion"),
                        rs.getDouble("total"),
                        rs.getDouble("adelanto"),
                        rs.getDouble("saldo"),
                        rs.getString("tipo")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar pedidos pendientes: {0}", e.getMessage());
        }
        return pendientes;
    }

    public List<EntregaHistorial> obtenerHistorialEntregas() {
        List<EntregaHistorial> historial = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_HISTORIAL_ENTREGAS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                historial.add(new EntregaHistorial(
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("fecha_entrega"),
                        rs.getString("tipo"),
                        rs.getDouble("total"),
                        rs.getDouble("pagado"),
                        rs.getString("metodo_pago") != null ? rs.getString("metodo_pago") : "Efectivo"
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar historial: {0}", e.getMessage());
        }
        return historial;
    }

    public List<EntregaHistorial> aplicarFiltrosHistorial(String consultaSQL, Object... parametros) {
        List<EntregaHistorial> historial = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(consultaSQL)) {

            for (int i = 0; i < parametros.length; i++) {
                stmt.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(new EntregaHistorial(
                            rs.getInt("id_pedido"),
                            rs.getString("nombre_cliente"),
                            rs.getString("fecha_entrega"),
                            rs.getString("tipo"),
                            rs.getDouble("total"),
                            rs.getDouble("pagado"),
                            rs.getString("metodo_pago") != null ? rs.getString("metodo_pago") : "Efectivo"
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al aplicar filtros en historial: {0}", e.getMessage());
        }
        return historial;
    }
}
