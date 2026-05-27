package com.example.demo.dao;

import com.example.demo.model.Factura;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacturaDAO {

    private static final Logger LOGGER = Logger.getLogger(FacturaDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public FacturaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Factura> listarTodas() {
        List<Factura> list = new ArrayList<>();
        String sql = "SELECT id_factura, id_orden, cliente, telefono, direccion, fecha, subtotal, costo_delivery, itbis, descuento, total, estado, metodo_pago, pagado FROM facturas ORDER BY fecha DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar facturas: {0}", e.getMessage());
        }
        return list;
    }

    public double sumarTotal() {
        String sql = "SELECT ISNULL(SUM(total), 0) FROM facturas";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al sumar total de facturas: {0}", e.getMessage());
        }
        return 0;
    }

    public int insertar(Factura f) {
        String sql = "INSERT INTO facturas (id_orden, cliente, telefono, direccion, fecha, subtotal, costo_delivery, itbis, descuento, total, estado, detalles, usuario_genera, fecha_generacion, metodo_pago, pagado) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, f.getIdOrden() > 0 ? f.getIdOrden() : null, Types.INTEGER);
            stmt.setString(2, f.getCliente());
            stmt.setString(3, f.getTelefono());
            stmt.setString(4, f.getDireccion());
            stmt.setString(5, f.getFecha());
            stmt.setDouble(6, f.getSubtotal());
            stmt.setDouble(7, f.getCostoDelivery());
            stmt.setDouble(8, f.getItbis());
            stmt.setDouble(9, f.getDescuento());
            stmt.setDouble(10, f.getTotal());
            stmt.setString(11, f.getEstado() != null ? f.getEstado() : "EMITIDA");
            stmt.setString(12, f.getDetalles());
            stmt.setString(13, f.getUsuarioGenera());
            stmt.setString(14, f.getMetodoPago() != null ? f.getMetodoPago() : "Efectivo");
            stmt.setString(15, f.getPagado() != null ? f.getPagado() : "NO");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar factura: {0}", e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Factura f) {
        String sql = "UPDATE facturas SET cliente=?, telefono=?, direccion=?, fecha=?, subtotal=?, costo_delivery=?, itbis=?, descuento=?, total=?, estado=?, metodo_pago=?, pagado=? WHERE id_factura=?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, f.getCliente());
            stmt.setString(2, f.getTelefono());
            stmt.setString(3, f.getDireccion());
            stmt.setString(4, f.getFecha());
            stmt.setDouble(5, f.getSubtotal());
            stmt.setDouble(6, f.getCostoDelivery());
            stmt.setDouble(7, f.getItbis());
            stmt.setDouble(8, f.getDescuento());
            stmt.setDouble(9, f.getTotal());
            stmt.setString(10, f.getEstado());
            stmt.setString(11, f.getMetodoPago());
            stmt.setString(12, f.getPagado());
            stmt.setInt(13, f.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar factura: {0}", e.getMessage());
            return false;
        }
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setId(rs.getInt("id_factura"));
        f.setIdOrden(rs.getInt("id_orden"));
        f.setCliente(rs.getString("cliente"));
        f.setTelefono(rs.getString("telefono"));
        f.setDireccion(rs.getString("direccion"));
        f.setFecha(rs.getDate("fecha") != null ? rs.getDate("fecha").toString() : null);
        f.setSubtotal(rs.getDouble("subtotal"));
        f.setCostoDelivery(rs.getDouble("costo_delivery"));
        f.setItbis(rs.getDouble("itbis"));
        f.setDescuento(rs.getDouble("descuento"));
        f.setTotal(rs.getDouble("total"));
        f.setEstado(rs.getString("estado"));
        f.setMetodoPago(rs.getString("metodo_pago"));
        f.setPagado(rs.getString("pagado"));
        return f;
    }
}
