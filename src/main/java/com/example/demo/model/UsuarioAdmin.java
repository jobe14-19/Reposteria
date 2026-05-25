package com.example.demo.model;

public class UsuarioAdmin {
 private int id;
 private String usuario;
 private String contrasena;
 private String nombre;
 private String perfil;
 private String estado;
 private String fechaRegistro;

 public UsuarioAdmin() {}

 public UsuarioAdmin(int id, String usuario, String nombre, String perfil, String estado, String fechaRegistro) {
 this.id = id;
 this.usuario = usuario;
 this.nombre = nombre;
 this.perfil = perfil;
 this.estado = estado;
 this.fechaRegistro = fechaRegistro;
 }

 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public String getUsuario() { return usuario; }
 public void setUsuario(String usuario) { this.usuario = usuario; }
 public String getContrasena() { return contrasena; }
 public void setContrasena(String contrasena) { this.contrasena = contrasena; }
 public String getNombre() { return nombre; }
 public void setNombre(String nombre) { this.nombre = nombre; }
 public String getPerfil() { return perfil; }
 public void setPerfil(String perfil) { this.perfil = perfil; }
 public String getEstado() { return estado; }
 public void setEstado(String estado) { this.estado = estado; }
 public String getFechaRegistro() { return fechaRegistro; }
 public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
