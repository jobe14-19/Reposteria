package com.example.demo.model;

public class EntregaHistorial {
 private int id;
 private String nombreCliente;
 private String fechaEntrega;
 private String tipo;
 private double total;
 private double pagado;
 private String metodoPago;

 public EntregaHistorial(int id, String nombreCliente, String fechaEntrega,
 String tipo, double total, double pagado, String metodoPago) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.fechaEntrega = fechaEntrega;
 this.tipo = tipo;
 this.total = total;
 this.pagado = pagado;
 this.metodoPago = metodoPago;
 }

 public int getId() { return id; }
 public void setId(int id) { this.id = id; }
 public String getNombreCliente() { return nombreCliente; }
 public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
 public String getFechaEntrega() { return fechaEntrega; }
 public void setFechaEntrega(String fechaEntrega) { this.fechaEntrega = fechaEntrega; }
 public String getTipo() { return tipo; }
 public void setTipo(String tipo) { this.tipo = tipo; }
 public double getTotal() { return total; }
 public void setTotal(double total) { this.total = total; }
 public double getPagado() { return pagado; }
 public void setPagado(double pagado) { this.pagado = pagado; }
 public String getMetodoPago() { return metodoPago; }
 public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

 public boolean estaPagadoCompleto() { return pagado >= total; }
 public double getSaldoPendiente() { return total - pagado; }
 public boolean esLocal() { return "Local".equals(tipo); }
 public boolean esDelivery() { return "Delivery".equals(tipo); }
}
