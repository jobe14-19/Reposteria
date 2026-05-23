package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class CompraHistorial {
    private int idCompra;
    private String proveedor;
    private String fechaCompra;
    private double total;
    private String usuarioRegistra;
    private int totalProductos;
    private String estado;
    private List<CompraDetalle> detalles;

    public CompraHistorial() {
        this.detalles = new ArrayList<>();
    }

    public CompraHistorial(int idCompra, String proveedor, String fechaCompra,
                            double total, String usuarioRegistra, int totalProductos, String estado) {
        this.idCompra = idCompra;
        this.proveedor = proveedor;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.usuarioRegistra = usuarioRegistra;
        this.totalProductos = totalProductos;
        this.estado = estado;
        this.detalles = new ArrayList<>();
    }

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(String fechaCompra) { this.fechaCompra = fechaCompra; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getUsuarioRegistra() { return usuarioRegistra; }
    public void setUsuarioRegistra(String usuarioRegistra) { this.usuarioRegistra = usuarioRegistra; }
    public int getTotalProductos() { return totalProductos; }
    public void setTotalProductos(int totalProductos) { this.totalProductos = totalProductos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<CompraDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<CompraDetalle> detalles) { this.detalles = detalles; }

    public static class CompraDetalle {
        private String producto;
        private double cantidad;
        private String unidad;
        private double precioUnitario;
        private double descuento;
        private double subtotal;

        public CompraDetalle() {}

        public CompraDetalle(String producto, double cantidad, String unidad,
                              double precioUnitario, double descuento, double subtotal) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.unidad = unidad;
            this.precioUnitario = precioUnitario;
            this.descuento = descuento;
            this.subtotal = subtotal;
        }

        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public double getCantidad() { return cantidad; }
        public void setCantidad(double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
        public double getDescuento() { return descuento; }
        public void setDescuento(double descuento) { this.descuento = descuento; }
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    }
}
