package com.example.demo.model;

import java.util.List;

public class ChefBox {
    private int id;
    private String nombre;
    private String descripcion;
    private double precio;
    private boolean disponible;
    private String fechaCreacion;
    private String estado;
    private int totalProductos;
    private List<ChefBoxProducto> productos;

    public ChefBox(int id, String nombre, String descripcion, double precio,
                   boolean disponible, String fechaCreacion, String estado, int totalProductos) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.totalProductos = totalProductos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getTotalProductos() { return totalProductos; }
    public void setTotalProductos(int totalProductos) { this.totalProductos = totalProductos; }
    public List<ChefBoxProducto> getProductos() { return productos; }
    public void setProductos(List<ChefBoxProducto> productos) { this.productos = productos; }

    public static class ChefBoxProducto {
        private int idProducto;
        private String nombreProducto;
        private int cantidad;
        private double precioUnitario;

        public ChefBoxProducto(int idProducto, String nombreProducto, int cantidad, double precioUnitario) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public int getIdProducto() { return idProducto; }
        public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
        public String getNombreProducto() { return nombreProducto; }
        public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
        public double getSubtotal() { return cantidad * precioUnitario; }
    }
}
