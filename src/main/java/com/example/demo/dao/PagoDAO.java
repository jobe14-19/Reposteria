package com.example.demo.dao;

import com.example.demo.model.Pago;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PagoDAO {

    private static final Logger LOGGER = Logger.getLogger(PagoDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public PagoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Pago> listarPorPedido(int idPedido) {
        List<Pago> pagos = new ArrayList<>();
        String sql = "SELECT id_pago, id_pedido, monto, fecha_pago, metodo_pago, referencia, estado FROM pagos WHERE id_pedido = ? ORDER BY fecha_pago DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar pagos por pedido: {0}", e.getMessage());
        }
        return pagos;
    }

    public double obtenerTotalPagado(int idPedido) {
        String sql = "SELECT ISNULL(SUM(monto), 0) FROM pagos WHERE id_pedido = ? AND estado = 'Pagado'";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener total pagado: {0}", e.getMessage());
        }
        return 0;
    }

    public int insertar(Pago pago) {
        String sql = "INSERT INTO pagos (id_pedido, monto, fecha_pago, metodo_pago, referencia, estado) VALUES (?, ?, GETDATE(), ?, ?, 'Pagado')";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pago.getIdPedido());
            stmt.setDouble(2, pago.getMonto());
            stmt.setString(3, pago.getMetodoPago() != null ? pago.getMetodoPago() : "Efectivo");
            stmt.setString(4, pago.getReferencia());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar pago: {0}", e.getMessage());
        }
        return -1;
    }

    private Pago mapear(ResultSet rs) throws SQLException {
        Pago p = new Pago();
        p.setIdPago(rs.getInt("id_pago"));
        p.setIdPedido(rs.getInt("id_pedido"));
        p.setMonto(rs.getDouble("monto"));
        p.setFechaPago(rs.getTimestamp("fecha_pago") != null ? rs.getTimestamp("fecha_pago").toString() : null);
        p.setMetodoPago(rs.getString("metodo_pago"));
        p.setReferencia(rs.getString("referencia"));
        p.setEstado(rs.getString("estado"));
        return p;
    }
}
