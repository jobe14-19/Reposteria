package com.example.demo.model;

public class Ingrediente {
 private int id;
 private String nombre;
 private String categoria;
 private String unidad;
 private double stockActual;
 private double stockMinimo;
 private String estado;

 public Ingrediente(int id, String nombre, String categoria, String unidad,
 double stockActual, double stockMinimo, String estado) {
 this.id = id;
 this.nombre = nombre;
 this.categoria = categoria;
 this.unidad = unidad;
 this.stockActual = stockActual;
 this.stockMinimo = stockMinimo;
 this.estado = estado;
 }

 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public String getNombre() { return nombre; }
 public void setNombre(String nombre) { this.nombre = nombre; }
 public String getCategoria() { return categoria; }
 public void setCategoria(String categoria) { this.categoria = categoria; }
 public String getUnidad() { return unidad; }
 public void setUnidad(String unidad) { this.unidad = unidad; }
 public double getStockActual() { return stockActual; }
 public void setStockActual(double stockActual) { this.stockActual = stockActual; }
 public double getStockMinimo() { return stockMinimo; }
 public void setStockMinimo(double stockMinimo) { this.stockMinimo = stockMinimo; }
 public String getEstado() { return estado; }
 public void setEstado(String estado) { this.estado = estado; }

 public boolean isCritico() { return "Crítico".equalsIgnoreCase(estado); }
 public boolean isBajo() { return "Bajo".equalsIgnoreCase(estado); }
 public boolean isNormal() { return "Normal".equalsIgnoreCase(estado); }
 public double getDiferenciaStock() { return stockMinimo - stockActual; }
}
