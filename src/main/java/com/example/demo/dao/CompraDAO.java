package com.example.demo.dao;

import com.example.demo.model.CompraHistorial;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompraDAO {

    private static final Logger LOGGER = Logger.getLogger(CompraDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public CompraDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<CompraHistorial> listarTodas() {
        List<CompraHistorial> list = new ArrayList<>();
        String sql = "SELECT c.id_compra, p.nombre AS proveedor, c.fecha_compra, c.total, c.usuario_registra, " +
            "(SELECT COUNT(*) FROM compra_detalles cd WHERE cd.id_compra = c.id_compra) AS total_productos " +
            "FROM compras c LEFT JOIN proveedores p ON c.id_proveedor = p.id_proveedor ORDER BY c.fecha_compra DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new CompraHistorial(
                    rs.getInt("id_compra"),
                    rs.getString("proveedor"),
                    rs.getTimestamp("fecha_compra") != null ? rs.getTimestamp("fecha_compra").toString() : null,
                    rs.getDouble("total"),
                    rs.getString("usuario_registra"),
                    rs.getInt("total_productos"),
                    "Completada"
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar compras: {0}", e.getMessage());
        }
        return list;
    }

    public int insertar(int idProveedor, int usuarioRegistra, double total) {
        String sql = "INSERT INTO compras (id_proveedor, fecha_compra, usuario_registra, total) VALUES (?, GETDATE(), ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idProveedor);
            stmt.setInt(2, usuarioRegistra);
            stmt.setDouble(3, total);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar compra: {0}", e.getMessage());
        }
        return -1;
    }

    public int contar() {
        String sql = "SELECT COUNT(*) FROM compras";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar compras: {0}", e.getMessage());
        }
        return 0;
    }
}
