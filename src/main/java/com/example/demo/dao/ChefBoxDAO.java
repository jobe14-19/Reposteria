package com.example.demo.dao;

import com.example.demo.model.ChefBox;
import com.example.demo.model.ChefBox.ChefBoxProducto;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChefBoxDAO {

    private static final Logger LOGGER = Logger.getLogger(ChefBoxDAO.class.getName());
    private final DatabaseConnection dbConnection;

    private static final String SQL_LISTAR = "SELECT cb.id_chef_box, cb.nombre, cb.descripcion, cb.precio, cb.disponible, FORMAT(cb.fecha_creacion, 'yyyy-MM-dd') as fecha_creacion, cb.estado, (SELECT COUNT(*) FROM chef_box_productos cbp WHERE cbp.id_chef_box = cb.id_chef_box) as total_productos FROM chefs_box cb WHERE cb.estado = 'Activo' ORDER BY cb.nombre";

    private static final String SQL_OBTENER = "SELECT id_chef_box, nombre, descripcion, precio, CASE WHEN disponible = 1 THEN 'true' ELSE 'false' END as disponible, FORMAT(fecha_creacion, 'yyyy-MM-dd') as fecha_creacion, estado FROM chefs_box WHERE id_chef_box = ?";

    private static final String SQL_PRODUCTOS_BOX = "SELECT cbp.id_producto, p.nombre as nombre_producto, cbp.cantidad, p.precio_unitario FROM chef_box_productos cbp INNER JOIN productos p ON cbp.id_producto = p.id_producto WHERE cbp.id_chef_box = ?";

    private static final String SQL_PRODUCTOS_DISPONIBLES = "SELECT id_producto, nombre, precio_unitario FROM productos WHERE estado = 'Activo' ORDER BY nombre";

    private static final String SQL_INSERTAR = "INSERT INTO chefs_box (nombre, descripcion, precio, disponible) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR = "UPDATE chefs_box SET nombre = ?, descripcion = ?, precio = ?, disponible = ?, fecha_modificacion = GETDATE() WHERE id_chef_box = ?";

    private static final String SQL_ELIMINAR = "UPDATE chefs_box SET estado = 'Inactivo' WHERE id_chef_box = ?";

    private static final String SQL_INSERTAR_PRODUCTO = "INSERT INTO chef_box_productos (id_chef_box, id_producto, cantidad) VALUES (?, ?, ?)";

    private static final String SQL_ELIMINAR_PRODUCTOS = "DELETE FROM chef_box_productos WHERE id_chef_box = ?";

    public ChefBoxDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        asegurarTablas();
    }

    private void asegurarTablas() {
        String sqlBox = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='chefs_box' AND xtype='U') CREATE TABLE chefs_box (id_chef_box INT IDENTITY(1,1) PRIMARY KEY, nombre NVARCHAR(100) NOT NULL, descripcion NVARCHAR(500), precio DECIMAL(12,2) DEFAULT 0, disponible BIT DEFAULT 1, fecha_creacion DATETIME DEFAULT GETDATE(), fecha_modificacion DATETIME, estado NVARCHAR(10) DEFAULT 'Activo')";
        String sqlProd = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='chef_box_productos' AND xtype='U') CREATE TABLE chef_box_productos (id_chef_box INT NOT NULL, id_producto INT NOT NULL, cantidad INT DEFAULT 1, PRIMARY KEY (id_chef_box, id_producto))";
        String alterBox = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('chefs_box') AND name = 'estado') ALTER TABLE chefs_box ADD estado NVARCHAR(10) DEFAULT 'Activo'";
        String alterProd = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'estado') ALTER TABLE productos ADD estado NVARCHAR(10) DEFAULT 'Activo'";
        try (Connection conn = dbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlBox);
            stmt.execute(sqlProd);
            try { stmt.execute(alterBox); } catch (SQLException ignored) {}
            try { stmt.execute(alterProd); } catch (SQLException ignored) {}
            try { stmt.execute("UPDATE productos SET estado = 'Activo' WHERE estado IS NULL"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron crear tablas Chef's Box: {0}", e.getMessage());
        }
    }

    public List<ChefBox> listarTodas() {
        List<ChefBox> lista = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new ChefBox(
                    rs.getInt("id_chef_box"),
                    rs.getString("nombre"),
                    rs.getString("descripcion"),
                    rs.getDouble("precio"),
                    rs.getBoolean("disponible"),
                    rs.getString("fecha_creacion"),
                    rs.getString("estado"),
                    rs.getInt("total_productos")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar Chef's Box: {0}", e.getMessage());
        }
        return lista;
    }

    public ChefBox obtenerPorId(int id) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChefBox box = new ChefBox(rs.getInt("id_chef_box"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getDouble("precio"),
                        Boolean.parseBoolean(rs.getString("disponible")),
                        rs.getString("fecha_creacion"), rs.getString("estado"), 0);
                    box.setProductos(obtenerProductosDeBox(id));
                    return box;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener Chef's Box: {0}", e.getMessage());
        }
        return null;
    }

    public List<ChefBoxProducto> obtenerProductosDeBox(int idChefBox) {
        List<ChefBoxProducto> lista = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTOS_BOX)) {
            stmt.setInt(1, idChefBox);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ChefBoxProducto(rs.getInt("id_producto"),
                        rs.getString("nombre_producto"), rs.getInt("cantidad"),
                        rs.getDouble("precio_unitario")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener productos de Chef's Box: {0}", e.getMessage());
        }
        return lista;
    }

    public List<ChefBoxProducto> obtenerProductosDisponibles() {
        List<ChefBoxProducto> lista = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTOS_DISPONIBLES);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new ChefBoxProducto(rs.getInt("id_producto"),
                    rs.getString("nombre"), 1, rs.getDouble("precio_unitario")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener productos disponibles: {0}", e.getMessage());
        }
        return lista;
    }

    public int insertar(ChefBox box, List<ChefBoxProducto> productos) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, box.getNombre());
                stmt.setString(2, box.getDescripcion());
                stmt.setDouble(3, box.getPrecio());
                stmt.setBoolean(4, box.isDisponible());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        insertarProductos(conn, id, productos);
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar Chef's Box: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
        return -1;
    }

    public boolean actualizar(ChefBox box, List<ChefBoxProducto> productos) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, box.getNombre());
                stmt.setString(2, box.getDescripcion());
                stmt.setDouble(3, box.getPrecio());
                stmt.setBoolean(4, box.isDisponible());
                stmt.setInt(5, box.getId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR_PRODUCTOS)) {
                stmt.setInt(1, box.getId());
                stmt.executeUpdate();
            }
            insertarProductos(conn, box.getId(), productos);
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar Chef's Box: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
        return false;
    }

    public boolean eliminar(int id) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar Chef's Box: {0}", e.getMessage());
            return false;
        }
    }

    public boolean toggleDisponible(int id, boolean disponible) {
        String sql = "UPDATE chefs_box SET disponible = ?, fecha_modificacion = GETDATE() WHERE id_chef_box = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, disponible);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cambiar disponibilidad: {0}", e.getMessage());
            return false;
        }
    }

    private void insertarProductos(Connection conn, int idChefBox, List<ChefBoxProducto> productos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PRODUCTO)) {
            for (ChefBoxProducto p : productos) {
                stmt.setInt(1, idChefBox);
                stmt.setInt(2, p.getIdProducto());
                stmt.setInt(3, p.getCantidad());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
