package com.example.demo.model;

public class PedidoPendiente {
 private int id;
 private String nombreCliente;
 private String direccion;
 private double total;
 private double adelanto;
 private double saldo;
 private String tipo;

 public PedidoPendiente(int id, String nombreCliente, String direccion,
 double total, double adelanto, double saldo, String tipo) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.direccion = direccion;
 this.total = total;
 this.adelanto = adelanto;
 this.saldo = saldo;
 this.tipo = tipo;
 }

 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public String getNombreCliente() { return nombreCliente; }
 public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
 public String getDireccion() { return direccion; }
 public void setDireccion(String direccion) { this.direccion = direccion; }
 public double getTotal() { return total; }
 public void setTotal(double total) { this.total = total; }
 public double getAdelanto() { return adelanto; }
 public void setAdelanto(double adelanto) { this.adelanto = adelanto; }
 public double getSaldo() { return saldo; }
 public void setSaldo(double saldo) { this.saldo = saldo; }
 public String getTipo() { return tipo; }
 public void setTipo(String tipo) { this.tipo = tipo; }

 public boolean esLocal() { return "Local".equals(tipo); }
 public boolean esDelivery() { return "Delivery".equals(tipo); }
 public boolean tieneSaldo() { return saldo > 0; }
}
