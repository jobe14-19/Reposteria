package com.example.demo.dao;

import com.example.demo.model.Maquina;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaquinaDAO {

    private static final Logger LOGGER = Logger.getLogger(MaquinaDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public MaquinaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Maquina> listarTodas() {
        List<Maquina> list = new ArrayList<>();
        String sql = "SELECT id_maquina, nombre, utilidad, estado, ultimo_mantenimiento, proximo_mantenimiento FROM maquinas ORDER BY nombre";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar maquinas: {0}", e.getMessage());
        }
        return list;
    }

    public Maquina obtenerPorId(int id) {
        String sql = "SELECT id_maquina, nombre, utilidad, estado, ultimo_mantenimiento, proximo_mantenimiento FROM maquinas WHERE id_maquina = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener maquina: {0}", e.getMessage());
        }
        return null;
    }

    public boolean actualizarEstado(int id, String estado) {
        String sql = "UPDATE maquinas SET estado = ? WHERE id_maquina = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar estado de maquina: {0}", e.getMessage());
            return false;
        }
    }

    public int contar() {
        String sql = "SELECT COUNT(*) FROM maquinas";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar maquinas: {0}", e.getMessage());
        }
        return 0;
    }

    private Maquina mapear(ResultSet rs) throws SQLException {
        return new Maquina(
            rs.getInt("id_maquina"),
            rs.getString("nombre"),
            rs.getString("utilidad"),
            rs.getString("estado"),
            rs.getDate("ultimo_mantenimiento") != null ? rs.getDate("ultimo_mantenimiento").toString() : null,
            rs.getDate("proximo_mantenimiento") != null ? rs.getDate("proximo_mantenimiento").toString() : null
        );
    }
}
