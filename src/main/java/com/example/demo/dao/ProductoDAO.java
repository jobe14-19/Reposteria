package com.example.demo.dao;

import com.example.demo.model.Producto;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductoDAO {

    private static final Logger LOGGER = Logger.getLogger(ProductoDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public ProductoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        asegurarTablas();
    }

    private void asegurarTablas() {
        String alterEstado = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'estado') ALTER TABLE productos ADD estado NVARCHAR(10) DEFAULT 'Activo'";
        try (Connection conn = dbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            try { stmt.execute(alterEstado); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET estado = 'Activo' WHERE estado IS NULL"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo verificar tabla productos: {0}", e.getMessage());
        }
    }

    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.precio_base, p.precio_unitario, p.costo_disenio, "
                   + "p.descripcion, p.estado, "
                   + "(SELECT COUNT(*) FROM recetas r WHERE r.id_producto = p.id_producto AND r.estado = 'Activo') as total_recetas "
                   + "FROM productos p WHERE p.estado = 'Activo' ORDER BY p.nombre";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Producto(
                    rs.getInt("id_producto"), rs.getString("nombre"),
                    rs.getDouble("precio_base"), rs.getDouble("precio_unitario"),
                    rs.getDouble("costo_disenio"), rs.getString("descripcion"),
                    rs.getString("estado"), rs.getInt("total_recetas")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar productos: {0}", e.getMessage());
        }
        return lista;
    }

    public Producto obtenerPorId(int id) {
        String sql = "SELECT id_producto, nombre, precio_base, precio_unitario, costo_disenio, descripcion, estado FROM productos WHERE id_producto = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Producto(rs.getInt("id_producto"), rs.getString("nombre"),
                        rs.getDouble("precio_base"), rs.getDouble("precio_unitario"),
                        rs.getDouble("costo_disenio"), rs.getString("descripcion"),
                        rs.getString("estado"), 0);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener producto: {0}", e.getMessage());
        }
        return null;
    }

    public int insertar(Producto p) {
        String sql = "INSERT INTO productos (nombre, precio_base, precio_unitario, costo_disenio, descripcion) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getNombre().trim());
            stmt.setDouble(2, p.getPrecioBase());
            stmt.setDouble(3, p.getPrecioUnitario());
            stmt.setDouble(4, p.getCostoDisenio());
            stmt.setString(5, p.getDescripcion().trim());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar producto: {0}", e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, precio_base = ?, precio_unitario = ?, costo_disenio = ?, descripcion = ? WHERE id_producto = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getNombre().trim());
            stmt.setDouble(2, p.getPrecioBase());
            stmt.setDouble(3, p.getPrecioUnitario());
            stmt.setDouble(4, p.getCostoDisenio());
            stmt.setString(5, p.getDescripcion().trim());
            stmt.setInt(6, p.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar producto: {0}", e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "UPDATE productos SET estado = 'Inactivo' WHERE id_producto = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar producto: {0}", e.getMessage());
            return false;
        }
    }
}
