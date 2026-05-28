package com.example.demo.dao;

import com.example.demo.model.CompraHistorial.CompraDetalle;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompraDetalleDAO {

    private static final Logger LOGGER = Logger.getLogger(CompraDetalleDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public CompraDetalleDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<CompraDetalle> listarPorCompra(int idCompra) {
        List<CompraDetalle> list = new ArrayList<>();
        String sql = "SELECT cd.cantidad, cd.precio_unitario, cd.descuento, cd.subtotal, p.nombre AS producto, '' as unidad " +
            "FROM compra_detalles cd INNER JOIN productos p ON cd.id_producto = p.id_producto WHERE cd.id_compra = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCompra);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new CompraDetalle(
                        rs.getString("producto"),
                        rs.getDouble("cantidad"),
                        rs.getString("unidad"),
                        rs.getDouble("precio_unitario"),
                        rs.getDouble("descuento"),
                        rs.getDouble("subtotal")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar detalles de compra: {0}", e.getMessage());
        }
        return list;
    }

    public boolean insertar(int idCompra, int idProducto, double cantidad, double precioUnitario, double descuento, double subtotal) {
        String sql = "INSERT INTO compra_detalles (id_compra, id_producto, cantidad, precio_unitario, descuento, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCompra);
            stmt.setInt(2, idProducto);
            stmt.setDouble(3, cantidad);
            stmt.setDouble(4, precioUnitario);
            stmt.setDouble(5, descuento);
            stmt.setDouble(6, subtotal);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar detalle de compra: {0}", e.getMessage());
            return false;
        }
    }

    public int contarPorCompra(int idCompra) {
        String sql = "SELECT COUNT(*) FROM compra_detalles WHERE id_compra = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCompra);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar detalles: {0}", e.getMessage());
        }
        return 0;
    }
}
