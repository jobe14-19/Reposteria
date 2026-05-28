package com.example.demo.dao;

import com.example.demo.model.Actividad;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActividadDAO {

    private static final Logger LOGGER = Logger.getLogger(ActividadDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public ActividadDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Actividad> listarRecientes(int limite) {
        List<Actividad> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id_actividad, fecha_hora, usuario, accion, detalle FROM actividad ORDER BY fecha_hora DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar actividad reciente: {0}", e.getMessage());
        }
        return list;
    }

    public List<Actividad> listarPorUsuario(String usuario) {
        List<Actividad> list = new ArrayList<>();
        String sql = "SELECT id_actividad, fecha_hora, usuario, accion, detalle FROM actividad WHERE usuario = ? ORDER BY fecha_hora DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar actividad por usuario: {0}", e.getMessage());
        }
        return list;
    }

    public int insertar(Actividad a) {
        String sql = "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, a.getUsuario());
            stmt.setString(2, a.getAccion());
            stmt.setString(3, a.getDetalle());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar actividad: {0}", e.getMessage());
        }
        return -1;
    }

    private Actividad mapear(ResultSet rs) throws SQLException {
        return new Actividad(
            rs.getInt("id_actividad"),
            rs.getTimestamp("fecha_hora") != null ? rs.getTimestamp("fecha_hora").toString() : null,
            rs.getString("usuario"),
            rs.getString("accion"),
            rs.getString("detalle")
        );
    }
}
