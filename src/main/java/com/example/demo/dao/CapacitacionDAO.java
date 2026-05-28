package com.example.demo.dao;

import com.example.demo.model.Capacitacion;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CapacitacionDAO {

    private static final Logger LOGGER = Logger.getLogger(CapacitacionDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public CapacitacionDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Capacitacion> listarTodas() {
        List<Capacitacion> list = new ArrayList<>();
        String sql = "SELECT id_capacitacion, id_empleado, tema, fecha, duracion, capacitador, usuario_registra FROM capacitaciones ORDER BY fecha DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar capacitaciones: {0}", e.getMessage());
        }
        return list;
    }

    public int insertar(Capacitacion c) {
        String sql = "INSERT INTO capacitaciones (id_empleado, tema, fecha, duracion, capacitador, usuario_registra) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, c.getIdEmpleado());
            stmt.setString(2, c.getTema());
            stmt.setString(3, c.getFecha());
            stmt.setDouble(4, c.getDuracion());
            stmt.setString(5, c.getCapacitador());
            stmt.setInt(6, c.getUsuarioRegistra());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar capacitacion: {0}", e.getMessage());
        }
        return -1;
    }

    public int contarPorEmpleado(int idEmpleado) {
        String sql = "SELECT COUNT(*) FROM capacitaciones WHERE id_empleado = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpleado);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar capacitaciones por empleado: {0}", e.getMessage());
        }
        return 0;
    }

    private Capacitacion mapear(ResultSet rs) throws SQLException {
        return new Capacitacion(
            rs.getInt("id_capacitacion"),
            rs.getInt("id_empleado"),
            rs.getString("tema"),
            rs.getDate("fecha") != null ? rs.getDate("fecha").toString() : null,
            rs.getDouble("duracion"),
            rs.getString("capacitador"),
            rs.getInt("usuario_registra")
        );
    }
}
