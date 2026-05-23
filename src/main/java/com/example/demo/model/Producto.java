package com.example.demo.model;

public class Producto {
    private int id;
    private String nombre;
    private double precioBase;
    private double precioUnitario;
    private double costoDisenio;
    private String descripcion;
    private String estado;
    private int totalRecetas;

    public Producto() {}

    public Producto(int id, String nombre, double precioBase, double precioUnitario,
                    double costoDisenio, String descripcion, String estado, int totalRecetas) {
        this.id = id;
        this.nombre = nombre;
        this.precioBase = precioBase;
        this.precioUnitario = precioUnitario;
        this.costoDisenio = costoDisenio;
        this.descripcion = descripcion;
        this.estado = estado;
        this.totalRecetas = totalRecetas;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public double getCostoDisenio() { return costoDisenio; }
    public void setCostoDisenio(double costoDisenio) { this.costoDisenio = costoDisenio; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getTotalRecetas() { return totalRecetas; }
    public void setTotalRecetas(int totalRecetas) { this.totalRecetas = totalRecetas; }
}
