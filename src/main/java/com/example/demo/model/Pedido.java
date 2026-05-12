package com.example.demo.model;

public class Pedido {
    private int id;
    private String nombreCliente;
    private String fechaPedido;
    private String fechaEntrega;
    private String producto;
    private double libras;
    private double total;
    private double adelanto;
    private String estado;
    
    // Constantes para estados
    public static final String ESTADO_PENDIENTE = "Pendiente";
    public static final String ESTADO_CONFIRMADO = "Confirmado";
    public static final String ESTADO_EN_PRODUCCION = "En producción";
    public static final String ESTADO_LISTO = "Listo";
    public static final String ESTADO_ENTREGADO = "Entregado";
    public static final String ESTADO_CANCELADO = "Cancelado";

    public Pedido(int id, String nombreCliente, String fechaPedido, String fechaEntrega,
                  String producto, double libras, double total, double adelanto, String estado) {
        this.id = id;
        this.nombreCliente = nombreCliente;
        this.fechaPedido = fechaPedido;
        this.fechaEntrega = fechaEntrega;
        this.producto = producto;
        this.libras = libras;
        this.total = total;
        this.adelanto = adelanto;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(String fechaPedido) { this.fechaPedido = fechaPedido; }
    public String getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(String fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public double getLibras() { return libras; }
    public void setLibras(double libras) { this.libras = libras; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getAdelanto() { return adelanto; }
    public void setAdelanto(double adelanto) { this.adelanto = adelanto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean estaPendiente() { return ESTADO_PENDIENTE.equalsIgnoreCase(estado); }
    public boolean estaConfirmado() { return ESTADO_CONFIRMADO.equalsIgnoreCase(estado); }
    public boolean estaEnProduccion() { return ESTADO_EN_PRODUCCION.equalsIgnoreCase(estado); }
    public boolean estaListo() { return ESTADO_LISTO.equalsIgnoreCase(estado); }
    public boolean estaEntregado() { return ESTADO_ENTREGADO.equalsIgnoreCase(estado); }
    public boolean estaCancelado() { return ESTADO_CANCELADO.equalsIgnoreCase(estado); }

    public double getSaldo() { return total - adelanto; }
    public boolean tieneSaldoPendiente() { return getSaldo() > 0; }
    public double getPorcentajePagado() { return total > 0 ? (adelanto / total) * 100 : 0; }
}
