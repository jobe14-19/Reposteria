package com.example.demo.model;

public class Empleado {
 private int id;
 private String nombre;
 private String cedula;
 private String telefono;
 private String area;
 private String estado;
 private String capacitacion;

 public Empleado(int id, String nombre, String cedula, String telefono,
 String area, String estado, String capacitacion) {
 this.id = id;
 this.nombre = nombre;
 this.cedula = cedula;
 this.telefono = telefono;
 this.area = area;
 this.estado = estado;
 this.capacitacion = capacitacion;
 }

 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public String getNombre() { return nombre; }
 public void setNombre(String nombre) { this.nombre = nombre; }
 public String getCedula() { return cedula; }
 public void setCedula(String cedula) { this.cedula = cedula; }
 public String getTelefono() { return telefono; }
 public void setTelefono(String telefono) { this.telefono = telefono; }
 public String getArea() { return area; }
 public void setArea(String area) { this.area = area; }
 public String getEstado() { return estado; }
 public void setEstado(String estado) { this.estado = estado; }
 public String getCapacitacion() { return capacitacion; }
 public void setCapacitacion(String capacitacion) { this.capacitacion = capacitacion; }

 public boolean esActivo() { return "Activo".equalsIgnoreCase(estado); }
 public boolean estaVacaciones() { return "Vacaciones".equalsIgnoreCase(estado); }
 public boolean estaInactivo() { return "Inactivo".equalsIgnoreCase(estado); }
 public boolean tieneCapacitacionCompleta() { return "Completo".equalsIgnoreCase(capacitacion); }
 public boolean tieneCapacitacionParcial() { return "Parcial".equalsIgnoreCase(capacitacion); }
 public boolean tieneCapacitacionPendiente() { return "Pendiente".equalsIgnoreCase(capacitacion); }
}
