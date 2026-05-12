package com.example.demo.dao;

import com.example.demo.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MantenimientoDAO {

    private static final Logger LOGGER = Logger.getLogger(MantenimientoDAO.class.getName());
    private final DatabaseConnection dbConnection;

    private static final String SQL_REGISTRAR_MANTENIMIENTO =
            "INSERT INTO mantenimiento (equipo, descripcion, tecnico, fecha_mantenimiento, proximo_mantenimiento) VALUES (?, ?, ?, ?, ?)";

    public MantenimientoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public boolean registrarMantenimiento(String equipo, String descripcion, String tecnico, java.time.LocalDate fechaMantenimiento, java.time.LocalDate proximoMantenimiento) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_MANTENIMIENTO)) {
            
            stmt.setString(1, equipo);
            stmt.setString(2, descripcion);
            stmt.setString(3, tecnico);
            stmt.setDate(4, java.sql.Date.valueOf(fechaMantenimiento));
            stmt.setDate(5, proximoMantenimiento != null ? java.sql.Date.valueOf(proximoMantenimiento) : null);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar mantenimiento: {0}", e.getMessage());
            return false;
        }
    }
}
