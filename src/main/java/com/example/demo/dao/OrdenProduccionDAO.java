package com.example.demo.dao;

import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.OrdenProduccion.OrdenFase;
import com.example.demo.model.OrdenProduccion.OrdenHistorial;
import com.example.demo.model.OrdenProduccion.OrdenIngrediente;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrdenProduccionDAO {

    private static final Logger LOGGER = Logger.getLogger(OrdenProduccionDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public OrdenProduccionDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        asegurarTablas();
    }

    private void asegurarTablas() {
        String[] sqls = {
            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ordenes_produccion' AND xtype='U') CREATE TABLE ordenes_produccion (" +
            "id_orden INT IDENTITY(1,1) PRIMARY KEY, numero_orden NVARCHAR(20) NOT NULL UNIQUE, estado NVARCHAR(20) DEFAULT 'ACTIVA', " +
            "categoria NVARCHAR(100), revestimiento NVARCHAR(100), sucursal NVARCHAR(100), fecha_entrega DATE, hora_entrega NVARCHAR(10), " +
            "cliente NVARCHAR(200), direccion NVARCHAR(500), telefono NVARCHAR(20), vendedor NVARCHAR(100), " +
            "libras DECIMAL(10,2) DEFAULT 0, base_tipo NVARCHAR(100), masa_tipo NVARCHAR(100), forma NVARCHAR(100), pisos INT DEFAULT 1, " +
            "lustres NVARCHAR(200), decoracion NVARCHAR(500), camuflajes NVARCHAR(200), flores NVARCHAR(200), mensaje NVARCHAR(500), " +
            "observaciones NVARCHAR(MAX), adornos NVARCHAR(500), rellenos NVARCHAR(500), " +
            "costo_estimado DECIMAL(12,2) DEFAULT 0, costo_real DECIMAL(12,2) DEFAULT 0, precio_venta DECIMAL(12,2) DEFAULT 0, " +
            "anticipo DECIMAL(12,2) DEFAULT 0, saldo DECIMAL(12,2) DEFAULT 0, id_receta INT, " +
            "fecha_creacion DATETIME DEFAULT GETDATE(), fecha_inicio DATETIME, fecha_completado DATETIME, " +
            "usuario_crea NVARCHAR(100), progreso INT DEFAULT 0, pausado BIT DEFAULT 0, " +
            "FOREIGN KEY (id_receta) REFERENCES recetas(id_receta))",

            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='orden_fases' AND xtype='U') CREATE TABLE orden_fases (" +
            "id_fase INT IDENTITY(1,1) PRIMARY KEY, id_orden INT NOT NULL, fase_nombre NVARCHAR(50) NOT NULL, fase_orden INT NOT NULL, " +
            "estado NVARCHAR(20) DEFAULT 'PENDIENTE', fecha_inicio DATETIME, fecha_fin DATETIME, " +
            "usuario_inicia NVARCHAR(100), usuario_completa NVARCHAR(100), observaciones NVARCHAR(MAX), " +
            "FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden))",

            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='orden_historial' AND xtype='U') CREATE TABLE orden_historial (" +
            "id_historial INT IDENTITY(1,1) PRIMARY KEY, id_orden INT NOT NULL, accion NVARCHAR(200) NOT NULL, " +
            "detalle NVARCHAR(MAX), usuario NVARCHAR(100), fecha_hora DATETIME DEFAULT GETDATE(), " +
            "FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden))",

            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='orden_ingredientes' AND xtype='U') CREATE TABLE orden_ingredientes (" +
            "id INT IDENTITY(1,1) PRIMARY KEY, id_orden INT NOT NULL, id_ingrediente INT NOT NULL, " +
            "cantidad_requerida DECIMAL(12,2) DEFAULT 0, cantidad_descontada DECIMAL(12,2) DEFAULT 0, descontado BIT DEFAULT 0, " +
            "FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden), " +
            "FOREIGN KEY (id_ingrediente) REFERENCES ingredientes(id_ingrediente))"
        };
        try (Connection conn = dbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron crear tablas ordenes: {0}", e.getMessage());
        }
    }

    public String generarNumeroOrden() {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(numero_orden, 4, 10) AS INT)), 0) + 1 FROM ordenes_produccion WHERE numero_orden LIKE 'ORD-%'";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return String.format("ORD-%04d", rs.getInt(1));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error generar numero: {0}", e.getMessage()); }
        return "ORD-0001";
    }

    public int insertar(OrdenProduccion orden) {
        String sql = "INSERT INTO ordenes_produccion (numero_orden, estado, categoria, revestimiento, sucursal, " +
            "fecha_entrega, hora_entrega, cliente, direccion, telefono, vendedor, libras, base_tipo, masa_tipo, forma, pisos, " +
            "lustres, decoracion, camuflajes, flores, mensaje, observaciones, adornos, rellenos, " +
            "costo_estimado, costo_real, precio_venta, anticipo, saldo, id_receta, usuario_crea) " +
            "VALUES (?, 'ACTIVA', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            String num = orden.getNumeroOrden() != null ? orden.getNumeroOrden() : generarNumeroOrden();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, num);
                stmt.setString(2, orden.getCategoria());
                stmt.setString(3, orden.getRevestimiento());
                stmt.setString(4, orden.getSucursal());
                stmt.setString(5, orden.getFechaEntrega());
                stmt.setString(6, orden.getHoraEntrega());
                stmt.setString(7, orden.getCliente());
                stmt.setString(8, orden.getDireccion());
                stmt.setString(9, orden.getTelefono());
                stmt.setString(10, orden.getVendedor());
                stmt.setDouble(11, orden.getLibras());
                stmt.setString(12, orden.getBaseTipo());
                stmt.setString(13, orden.getMaso());
                stmt.setString(14, orden.getForma());
                stmt.setInt(15, orden.getPisos());
                stmt.setString(16, orden.getLustres());
                stmt.setString(17, orden.getDecoracion());
                stmt.setString(18, orden.getCamuflajes());
                stmt.setString(19, orden.getFlores());
                stmt.setString(20, orden.getMensaje());
                stmt.setString(21, orden.getObservaciones());
                stmt.setString(22, orden.getAdornos());
                stmt.setString(23, orden.getRellenos());
                stmt.setDouble(24, orden.getCostoEstimado());
                stmt.setDouble(25, orden.getCostoReal());
                stmt.setDouble(26, orden.getPrecioVenta());
                stmt.setDouble(27, orden.getAnticipo());
                stmt.setDouble(28, orden.getSaldo());
                if (orden.getIdReceta() > 0) stmt.setInt(29, orden.getIdReceta()); else stmt.setNull(29, Types.INTEGER);
                stmt.setString(30, orden.getUsuarioCrea());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        crearFasesPorDefecto(conn, id);
                        if (orden.getIngredientes() != null && !orden.getIngredientes().isEmpty())
                            insertarIngredientes(conn, id, orden.getIngredientes());
                        registrarHistorial(conn, id, "CREACION", "Orden creada: " + num, orden.getUsuarioCrea());
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar orden: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
        return -1;
    }

    private void crearFasesPorDefecto(Connection conn, int idOrden) throws SQLException {
        String[] fases = {"Recepcion Pedido", "Validacion Stock", "Preparacion", "Relleno", "Decoracion", "Revestimiento", "Control Calidad", "Empaque y Entrega"};
        String sql = "INSERT INTO orden_fases (id_orden, fase_nombre, fase_orden, estado) VALUES (?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < fases.length; i++) {
                stmt.setInt(1, idOrden);
                stmt.setString(2, fases[i]);
                stmt.setInt(3, i + 1);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public boolean actualizar(OrdenProduccion orden) {
        String sql = "UPDATE ordenes_produccion SET categoria=?, revestimiento=?, sucursal=?, fecha_entrega=?, " +
            "hora_entrega=?, cliente=?, direccion=?, telefono=?, vendedor=?, libras=?, base_tipo=?, masa_tipo=?, forma=?, " +
            "pisos=?, lustres=?, decoracion=?, camuflajes=?, flores=?, mensaje=?, observaciones=?, adornos=?, rellenos=?, " +
            "costo_estimado=?, costo_real=?, precio_venta=?, anticipo=?, saldo=?, id_receta=? WHERE id_orden=?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orden.getCategoria());
            stmt.setString(2, orden.getRevestimiento());
            stmt.setString(3, orden.getSucursal());
            stmt.setString(4, orden.getFechaEntrega());
            stmt.setString(5, orden.getHoraEntrega());
            stmt.setString(6, orden.getCliente());
            stmt.setString(7, orden.getDireccion());
            stmt.setString(8, orden.getTelefono());
            stmt.setString(9, orden.getVendedor());
            stmt.setDouble(10, orden.getLibras());
            stmt.setString(11, orden.getBaseTipo());
            stmt.setString(12, orden.getMaso());
            stmt.setString(13, orden.getForma());
            stmt.setInt(14, orden.getPisos());
            stmt.setString(15, orden.getLustres());
            stmt.setString(16, orden.getDecoracion());
            stmt.setString(17, orden.getCamuflajes());
            stmt.setString(18, orden.getFlores());
            stmt.setString(19, orden.getMensaje());
            stmt.setString(20, orden.getObservaciones());
            stmt.setString(21, orden.getAdornos());
            stmt.setString(22, orden.getRellenos());
            stmt.setDouble(23, orden.getCostoEstimado());
            stmt.setDouble(24, orden.getCostoReal());
            stmt.setDouble(25, orden.getPrecioVenta());
            stmt.setDouble(26, orden.getAnticipo());
            stmt.setDouble(27, orden.getSaldo());
            if (orden.getIdReceta() > 0) stmt.setInt(28, orden.getIdReceta()); else stmt.setNull(28, Types.INTEGER);
            stmt.setInt(29, orden.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar orden: {0}", e.getMessage());
            return false;
        }
    }

    public boolean cambiarEstado(int idOrden, String nuevoEstado, String usuario) {
        String sql = "UPDATE ordenes_produccion SET estado=? WHERE id_orden=?";
        String hist = "UPDATE orden_historial SET accion='CAMBIO_ESTADO', detalle='Estado cambiado a: " + nuevoEstado + "', usuario=? WHERE id_orden=?";
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nuevoEstado); stmt.setInt(2, idOrden);
                stmt.executeUpdate();
            }
            if ("EN PRODUCCION".equals(nuevoEstado)) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE ordenes_produccion SET fecha_inicio=GETDATE(), progreso=10 WHERE id_orden=?")) {
                    up.setInt(1, idOrden); up.executeUpdate();
                }
            }
            if ("COMPLETADA".equals(nuevoEstado)) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE ordenes_produccion SET fecha_completado=GETDATE(), progreso=100 WHERE id_orden=?")) {
                    up.setInt(1, idOrden); up.executeUpdate();
                }
            }
            registrarHistorial(conn, idOrden, "CAMBIO_ESTADO", "Estado cambiado a: " + nuevoEstado, usuario);
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error cambiar estado: {0}", e.getMessage());
            return false;
        }
    }

    public boolean pausarReanudar(int idOrden, boolean pausar, String usuario) {
        String sql = "UPDATE ordenes_produccion SET pausado=? WHERE id_orden=?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, pausar); stmt.setInt(2, idOrden);
            stmt.executeUpdate();
            registrarHistorial(conn, idOrden, pausar ? "PAUSA" : "REANUDACION", "Produccion " + (pausar ? "pausada" : "reanudada"), usuario);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error pausar/reanudar: {0}", e.getMessage());
            return false;
        }
    }

    public boolean iniciarFase(int idFase, String usuario) {
        String sql = "UPDATE orden_fases SET estado='EN CURSO', fecha_inicio=GETDATE(), usuario_inicia=? WHERE id_fase=? AND estado='PENDIENTE'";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario); stmt.setInt(2, idFase);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error iniciar fase: {0}", e.getMessage());
            return false;
        }
    }

    public boolean completarFase(int idFase, String usuario, String observaciones) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE orden_fases SET estado='COMPLETADA', fecha_fin=GETDATE(), usuario_completa=?, observaciones=? WHERE id_fase=?")) {
                stmt.setString(1, usuario); stmt.setString(2, observaciones); stmt.setInt(3, idFase);
                stmt.executeUpdate();
            }
            int idOrden = 0;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id_orden FROM orden_fases WHERE id_fase=?")) {
                stmt.setInt(1, idFase);
                try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) idOrden = rs.getInt("id_orden"); }
            }
            if (idOrden > 0) {
                actualizarProgreso(conn, idOrden);
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM orden_fases WHERE id_orden=? AND estado='COMPLETADA'")) {
                    stmt.setInt(1, idOrden);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) >= 8) {
                            try (PreparedStatement up = conn.prepareStatement(
                                "UPDATE ordenes_produccion SET estado='COMPLETADA', fecha_completado=GETDATE() WHERE id_orden=?")) {
                                up.setInt(1, idOrden); up.executeUpdate();
                            }
                            registrarHistorial(conn, idOrden, "COMPLETADA", "Orden completada automaticamente", usuario);
                        }
                    }
                }
                registrarHistorial(conn, idOrden, "FASE_COMPLETADA", "Fase completada por " + usuario, usuario);
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error completar fase: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private void actualizarProgreso(Connection conn, int idOrden) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE ordenes_produccion SET progreso = (SELECT CAST(COUNT(*) * 100 / 8 AS INT) FROM orden_fases WHERE id_orden=? AND estado='COMPLETADA') WHERE id_orden=?")) {
            stmt.setInt(1, idOrden); stmt.setInt(2, idOrden);
            stmt.executeUpdate();
        }
    }

    public boolean descontarStock(int idOrden, String usuario) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            List<OrdenIngrediente> ingredientes = obtenerIngredientes(conn, idOrden);
            StockMovimientoDAO stockDAO = new StockMovimientoDAO();
            for (OrdenIngrediente ing : ingredientes) {
                if (ing.isDescontado()) continue;
                double stockActual = stockDAO.getStockActual(ing.getIdIngrediente());
                if (stockActual < ing.getCantidadRequerida()) {
                    conn.rollback();
                    return false;
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE ingredientes SET stock_actual = stock_actual - ? WHERE id_ingrediente=?")) {
                    stmt.setDouble(1, ing.getCantidadRequerida()); stmt.setInt(2, ing.getIdIngrediente());
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO stock_movimientos (id_ingrediente, tipo_movimiento, cantidad, stock_anterior, stock_nuevo, motivo, referencia_tipo, referencia_id, usuario_registra, fecha_hora) " +
                    "VALUES (?, 'SALIDA', ?, (SELECT stock_actual + ? FROM ingredientes WHERE id_ingrediente=?), (SELECT stock_actual FROM ingredientes WHERE id_ingrediente=?), 'Produccion - Orden " + idOrden + "', 'ORDEN', ?, ?, GETDATE())")) {
                    stmt.setInt(1, ing.getIdIngrediente());
                    stmt.setDouble(2, ing.getCantidadRequerida());
                    stmt.setDouble(3, ing.getCantidadRequerida());
                    stmt.setInt(4, ing.getIdIngrediente());
                    stmt.setInt(5, ing.getIdIngrediente());
                    stmt.setInt(6, idOrden);
                    stmt.setString(7, usuario);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE orden_ingredientes SET cantidad_descontada=?, descontado=1 WHERE id_orden=? AND id_ingrediente=?")) {
                    stmt.setDouble(1, ing.getCantidadRequerida()); stmt.setInt(2, idOrden); stmt.setInt(3, ing.getIdIngrediente());
                    stmt.executeUpdate();
                }
            }
            registrarHistorial(conn, idOrden, "STOCK_DESHABITADO", "Stock descontado para produccion", usuario);
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error descontar stock: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private void insertarIngredientes(Connection conn, int idOrden, List<OrdenIngrediente> ingredientes) throws SQLException {
        String sql = "INSERT INTO orden_ingredientes (id_orden, id_ingrediente, cantidad_requerida) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (OrdenIngrediente ing : ingredientes) {
                stmt.setInt(1, idOrden); stmt.setInt(2, ing.getIdIngrediente());
                stmt.setDouble(3, ing.getCantidadRequerida());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void registrarHistorial(Connection conn, int idOrden, String accion, String detalle, String usuario) throws SQLException {
        String sql = "INSERT INTO orden_historial (id_orden, accion, detalle, usuario, fecha_hora) VALUES (?, ?, ?, ?, GETDATE())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idOrden); stmt.setString(2, accion);
            stmt.setString(3, detalle); stmt.setString(4, usuario);
            stmt.executeUpdate();
        }
    }

    public List<OrdenProduccion> listarTodas() {
        List<OrdenProduccion> lista = new ArrayList<>();
        String sql = "SELECT o.*, r.nombre_receta FROM ordenes_produccion o LEFT JOIN recetas r ON o.id_receta = r.id_receta ORDER BY o.fecha_creacion DESC";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) { lista.add(mapearOrden(rs)); }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listar ordenes: {0}", e.getMessage());
        }
        return lista;
    }

    public List<OrdenProduccion> listarPorEstado(String estado) {
        List<OrdenProduccion> lista = new ArrayList<>();
        String sql = "SELECT o.*, r.nombre_receta FROM ordenes_produccion o LEFT JOIN recetas r ON o.id_receta = r.id_receta WHERE o.estado=? ORDER BY o.fecha_entrega";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            try (ResultSet rs = stmt.executeQuery()) { while (rs.next()) lista.add(mapearOrden(rs)); }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listar por estado: {0}", e.getMessage());
        }
        return lista;
    }

    public OrdenProduccion obtenerPorId(int id) {
        String sql = "SELECT o.*, r.nombre_receta FROM ordenes_produccion o LEFT JOIN recetas r ON o.id_receta = r.id_receta WHERE o.id_orden=?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    OrdenProduccion orden = mapearOrden(rs);
                    orden.setFases(obtenerFases(conn, id));
                    orden.setHistorial(obtenerHistorial(conn, id));
                    orden.setIngredientes(obtenerIngredientes(conn, id));
                    return orden;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obtener orden: {0}", e.getMessage());
        }
        return null;
    }

    private OrdenProduccion mapearOrden(ResultSet rs) throws SQLException {
        OrdenProduccion o = new OrdenProduccion();
        o.setId(rs.getInt("id_orden"));
        o.setNumeroOrden(rs.getString("numero_orden"));
        o.setEstado(rs.getString("estado"));
        o.setCategoria(rs.getString("categoria"));
        o.setRevestimiento(rs.getString("revestimiento"));
        o.setSucursal(rs.getString("sucursal"));
        o.setFechaEntrega(rs.getString("fecha_entrega") != null ? rs.getDate("fecha_entrega").toString() : null);
        o.setHoraEntrega(rs.getString("hora_entrega"));
        o.setCliente(rs.getString("cliente"));
        o.setDireccion(rs.getString("direccion"));
        o.setTelefono(rs.getString("telefono"));
        o.setVendedor(rs.getString("vendedor"));
        o.setLibras(rs.getDouble("libras"));
        o.setBaseTipo(rs.getString("base_tipo"));
        o.setMaso(rs.getString("masa_tipo"));
        o.setForma(rs.getString("forma"));
        o.setPisos(rs.getInt("pisos"));
        o.setLustres(rs.getString("lustres"));
        o.setDecoracion(rs.getString("decoracion"));
        o.setCamuflajes(rs.getString("camuflajes"));
        o.setFlores(rs.getString("flores"));
        o.setMensaje(rs.getString("mensaje"));
        o.setObservaciones(rs.getString("observaciones"));
        o.setAdornos(rs.getString("adornos"));
        o.setRellenos(rs.getString("rellenos"));
        o.setCostoEstimado(rs.getDouble("costo_estimado"));
        o.setCostoReal(rs.getDouble("costo_real"));
        o.setPrecioVenta(rs.getDouble("precio_venta"));
        o.setAnticipo(rs.getDouble("anticipo"));
        o.setSaldo(rs.getDouble("saldo"));
        o.setIdReceta(rs.getInt("id_receta"));
        o.setNombreReceta(rs.getString("nombre_receta"));
        o.setUsuarioCrea(rs.getString("usuario_crea"));
        o.setProgreso(rs.getInt("progreso"));
        o.setPausado(rs.getBoolean("pausado"));
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) o.setFechaCreacion(ts.toLocalDateTime());
        ts = rs.getTimestamp("fecha_inicio");
        if (ts != null) o.setFechaInicio(ts.toLocalDateTime());
        ts = rs.getTimestamp("fecha_completado");
        if (ts != null) o.setFechaCompletado(ts.toLocalDateTime());
        return o;
    }

    private List<OrdenFase> obtenerFases(Connection conn, int idOrden) throws SQLException {
        List<OrdenFase> lista = new ArrayList<>();
        String sql = "SELECT * FROM orden_fases WHERE id_orden=? ORDER BY fase_orden";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrdenFase f = new OrdenFase();
                    f.setIdFase(rs.getInt("id_fase"));
                    f.setIdOrden(rs.getInt("id_orden"));
                    f.setFaseOrden(rs.getInt("fase_orden"));
                    f.setFaseNombre(rs.getString("fase_nombre"));
                    f.setEstado(rs.getString("estado"));
                    Timestamp ts = rs.getTimestamp("fecha_inicio");
                    f.setFechaInicio(ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
                    ts = rs.getTimestamp("fecha_fin");
                    f.setFechaFin(ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
                    f.setUsuarioInicia(rs.getString("usuario_inicia"));
                    f.setUsuarioCompleta(rs.getString("usuario_completa"));
                    f.setObservaciones(rs.getString("observaciones"));
                    lista.add(f);
                }
            }
        }
        return lista;
    }

    private List<OrdenHistorial> obtenerHistorial(Connection conn, int idOrden) throws SQLException {
        List<OrdenHistorial> lista = new ArrayList<>();
        String sql = "SELECT * FROM orden_historial WHERE id_orden=? ORDER BY fecha_hora DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrdenHistorial h = new OrdenHistorial();
                    h.setIdHistorial(rs.getInt("id_historial"));
                    h.setIdOrden(rs.getInt("id_orden"));
                    h.setAccion(rs.getString("accion"));
                    h.setDetalle(rs.getString("detalle"));
                    h.setUsuario(rs.getString("usuario"));
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) h.setFechaHora(ts.toLocalDateTime());
                    lista.add(h);
                }
            }
        }
        return lista;
    }

    private List<OrdenIngrediente> obtenerIngredientes(Connection conn, int idOrden) throws SQLException {
        List<OrdenIngrediente> lista = new ArrayList<>();
        String sql = "SELECT oi.*, i.nombre as nombre_ingrediente, i.unidad FROM orden_ingredientes oi INNER JOIN ingredientes i ON oi.id_ingrediente = i.id_ingrediente WHERE oi.id_orden=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrdenIngrediente ing = new OrdenIngrediente();
                    ing.setId(rs.getInt("id"));
                    ing.setIdOrden(rs.getInt("id_orden"));
                    ing.setIdIngrediente(rs.getInt("id_ingrediente"));
                    ing.setNombreIngrediente(rs.getString("nombre_ingrediente"));
                    ing.setUnidad(rs.getString("unidad"));
                    ing.setCantidadRequerida(rs.getDouble("cantidad_requerida"));
                    ing.setCantidadDescontada(rs.getDouble("cantidad_descontada"));
                    ing.setDescontado(rs.getBoolean("descontado"));
                    lista.add(ing);
                }
            }
        }
        return lista;
    }

    public List<Receta> listarRecetas() {
        return new RecetaDAO().listarTodas();
    }

    public List<Receta.RecetaIngrediente> obtenerIngredientesReceta(int idReceta) {
        Receta r = new RecetaDAO().obtenerPorId(idReceta);
        return r != null ? r.getIngredientes() : new ArrayList<>();
    }

    public boolean validarStockDisponible(int idOrden) {
        OrdenProduccion orden = obtenerPorId(idOrden);
        if (orden == null) return false;
        StockMovimientoDAO stockDAO = new StockMovimientoDAO();
        for (OrdenIngrediente ing : orden.getIngredientes()) {
            if (stockDAO.getStockActual(ing.getIdIngrediente()) < ing.getCantidadRequerida()) return false;
        }
        return true;
    }
}
