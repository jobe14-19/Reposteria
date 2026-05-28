package com.example.demo.model;

public class Maquina {
    private int id;
    private String nombre;
    private String utilidad;
    private String estado;
    private String ultimoMantenimiento;
    private String proximoMantenimiento;

    public Maquina() {}

    public Maquina(int id, String nombre, String utilidad, String estado, String ultimoMantenimiento, String proximoMantenimiento) {
        this.id = id;
        this.nombre = nombre;
        this.utilidad = utilidad;
        this.estado = estado;
        this.ultimoMantenimiento = ultimoMantenimiento;
        this.proximoMantenimiento = proximoMantenimiento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUtilidad() { return utilidad; }
    public void setUtilidad(String utilidad) { this.utilidad = utilidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getUltimoMantenimiento() { return ultimoMantenimiento; }
    public void setUltimoMantenimiento(String ultimoMantenimiento) { this.ultimoMantenimiento = ultimoMantenimiento; }
    public String getProximoMantenimiento() { return proximoMantenimiento; }
    public void setProximoMantenimiento(String proximoMantenimiento) { this.proximoMantenimiento = proximoMantenimiento; }
}
