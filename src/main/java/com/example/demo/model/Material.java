package com.example.demo.model;

public class Material {
    private int id;
    private String nombre;
    private String unidad;
    private int stockActual;
    private int stockMinimo;

    public Material() {}

    public Material(int id, String nombre, String unidad, int stockActual, int stockMinimo) {
        this.id = id;
        this.nombre = nombre;
        this.unidad = unidad;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    @Override
    public String toString() {
        return nombre + " (" + stockActual + " " + unidad + ")";
    }
}
