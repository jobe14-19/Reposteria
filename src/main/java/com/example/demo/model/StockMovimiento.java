package com.example.demo.model;

import java.time.LocalDateTime;

public class StockMovimiento {
    private int id;
    private int idIngrediente;
    private String nombreIngrediente;
    private String tipoMovimiento;
    private double cantidad;
    private double stockAnterior;
    private double stockNuevo;
    private String motivo;
    private String referenciaTipo;
    private int referenciaId;
    private String usuarioRegistra;
    private LocalDateTime fechaHora;

    public StockMovimiento() {}

    public StockMovimiento(int idIngrediente, String nombreIngrediente, String tipoMovimiento,
                           double cantidad, double stockAnterior, double stockNuevo,
                           String motivo, String referenciaTipo, int referenciaId, String usuarioRegistra) {
        this.idIngrediente = idIngrediente;
        this.nombreIngrediente = nombreIngrediente;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.usuarioRegistra = usuarioRegistra;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdIngrediente() { return idIngrediente; }
    public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }
    public String getNombreIngrediente() { return nombreIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public double getStockAnterior() { return stockAnterior; }
    public void setStockAnterior(double stockAnterior) { this.stockAnterior = stockAnterior; }
    public double getStockNuevo() { return stockNuevo; }
    public void setStockNuevo(double stockNuevo) { this.stockNuevo = stockNuevo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }
    public int getReferenciaId() { return referenciaId; }
    public void setReferenciaId(int referenciaId) { this.referenciaId = referenciaId; }
    public String getUsuarioRegistra() { return usuarioRegistra; }
    public void setUsuarioRegistra(String usuarioRegistra) { this.usuarioRegistra = usuarioRegistra; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getFechaHoraStr() {
        return fechaHora != null ? fechaHora.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
    }
    public String getStockDiffStr() {
        double diff = stockNuevo - stockAnterior;
        return (diff >= 0 ? "+" : "") + String.format("%.2f", diff);
    }
}
