package com.example.demo.model;

public class Actividad {
    private int idActividad;
    private String fechaHora;
    private String usuario;
    private String accion;
    private String detalle;

    public Actividad() {}

    public Actividad(int idActividad, String fechaHora, String usuario, String accion, String detalle) {
        this.idActividad = idActividad;
        this.fechaHora = fechaHora;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
    }

    public int getIdActividad() { return idActividad; }
    public void setIdActividad(int idActividad) { this.idActividad = idActividad; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
