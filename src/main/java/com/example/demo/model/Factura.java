package com.example.demo.model;

public class Factura {
    private int id;
    private int idOrden;
    private String cliente;
    private String telefono;
    private String direccion;
    private String fecha;
    private double subtotal;
    private double costoDelivery;
    private double itbis;
    private double descuento;
    private double total;
    private String estado;
    private String detalles;
    private String usuarioGenera;
    private String fechaGeneracion;
    private String metodoPago;
    private String pagado;

    public Factura() {}

    public Factura(int id, String cliente, String telefono, String direccion, String fecha,
                   double subtotal, double costoDelivery, double itbis, double descuento, double total,
                   String estado, String metodoPago, String pagado) {
        this.id = id;
        this.cliente = cliente;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fecha = fecha;
        this.subtotal = subtotal;
        this.costoDelivery = costoDelivery;
        this.itbis = itbis;
        this.descuento = descuento;
        this.total = total;
        this.estado = estado;
        this.metodoPago = metodoPago;
        this.pagado = pagado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getCostoDelivery() { return costoDelivery; }
    public void setCostoDelivery(double costoDelivery) { this.costoDelivery = costoDelivery; }
    public double getItbis() { return itbis; }
    public void setItbis(double itbis) { this.itbis = itbis; }
    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }
    public String getUsuarioGenera() { return usuarioGenera; }
    public void setUsuarioGenera(String usuarioGenera) { this.usuarioGenera = usuarioGenera; }
    public String getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(String fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getPagado() { return pagado; }
    public void setPagado(String pagado) { this.pagado = pagado; }
}
