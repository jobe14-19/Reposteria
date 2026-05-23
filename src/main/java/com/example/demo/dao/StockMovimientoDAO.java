package com.example.demo.dao;

import com.example.demo.model.StockMovimiento;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StockMovimientoDAO {

    private static final Logger LOGGER = Logger.getLogger(StockMovimientoDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public StockMovimientoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        asegurarTabla();
    }

    private void asegurarTabla() {
        String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='stock_movimientos' AND xtype='U') "
            + "CREATE TABLE stock_movimientos ("
            + "id_movimiento INT IDENTITY(1,1) PRIMARY KEY, "
            + "id_ingrediente INT NOT NULL, "
            + "tipo_movimiento NVARCHAR(20) NOT NULL, "
            + "cantidad DECIMAL(12,2) NOT NULL, "
            + "stock_anterior DECIMAL(12,2) NOT NULL DEFAULT 0, "
            + "stock_nuevo DECIMAL(12,2) NOT NULL DEFAULT 0, "
            + "motivo NVARCHAR(200), "
            + "referencia_tipo NVARCHAR(50), "
            + "referencia_id INT, "
            + "usuario_registra NVARCHAR(100) NOT NULL, "
            + "fecha_hora DATETIME NOT NULL DEFAULT GETDATE()"
            + ")";
        try (Connection conn = dbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear tabla stock_movimientos: {0}", e.getMessage());
        }
    }

    public boolean registrarEntrada(int idIngrediente, String nombreIngrediente, double cantidad,
                                     String motivo, String referenciaTipo, int referenciaId, String usuario) {
        return registrarMovimiento(idIngrediente, nombreIngrediente, "ENTRADA", cantidad, motivo, referenciaTipo, referenciaId, usuario);
    }

    public boolean registrarSalida(int idIngrediente, String nombreIngrediente, double cantidad,
                                    String motivo, String referenciaTipo, int referenciaId, String usuario) {
        return registrarMovimiento(idIngrediente, nombreIngrediente, "SALIDA", cantidad, motivo, referenciaTipo, referenciaId, usuario);
    }

    public boolean registrarAjuste(int idIngrediente, String nombreIngrediente, double cantidad,
                                    String motivo, String usuario) {
        return registrarMovimiento(idIngrediente, nombreIngrediente, "AJUSTE", cantidad, motivo, null, 0, usuario);
    }

    private boolean registrarMovimiento(int idIngrediente, String nombreIngrediente, String tipo,
                                         double cantidad, String motivo, String referenciaTipo,
                                         int referenciaId, String usuario) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            double stockAnterior = obtenerStockActual(conn, idIngrediente);
            double cantidadReal = "SALIDA".equals(tipo) ? -Math.abs(cantidad) : Math.abs(cantidad);
            double stockNuevo = stockAnterior + cantidadReal;

            if (stockNuevo < 0) {
                LOGGER.log(Level.WARNING, "Stock insuficiente: {0} (actual: {1}, necesario: {2})",
                    new Object[]{nombreIngrediente, stockAnterior, cantidad});
                conn.rollback();
                return false;
            }

            actualizarStockIngrediente(conn, idIngrediente, stockNuevo);

            String sql = "INSERT INTO stock_movimientos (id_ingrediente, tipo_movimiento, cantidad, "
                + "stock_anterior, stock_nuevo, motivo, referencia_tipo, referencia_id, usuario_registra, fecha_hora) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idIngrediente);
                stmt.setString(2, tipo);
                stmt.setDouble(3, cantidadReal);
                stmt.setDouble(4, stockAnterior);
                stmt.setDouble(5, stockNuevo);
                stmt.setString(6, motivo);
                stmt.setString(7, referenciaTipo);
                stmt.setInt(8, referenciaId);
                stmt.setString(9, usuario);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar movimiento stock: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private double obtenerStockActual(Connection conn, int idIngrediente) throws SQLException {
        String sql = "SELECT stock_actual FROM ingredientes WHERE id_ingrediente = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idIngrediente);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("stock_actual") : 0;
            }
        }
    }

    private void actualizarStockIngrediente(Connection conn, int idIngrediente, double nuevoStock) throws SQLException {
        String sql = "UPDATE ingredientes SET stock_actual = ? WHERE id_ingrediente = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, nuevoStock);
            stmt.setInt(2, idIngrediente);
            stmt.executeUpdate();
        }
    }

    public double getStockActual(int idIngrediente) {
        try (Connection conn = dbConnection.getConnection()) {
            return obtenerStockActual(conn, idIngrediente);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener stock: {0}", e.getMessage());
            return 0;
        }
    }

    public boolean validarStockSuficiente(int idIngrediente, double cantidadRequerida) {
        double actual = getStockActual(idIngrediente);
        return actual >= cantidadRequerida;
    }

    public int listarMovimientosCount(String filtro) {
        String sql = "SELECT COUNT(*) FROM stock_movimientos m "
            + "LEFT JOIN ingredientes i ON m.id_ingrediente = i.id_ingrediente "
            + "WHERE (i.nombre LIKE ? OR m.tipo_movimiento LIKE ? OR m.usuario_registra LIKE ? OR m.motivo LIKE ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String like = "%" + (filtro != null ? filtro : "") + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            stmt.setString(3, like);
            stmt.setString(4, like);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al contar movimientos: {0}", e.getMessage());
            return 0;
        }
    }

    public List<StockMovimiento> listarMovimientos(String filtro, int offset, int limit) {
        List<StockMovimiento> lista = new ArrayList<>();
        String sql = "SELECT m.id_movimiento, m.id_ingrediente, i.nombre as nombre_ingrediente, "
            + "m.tipo_movimiento, m.cantidad, m.stock_anterior, m.stock_nuevo, "
            + "m.motivo, m.referencia_tipo, m.referencia_id, m.usuario_registra, m.fecha_hora "
            + "FROM stock_movimientos m "
            + "LEFT JOIN ingredientes i ON m.id_ingrediente = i.id_ingrediente "
            + "WHERE (i.nombre LIKE ? OR m.tipo_movimiento LIKE ? OR m.usuario_registra LIKE ? OR m.motivo LIKE ?) "
            + "ORDER BY m.fecha_hora DESC "
            + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String like = "%" + (filtro != null ? filtro : "") + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            stmt.setString(3, like);
            stmt.setString(4, like);
            stmt.setInt(5, offset);
            stmt.setInt(6, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovimiento m = new StockMovimiento();
                    m.setId(rs.getInt("id_movimiento"));
                    m.setIdIngrediente(rs.getInt("id_ingrediente"));
                    m.setNombreIngrediente(rs.getString("nombre_ingrediente"));
                    m.setTipoMovimiento(rs.getString("tipo_movimiento"));
                    m.setCantidad(rs.getDouble("cantidad"));
                    m.setStockAnterior(rs.getDouble("stock_anterior"));
                    m.setStockNuevo(rs.getDouble("stock_nuevo"));
                    m.setMotivo(rs.getString("motivo"));
                    m.setReferenciaTipo(rs.getString("referencia_tipo"));
                    m.setReferenciaId(rs.getInt("referencia_id"));
                    m.setUsuarioRegistra(rs.getString("usuario_registra"));
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) m.setFechaHora(ts.toLocalDateTime());
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar movimientos: {0}", e.getMessage());
        }
        return lista;
    }

    public List<StockMovimiento> listarMovimientosRecientes(int limite) {
        return listarMovimientos("", 0, limite);
    }

    public int buscarIdIngredientePorNombre(String nombre) {
        String sql = "SELECT id_ingrediente FROM ingredientes WHERE nombre = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id_ingrediente") : -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al buscar ingrediente por nombre: {0}", e.getMessage());
            return -1;
        }
    }
}
