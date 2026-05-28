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

            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='tipo_entrega') " +
            "ALTER TABLE ordenes_produccion ADD tipo_entrega NVARCHAR(2) DEFAULT 'L'",

            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='costo_delivery') " +
            "ALTER TABLE ordenes_produccion ADD costo_delivery DECIMAL(12,2) DEFAULT 0",

            "IF EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='forma_pago') " +
            "EXEC sp_rename 'ordenes_produccion.forma_pago', 'tipo_pago', 'COLUMN'; " +
            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='tipo_pago') " +
            "ALTER TABLE ordenes_produccion ADD tipo_pago NVARCHAR(50) DEFAULT 'Efectivo'",

            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='estado_pago') " +
            "ALTER TABLE ordenes_produccion ADD estado_pago NVARCHAR(20) DEFAULT 'Pendiente'",

            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='id_pedido') " +
            "ALTER TABLE ordenes_produccion ADD id_pedido INT",

            "IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='fecha_entregado') " +
            "ALTER TABLE ordenes_produccion ADD fecha_entregado DATETIME",

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
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(numero_orden, 5, 10) AS INT)), 0) + 1 FROM ordenes_produccion WHERE numero_orden LIKE 'ORD-%'";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return String.format("ORD-%04d", rs.getInt(1));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error generar numero: {0}", e.getMessage()); }
        return "ORD-0001";
    }

    public int insertar(OrdenProduccion orden) {
        String sql = "INSERT INTO ordenes_produccion (numero_orden, estado, categoria, revestimiento, sucursal, " +
            "fecha_entrega, hora_entrega, cliente, direccion, telefono, vendedor, libras, base_tipo, masa_tipo, forma, pisos, " +
            "lustres, decoracion, camuflajes, flores, mensaje, observaciones, adornos, rellenos, " +
            "costo_estimado, costo_real, precio_venta, anticipo, saldo, id_receta, usuario_crea, tipo_pago, estado_pago, id_pedido) " +
            "VALUES (?, 'ACTIVA', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                if (orden.getFechaEntrega() != null) stmt.setDate(5, java.sql.Date.valueOf(orden.getFechaEntrega())); else stmt.setNull(5, Types.DATE);
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
                stmt.setString(31, orden.getTipoPago() != null ? orden.getTipoPago() : "Efectivo");
                stmt.setString(32, orden.getEstadoPago() != null ? orden.getEstadoPago() : "Pendiente");
                if (orden.getIdPedido() > 0) stmt.setInt(33, orden.getIdPedido()); else stmt.setNull(33, Types.INTEGER);
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

    public int insertarEnTransaccion(Connection conn, OrdenProduccion orden) throws SQLException {
        String sql = "INSERT INTO ordenes_produccion (numero_orden, estado, categoria, revestimiento, sucursal, " +
            "fecha_entrega, hora_entrega, cliente, direccion, telefono, vendedor, libras, base_tipo, masa_tipo, forma, pisos, " +
            "lustres, decoracion, camuflajes, flores, mensaje, observaciones, adornos, rellenos, " +
            "costo_estimado, costo_real, precio_venta, anticipo, saldo, id_receta, usuario_crea, tipo_pago, estado_pago, id_pedido) " +
            "VALUES (?, 'ACTIVA', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String num = orden.getNumeroOrden() != null ? orden.getNumeroOrden() : generarNumeroOrden();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, num);
            stmt.setString(2, orden.getCategoria());
            stmt.setString(3, orden.getRevestimiento());
            stmt.setString(4, orden.getSucursal());
            if (orden.getFechaEntrega() != null) stmt.setDate(5, java.sql.Date.valueOf(orden.getFechaEntrega())); else stmt.setNull(5, Types.DATE);
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
            stmt.setString(31, orden.getTipoPago() != null ? orden.getTipoPago() : "Efectivo");
            stmt.setString(32, orden.getEstadoPago() != null ? orden.getEstadoPago() : "Pendiente");
            if (orden.getIdPedido() > 0) stmt.setInt(33, orden.getIdPedido()); else stmt.setNull(33, Types.INTEGER);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    crearFasesPorDefecto(conn, id);
                    if (orden.getIngredientes() != null && !orden.getIngredientes().isEmpty())
                        insertarIngredientes(conn, id, orden.getIngredientes());
                    registrarHistorial(conn, id, "CREACION", "Orden creada: " + num, orden.getUsuarioCrea());
                    return id;
                }
            }
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
            "costo_estimado=?, costo_real=?, precio_venta=?, anticipo=?, saldo=?, id_receta=?, tipo_pago=?, estado_pago=?, id_pedido=? WHERE id_orden=?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orden.getCategoria());
            stmt.setString(2, orden.getRevestimiento());
            stmt.setString(3, orden.getSucursal());
            if (orden.getFechaEntrega() != null) stmt.setDate(4, java.sql.Date.valueOf(orden.getFechaEntrega())); else stmt.setNull(4, Types.DATE);
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
            stmt.setString(29, orden.getTipoPago() != null ? orden.getTipoPago() : "Efectivo");
            stmt.setString(30, orden.getEstadoPago() != null ? orden.getEstadoPago() : "Pendiente");
            if (orden.getIdPedido() > 0) stmt.setInt(31, orden.getIdPedido()); else stmt.setNull(31, Types.INTEGER);
            stmt.setInt(32, orden.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar orden: {0}", e.getMessage());
            return false;
        }
    }

    public boolean actualizarEstadoPago(int idOrden, String estadoPago) {
        String sql = "UPDATE ordenes_produccion SET estado_pago = ? WHERE id_orden = ?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estadoPago);
            stmt.setInt(2, idOrden);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar estado_pago: {0}", e.getMessage());
            return false;
        }
    }

    public boolean cambiarEstado(int idOrden, String nuevoEstado, String usuario) {
        String sql = "UPDATE ordenes_produccion SET estado=? WHERE id_orden=?";
        String hist = "UPDATE orden_historial SET accion='CAMBIO_ESTADO', detalle='Estado cambiado a: " + nuevoEstado + "', usuario=? WHERE id_orden=?";
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nuevoEstado); stmt.setInt(2, idOrden);
                stmt.executeUpdate();
            }
            if ("EN PRODUCCION".equals(nuevoEstado)) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE ordenes_produccion SET fecha_inicio=GETDATE(), progreso=10 WHERE id_orden=?")) {
                    up.setInt(1, idOrden); up.executeUpdate();
                }
                if (!descontarStock(conn, idOrden, usuario)) {
                    conn.rollback();
                    return false;
                }
            }
            if ("COMPLETADA".equals(nuevoEstado) || "LISTO_PARA_ENTREGAR".equals(nuevoEstado)) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE ordenes_produccion SET fecha_completado=GETDATE(), progreso=100 WHERE id_orden=?")) {
                    up.setInt(1, idOrden); up.executeUpdate();
                }
                generarFacturaAuto(conn, idOrden, usuario);
                int idPedido = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id_pedido FROM ordenes_produccion WHERE id_orden=?")) {
                    ps.setInt(1, idOrden);
                    try (ResultSet r = ps.executeQuery()) { if (r.next()) idPedido = r.getInt("id_pedido"); }
                }
                if (idPedido > 0) {
                    try (PreparedStatement upPed = conn.prepareStatement(
                        "UPDATE pedidos SET estado='Listo para entregar' WHERE id_pedido=?")) {
                        upPed.setInt(1, idPedido); upPed.executeUpdate();
                    }
                    registrarHistorial(conn, idOrden, "PEDIDO_LISTO", "Pedido marcado como Listo para entregar", usuario);
                }
            }
            if ("ENTREGADA".equals(nuevoEstado)) {
                generarFacturaAuto(conn, idOrden, usuario);
                int idPedido = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id_pedido FROM ordenes_produccion WHERE id_orden=?")) {
                    ps.setInt(1, idOrden);
                    try (ResultSet r = ps.executeQuery()) { if (r.next()) idPedido = r.getInt("id_pedido"); }
                }
                if (idPedido > 0) {
                    try (PreparedStatement upPed = conn.prepareStatement(
                        "UPDATE pedidos SET estado='Entregado' WHERE id_pedido=?")) {
                        upPed.setInt(1, idPedido); upPed.executeUpdate();
                    }
                    registrarHistorial(conn, idOrden, "PEDIDO_ENTREGADO", "Pedido marcado como Entregado", usuario);
                }
            }
            registrarHistorial(conn, idOrden, "CAMBIO_ESTADO", "Estado cambiado a: " + nuevoEstado, usuario);
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error cambiar estado: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
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
                                "UPDATE ordenes_produccion SET estado='LISTO_PARA_ENTREGAR', fecha_completado=GETDATE(), progreso=100 WHERE id_orden=?")) {
                                up.setInt(1, idOrden); up.executeUpdate();
                            }
                            registrarHistorial(conn, idOrden, "LISTO_PARA_ENTREGAR", "Orden lista para entregar", usuario);
                            generarFacturaAuto(conn, idOrden, usuario);
                            int idPedido = 0;
                            try (PreparedStatement ps = conn.prepareStatement("SELECT id_pedido FROM ordenes_produccion WHERE id_orden=?")) {
                                ps.setInt(1, idOrden);
                                try (ResultSet r = ps.executeQuery()) { if (r.next()) idPedido = r.getInt("id_pedido"); }
                            }
                            if (idPedido > 0) {
                                try (PreparedStatement upPed = conn.prepareStatement(
                                    "UPDATE pedidos SET estado='Listo para entregar' WHERE id_pedido=?")) {
                                    upPed.setInt(1, idPedido); upPed.executeUpdate();
                                }
                            }
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
            boolean result = descontarStock(conn, idOrden, usuario);
            if (result) conn.commit();
            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error descontar stock: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private void generarFacturaAuto(Connection conn, int idOrden, String usuario) throws SQLException {
        String sqlOrd = "SELECT id_orden, cliente, telefono, direccion, precio_venta, costo_delivery, categoria, libras, decoracion, adornos, rellenos, mensaje FROM ordenes_produccion WHERE id_orden=?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlOrd)) {
            stmt.setInt(1, idOrden);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String cliente = rs.getString("cliente");
                    if (cliente == null || cliente.trim().isEmpty()) return;
                    String telefono = rs.getString("telefono");
                    String direccion = rs.getString("direccion");
                    double subtotal = rs.getDouble("precio_venta");
                    double delivery = rs.getDouble("costo_delivery");
                    if (delivery < 0) delivery = 0;
                    double itbis = (subtotal + delivery) * 0.18;
                    double total = subtotal + delivery + itbis;
                    String detalles = String.format("Categoria: %s | Libras: %.1f | Decoracion: %s | Adornos: %s | Rellenos: %s | Mensaje: %s",
                        rs.getString("categoria"), rs.getDouble("libras"),
                        rs.getString("decoracion"), rs.getString("adornos"),
                        rs.getString("rellenos"), rs.getString("mensaje"));
                    String sqlIns = "INSERT INTO facturas (id_orden, cliente, telefono, direccion, fecha, subtotal, costo_delivery, itbis, descuento, total, estado, detalles, usuario_genera, fecha_generacion) VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, 0, ?, 'EMITIDA', ?, ?, GETDATE())";
                    try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
                        ins.setInt(1, idOrden);
                        ins.setString(2, cliente);
                        ins.setString(3, telefono);
                        ins.setString(4, direccion);
                        ins.setDouble(5, subtotal);
                        ins.setDouble(6, delivery);
                        ins.setDouble(7, itbis);
                        ins.setDouble(8, total);
                        ins.setString(9, detalles);
                        ins.setString(10, usuario);
                        ins.executeUpdate();
                    }
                    registrarHistorial(conn, idOrden, "FACTURA_AUTO", "Factura generada automaticamente para " + cliente, usuario);
                }
            }
        }
    }

    private boolean descontarStock(Connection conn, int idOrden, String usuario) throws SQLException {
        List<OrdenIngrediente> ingredientes = obtenerIngredientes(conn, idOrden);
        StockMovimientoDAO stockDAO = new StockMovimientoDAO();
        for (OrdenIngrediente ing : ingredientes) {
            if (ing.isDescontado()) continue;
            double stockActual = stockDAO.getStockActual(ing.getIdIngrediente());
            if (stockActual < ing.getCantidadRequerida()) {
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
        return true;
    }

    private void insertarIngredientes(Connection conn, int idOrden, List<OrdenIngrediente> ingredientes) throws SQLException {
        String sql = "INSERT INTO orden_ingredientes (id_orden, id_ingrediente, cantidad_requerida) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (OrdenIngrediente ing : ingredientes) {
                if (ing.getIdIngrediente() <= 0) continue;
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

    private OrdenProduccion obtenerPorIdEnTransaccion(Connection conn, int id) throws SQLException {
        String sql = "SELECT o.*, r.nombre_receta FROM ordenes_produccion o LEFT JOIN recetas r ON o.id_receta = r.id_receta WHERE o.id_orden=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        o.setTipoPago(rs.getString("tipo_pago"));
        o.setEstadoPago(rs.getString("estado_pago"));
        o.setIdPedido(rs.getInt("id_pedido"));
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

    public boolean actualizarPagoPorIdPedido(int idPedido, double monto, String estadoPago) {
        String sql = "UPDATE ordenes_produccion SET anticipo = ?, saldo = CASE WHEN ? = 'PAGADO' THEN 0 ELSE saldo END, estado_pago = ? WHERE id_pedido = ?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, monto);
            stmt.setString(2, estadoPago);
            stmt.setString(3, estadoPago);
            stmt.setInt(4, idPedido);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar pago orden: {0}", e.getMessage());
            return false;
        }
    }

    public boolean asignarReceta(int idOrden, int idReceta) {
        String sql = "UPDATE ordenes_produccion SET id_receta = ? WHERE id_orden = ?";
        try (Connection conn = dbConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idReceta);
            stmt.setInt(2, idOrden);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al asignar receta a orden {0}: {1}", new Object[]{idOrden, e.getMessage()});
            return false;
        }
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

    public int asegurarPedidoVinculadoConConex(Connection conn, int idOrden) {
        String sqlSelect = "SELECT id_pedido, numero_orden, cliente, fecha_entrega, libras, precio_venta, anticipo, estado, tipo_pago, estado_pago, direccion, costo_delivery, categoria, tipo_entrega FROM ordenes_produccion WHERE id_orden=?";
        int idPedidoExistente = 0;
        String numeroOrden = "";
        String cliente = "";
        String fechaEntrega = null;
        double libras = 0;
        double precioVenta = 0;
        double anticipo = 0;
        String estado = "Pendiente";
        String tipoPago = "Efectivo";
        String estadoPago = "Pendiente";
        String direccion = "";
        double costoDelivery = 0;
        String categoria = "Pastel";
        String tipoEntrega = "L";

        try (PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
            stmt.setInt(1, idOrden);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idPedidoExistente = rs.getInt("id_pedido");
                    numeroOrden = rs.getString("numero_orden");
                    cliente = rs.getString("cliente");
                    Date d = rs.getDate("fecha_entrega");
                    if (d != null) fechaEntrega = d.toString();
                    libras = rs.getDouble("libras");
                    precioVenta = rs.getDouble("precio_venta");
                    anticipo = rs.getDouble("anticipo");
                    estado = rs.getString("estado");
                    tipoPago = rs.getString("tipo_pago");
                    estadoPago = rs.getString("estado_pago");
                    direccion = rs.getString("direccion");
                    costoDelivery = rs.getDouble("costo_delivery");
                    categoria = rs.getString("categoria");
                    tipoEntrega = rs.getString("tipo_entrega");
                    if (tipoEntrega == null || tipoEntrega.trim().isEmpty()) tipoEntrega = "L";
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar orden para vincular pedido: " + e.getMessage());
            return -1;
        }

        if (idPedidoExistente > 0) return idPedidoExistente;

        Integer idCliente = null;
        String clientUsername = null;
        if (cliente != null && !cliente.trim().isEmpty()) {
            String sqlClient = "SELECT TOP 1 id_cliente, usuario FROM clientes WHERE (nombre + ' ' + ISNULL(apellido, '')) LIKE ? OR nombre LIKE ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlClient)) {
                String like = "%" + cliente.trim() + "%";
                stmt.setString(1, like);
                stmt.setString(2, like);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        idCliente = rs.getInt("id_cliente");
                        clientUsername = rs.getString("usuario");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al buscar cliente coincidente: " + e.getMessage());
            }
        }
        if (idCliente == null) {
            idCliente = 3; 
            clientUsername = "cliente";
        }

        String sqlIns = "INSERT INTO pedidos (id_cliente, fecha_pedido, fecha_entrega, libras, total, adelanto, estado, tipo_pago, estado_pago, username, producto, observaciones, direccion_entrega, costo_delivery, tipo_entrega) " +
                        "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int idPedido = -1;
        try (PreparedStatement stmt = conn.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
            if (idCliente != null) stmt.setInt(1, idCliente); else stmt.setNull(1, Types.INTEGER);
            if (fechaEntrega != null) stmt.setDate(2, java.sql.Date.valueOf(fechaEntrega)); else stmt.setNull(2, Types.DATE);
            stmt.setDouble(3, libras);
            stmt.setDouble(4, precioVenta);
            stmt.setDouble(5, anticipo);
            stmt.setString(6, estado);
            stmt.setString(7, tipoPago != null ? tipoPago : "Efectivo");
            stmt.setString(8, estadoPago != null ? estadoPago : "Pendiente");
            stmt.setString(9, clientUsername);
            stmt.setString(10, categoria != null ? categoria : "Pastel");
            stmt.setString(11, "Pedido generado automáticamente desde orden de producción #" + numeroOrden);
            stmt.setString(12, direccion);
            stmt.setDouble(13, costoDelivery);
            stmt.setString(14, tipoEntrega);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) idPedido = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar pedido dummy para orden: " + e.getMessage());
            return -1;
        }

        if (idPedido > 0) {
            String sqlUpd = "UPDATE ordenes_produccion SET id_pedido = ? WHERE id_orden = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpd)) {
                stmt.setInt(1, idPedido);
                stmt.setInt(2, idOrden);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar orden de producción con id_pedido: " + e.getMessage());
                return -1;
            }
            return idPedido;
        }
        return -1;
    }

    public boolean registrarPagoCompleto(int idOrden, String metodoPago, String referencia, double monto, String usuario) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            int idPedido = asegurarPedidoVinculadoConConex(conn, idOrden);
            if (idPedido <= 0) {
                conn.rollback();
                return false;
            }

            OrdenProduccion orden = obtenerPorIdEnTransaccion(conn, idOrden);
            if (orden == null) {
                conn.rollback();
                return false;
            }

            double nuevoAnticipo = orden.getAnticipo() + monto;
            double nuevoSaldo = Math.max(0, orden.getPrecioVenta() - nuevoAnticipo);
            String nuevoEstadoPago = nuevoSaldo <= 0 ? "PAGADO" : "PAGADO_PARCIAL";

            String sqlOrd = "UPDATE ordenes_produccion SET anticipo = ?, saldo = ?, estado_pago = ?, tipo_pago = ? WHERE id_orden = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlOrd)) {
                stmt.setDouble(1, nuevoAnticipo);
                stmt.setDouble(2, nuevoSaldo);
                stmt.setString(3, nuevoEstadoPago);
                stmt.setString(4, metodoPago);
                stmt.setInt(5, idOrden);
                stmt.executeUpdate();
            }

            String sqlPed = "UPDATE pedidos SET total = ?, adelanto = ?, estado_pago = ?, tipo_pago = ? WHERE id_pedido = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPed)) {
                stmt.setDouble(1, orden.getPrecioVenta());
                stmt.setDouble(2, nuevoAnticipo);
                stmt.setString(3, nuevoEstadoPago);
                stmt.setString(4, metodoPago);
                stmt.setInt(5, idPedido);
                stmt.executeUpdate();
            }

            String sqlPago = "INSERT INTO pagos (id_pedido, monto, fecha_pago, metodo_pago, referencia, estado) VALUES (?, ?, GETDATE(), ?, ?, 'Pagado')";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPago)) {
                stmt.setInt(1, idPedido);
                stmt.setDouble(2, monto);
                stmt.setString(3, metodoPago);
                stmt.setString(4, referencia != null ? referencia : "Pago registrado desde Orden de Producción");
                stmt.executeUpdate();
            }

            int idFactura = -1;
            String sqlFactCheck = "SELECT id_factura FROM facturas WHERE id_orden = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlFactCheck)) {
                stmt.setInt(1, idOrden);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) idFactura = rs.getInt("id_factura");
                }
            }

            if (idFactura > 0) {
                String sqlFactUpd = "UPDATE facturas SET pagado = ?, estado = ?, metodo_pago = ? WHERE id_factura = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlFactUpd)) {
                    stmt.setString(1, nuevoEstadoPago.equals("PAGADO") ? "SI" : "NO");
                    stmt.setString(2, nuevoEstadoPago.equals("PAGADO") ? "PAGADA" : "EMITIDA");
                    stmt.setString(3, metodoPago);
                    stmt.setInt(4, idFactura);
                    stmt.executeUpdate();
                }
            } else {
                String cliente = orden.getCliente();
                String telefono = orden.getTelefono();
                String direccion = orden.getDireccion();
                double subtotal = orden.getPrecioVenta();
                double delivery = orden.getCostoDelivery();
                if (delivery < 0) delivery = 0;
                double itbis = (subtotal + delivery) * 0.18;
                double total = subtotal + delivery + itbis;
                String detalles = String.format("Categoria: %s | Libras: %.1f | Decoracion: %s | Adornos: %s | Rellenos: %s | Mensaje: %s",
                    orden.getCategoria(), orden.getLibras(),
                    orden.getDecoracion(), orden.getAdornos(),
                    orden.getRellenos(), orden.getMensaje());
                    
                String sqlIns = "INSERT INTO facturas (id_orden, cliente, telefono, direccion, fecha, subtotal, costo_delivery, itbis, descuento, total, estado, detalles, usuario_genera, fecha_generacion, metodo_pago, pagado) " +
                                "VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, 0, ?, ?, ?, ?, GETDATE(), ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlIns)) {
                    stmt.setInt(1, idOrden);
                    stmt.setString(2, cliente);
                    stmt.setString(3, telefono);
                    stmt.setString(4, direccion);
                    stmt.setDouble(5, subtotal);
                    stmt.setDouble(6, delivery);
                    stmt.setDouble(7, itbis);
                    stmt.setDouble(8, total);
                    stmt.setString(9, nuevoEstadoPago.equals("PAGADO") ? "PAGADA" : "EMITIDA");
                    stmt.setString(10, detalles);
                    stmt.setString(11, usuario);
                    stmt.setString(12, metodoPago);
                    stmt.setString(13, nuevoEstadoPago.equals("PAGADO") ? "SI" : "NO");
                    stmt.executeUpdate();
                }
            }

            registrarHistorial(conn, idOrden, "REGISTRO_PAGO", "Pago registrado de RD$" + String.format("%.2f", monto) + " via " + metodoPago + ". Referencia: " + referencia, usuario);

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar pago en orden: " + e.getMessage(), e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
            }
        }
    }
}
