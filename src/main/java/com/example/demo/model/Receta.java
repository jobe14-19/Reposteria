package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class Receta {
    private int id;
    private int idProducto;
    private String nombreProducto;
    private String nombreReceta;
    private String descripcion;
    private String categoria;
    private int tiempoPreparacion;
    private double cantidadProducida;
    private String imagenRef;
    private double porciones;
    private double costoEstimado;
    private double rendimiento;
    private double desperdicio;
    private String estado;
    private List<RecetaIngrediente> ingredientes;
    private List<PasoReceta> pasos;

    public Receta() {
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
    }

    public Receta(int id, int idProducto, String nombreProducto, String nombreReceta,
                  String descripcion, String categoria, int tiempoPreparacion,
                  double cantidadProducida, String imagenRef, double porciones,
                  double costoEstimado, double rendimiento, double desperdicio, String estado) {
        this.id = id;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.nombreReceta = nombreReceta;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.tiempoPreparacion = tiempoPreparacion;
        this.cantidadProducida = cantidadProducida;
        this.imagenRef = imagenRef;
        this.porciones = porciones;
        this.costoEstimado = costoEstimado;
        this.rendimiento = rendimiento;
        this.desperdicio = desperdicio;
        this.estado = estado;
        this.ingredientes = new ArrayList<>();
        this.pasos = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getNombreReceta() { return nombreReceta; }
    public void setNombreReceta(String nombreReceta) { this.nombreReceta = nombreReceta; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public int getTiempoPreparacion() { return tiempoPreparacion; }
    public void setTiempoPreparacion(int tiempoPreparacion) { this.tiempoPreparacion = tiempoPreparacion; }
    public double getCantidadProducida() { return cantidadProducida; }
    public void setCantidadProducida(double cantidadProducida) { this.cantidadProducida = cantidadProducida; }
    public String getImagenRef() { return imagenRef; }
    public void setImagenRef(String imagenRef) { this.imagenRef = imagenRef; }
    public double getPorciones() { return porciones; }
    public void setPorciones(double porciones) { this.porciones = porciones; }
    public double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(double costoEstimado) { this.costoEstimado = costoEstimado; }
    public double getRendimiento() { return rendimiento; }
    public void setRendimiento(double rendimiento) { this.rendimiento = rendimiento; }
    public double getDesperdicio() { return desperdicio; }
    public void setDesperdicio(double desperdicio) { this.desperdicio = desperdicio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<RecetaIngrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<RecetaIngrediente> ingredientes) { this.ingredientes = ingredientes; }
    public List<PasoReceta> getPasos() { return pasos; }
    public void setPasos(List<PasoReceta> pasos) { this.pasos = pasos; }
    public int getTotalIngredientes() { return ingredientes.size(); }
    public int getTotalPasos() { return pasos.size(); }
    public String getTiempoStr() {
        if (tiempoPreparacion < 60) return tiempoPreparacion + " min";
        return (tiempoPreparacion / 60) + "h " + (tiempoPreparacion % 60) + "min";
    }

    public String getIngredientesStr() {
        if (ingredientes == null || ingredientes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ingredientes.size(); i++) {
            if (i > 0) sb.append(", ");
            RecetaIngrediente ing = ingredientes.get(i);
            sb.append(ing.getNombreIngrediente()).append(" ").append(ing.getCantidad()).append(ing.getUnidad());
        }
        return sb.toString();
    }

    public static class RecetaIngrediente {
        private int idIngrediente;
        private String nombreIngrediente;
        private double cantidad;
        private String unidad;

        public RecetaIngrediente() {}
        public RecetaIngrediente(int idIngrediente, String nombreIngrediente, double cantidad, String unidad) {
            this.idIngrediente = idIngrediente;
            this.nombreIngrediente = nombreIngrediente;
            this.cantidad = cantidad;
            this.unidad = unidad;
        }
        public int getIdIngrediente() { return idIngrediente; }
        public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }
        public String getNombreIngrediente() { return nombreIngrediente; }
        public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
        public double getCantidad() { return cantidad; }
        public void setCantidad(double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }

    public static class PasoReceta {
        private int idPaso;
        private int numeroPaso;
        private String titulo;
        private String descripcion;
        private int tiempoEstimado;
        private String imagenRef;

        public PasoReceta() {}
        public PasoReceta(int numeroPaso, String titulo, String descripcion, int tiempoEstimado, String imagenRef) {
            this.numeroPaso = numeroPaso;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.tiempoEstimado = tiempoEstimado;
            this.imagenRef = imagenRef;
        }
        public int getIdPaso() { return idPaso; }
        public void setIdPaso(int idPaso) { this.idPaso = idPaso; }
        public int getNumeroPaso() { return numeroPaso; }
        public void setNumeroPaso(int numeroPaso) { this.numeroPaso = numeroPaso; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public int getTiempoEstimado() { return tiempoEstimado; }
        public void setTiempoEstimado(int tiempoEstimado) { this.tiempoEstimado = tiempoEstimado; }
        public String getImagenRef() { return imagenRef; }
        public void setImagenRef(String imagenRef) { this.imagenRef = imagenRef; }
        public String getTiempoStr() {
            if (tiempoEstimado < 60) return tiempoEstimado + " min";
            return (tiempoEstimado / 60) + "h " + (tiempoEstimado % 60) + "min";
        }
    }
}
