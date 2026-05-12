package com.example.demo.model;

public class Cliente {
    private int id;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private String direccion;
    private String rnc;
    private String usuario;
    private String contrasena;
    private String estado;
    private String fechaRegistro;
    private int totalPedidos;

    // Constructor completo
    public Cliente(int id, String nombre, String apellido, String telefono, String email, String direccion, 
                   String rnc, String usuario, String contrasena, String estado, String fechaRegistro, int totalPedidos) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.rnc = rnc;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.totalPedidos = totalPedidos;
    }

    // Constructor simplificado (para vistas de tabla)
    public Cliente(int id, String nombre, String apellido, String telefono, String email, String fechaRegistro, int totalPedidos) {
        this(id, nombre, apellido, telefono, email, "", "", "", "", "Activo", fechaRegistro, totalPedidos);
    }
    
    // Constructor para modales (sin total pedidos y fecha registro)
    public Cliente(int id, String nombre, String apellido, String telefono, String email,
                   String direccion, String rnc, String usuario, String contrasena) {
        this(id, nombre, apellido, telefono, email, direccion, rnc, usuario, contrasena, "Activo", "", 0);
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getRnc() { return rnc; }
    public void setRnc(String rnc) { this.rnc = rnc; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public int getTotalPedidos() { return totalPedidos; }
    public void setTotalPedidos(int totalPedidos) { this.totalPedidos = totalPedidos; }

    public String nombreCompleto() { return nombre + (apellido != null && !apellido.isEmpty() ? " " + apellido : ""); }
}
