package com.example.demo.dao;

import com.example.demo.model.ChecklistItem;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChecklistItemDAO {

    private static final Logger LOGGER = Logger.getLogger(ChecklistItemDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public ChecklistItemDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<ChecklistItem> listarTodas() {
        List<ChecklistItem> list = new ArrayList<>();
        String sql = "SELECT id_checklist, nombre, estado FROM checklist_items WHERE estado = 'Activo' ORDER BY nombre";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new ChecklistItem(
                    rs.getInt("id_checklist"),
                    rs.getString("nombre"),
                    rs.getString("estado")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar checklist items: {0}", e.getMessage());
        }
        return list;
    }

    public int insertar(String nombre) {
        String sql = "INSERT INTO checklist_items (nombre) VALUES (?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nombre);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar checklist item: {0}", e.getMessage());
        }
        return -1;
    }

    public boolean desactivar(int id) {
        String sql = "UPDATE checklist_items SET estado = 'Inactivo' WHERE id_checklist = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al desactivar checklist item: {0}", e.getMessage());
            return false;
        }
    }
}
