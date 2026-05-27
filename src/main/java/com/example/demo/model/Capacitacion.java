package com.example.demo.model;

public class Capacitacion {
    private int id;
    private int idEmpleado;
    private String tema;
    private String fecha;
    private double duracion;
    private String capacitador;
    private int usuarioRegistra;

    public Capacitacion() {}

    public Capacitacion(int id, int idEmpleado, String tema, String fecha, double duracion, String capacitador, int usuarioRegistra) {
        this.id = id;
        this.idEmpleado = idEmpleado;
        this.tema = tema;
        this.fecha = fecha;
        this.duracion = duracion;
        this.capacitador = capacitador;
        this.usuarioRegistra = usuarioRegistra;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public double getDuracion() { return duracion; }
    public void setDuracion(double duracion) { this.duracion = duracion; }
    public String getCapacitador() { return capacitador; }
    public void setCapacitador(String capacitador) { this.capacitador = capacitador; }
    public int getUsuarioRegistra() { return usuarioRegistra; }
    public void setUsuarioRegistra(int usuarioRegistra) { this.usuarioRegistra = usuarioRegistra; }
}
