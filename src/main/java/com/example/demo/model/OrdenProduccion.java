package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenProduccion {
    private int id;
    private String numeroOrden;
    private String estado;
    private String categoria, revestimiento, sucursal;
    private String fechaEntrega, horaEntrega;
    private String cliente, direccion, telefono, vendedor;
    private double libras;
    private String baseTipo, masoTipo, forma;
    private int pisos;
    private String lustres, decoracion, camuflajes, flores;
    private String mensaje, observaciones, adornos, rellenos;
    private double costoEstimado, costoReal, precioVenta, anticipo, saldo;
    private int idReceta;
    private String nombreReceta;
    private LocalDateTime fechaCreacion, fechaInicio, fechaCompletado;
    private String usuarioCrea;
    private int progreso;
    private boolean pausado;
    private String tipoEntrega;
    private double costoDelivery;

    private List<OrdenFase> fases;
    private List<OrdenHistorial> historial;
    private List<OrdenIngrediente> ingredientes;

    public OrdenProduccion() {
        this.fases = new ArrayList<>();
        this.historial = new ArrayList<>();
        this.ingredientes = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumeroOrden() { return numeroOrden; }
    public void setNumeroOrden(String numeroOrden) { this.numeroOrden = numeroOrden; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getRevestimiento() { return revestimiento; }
    public void setRevestimiento(String revestimiento) { this.revestimiento = revestimiento; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }
    public String getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(String fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public String getHoraEntrega() { return horaEntrega; }
    public void setHoraEntrega(String horaEntrega) { this.horaEntrega = horaEntrega; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getVendedor() { return vendedor; }
    public void setVendedor(String vendedor) { this.vendedor = vendedor; }
    public double getLibras() { return libras; }
    public void setLibras(double libras) { this.libras = libras; }
    public String getBaseTipo() { return baseTipo; }
    public void setBaseTipo(String baseTipo) { this.baseTipo = baseTipo; }
    public String getMaso() { return masoTipo; }
    public void setMaso(String masoTipo) { this.masoTipo = masoTipo; }
    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
    public int getPisos() { return pisos; }
    public void setPisos(int pisos) { this.pisos = pisos; }
    public String getLustres() { return lustres; }
    public void setLustres(String lustres) { this.lustres = lustres; }
    public String getDecoracion() { return decoracion; }
    public void setDecoracion(String decoracion) { this.decoracion = decoracion; }
    public String getCamuflajes() { return camuflajes; }
    public void setCamuflajes(String camuflajes) { this.camuflajes = camuflajes; }
    public String getFlores() { return flores; }
    public void setFlores(String flores) { this.flores = flores; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getAdornos() { return adornos; }
    public void setAdornos(String adornos) { this.adornos = adornos; }
    public String getRellenos() { return rellenos; }
    public void setRellenos(String rellenos) { this.rellenos = rellenos; }
    public double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(double costoEstimado) { this.costoEstimado = costoEstimado; }
    public double getCostoReal() { return costoReal; }
    public void setCostoReal(double costoReal) { this.costoReal = costoReal; }
    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    public double getAnticipo() { return anticipo; }
    public void setAnticipo(double anticipo) { this.anticipo = anticipo; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public int getIdReceta() { return idReceta; }
    public void setIdReceta(int idReceta) { this.idReceta = idReceta; }
    public String getNombreReceta() { return nombreReceta; }
    public void setNombreReceta(String nombreReceta) { this.nombreReceta = nombreReceta; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaCompletado() { return fechaCompletado; }
    public void setFechaCompletado(LocalDateTime fechaCompletado) { this.fechaCompletado = fechaCompletado; }
    public String getUsuarioCrea() { return usuarioCrea; }
    public void setUsuarioCrea(String usuarioCrea) { this.usuarioCrea = usuarioCrea; }
    public int getProgreso() { return progreso; }
    public void setProgreso(int progreso) { this.progreso = progreso; }
    public boolean isPausado() { return pausado; }
    public void setPausado(boolean pausado) { this.pausado = pausado; }
    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }
    public double getCostoDelivery() { return costoDelivery; }
    public void setCostoDelivery(double costoDelivery) { this.costoDelivery = costoDelivery; }
    public List<OrdenFase> getFases() { return fases; }
    public void setFases(List<OrdenFase> fases) { this.fases = fases; }
    public List<OrdenHistorial> getHistorial() { return historial; }
    public void setHistorial(List<OrdenHistorial> historial) { this.historial = historial; }
    public List<OrdenIngrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<OrdenIngrediente> ingredientes) { this.ingredientes = ingredientes; }
    public String getFechaCreacionStr() {
        return fechaCreacion != null ? fechaCreacion.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
    }
    public String getFechaInicioStr() {
        return fechaInicio != null ? fechaInicio.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
    }
    public String getEstadoBadge() {
        switch (estado != null ? estado : "") {
            case "ACTIVA": return "background-color: #007BFF; color: white;";
            case "EN PRODUCCION": return "background-color: #FF9800; color: white;";
            case "COMPLETADA": return "background-color: #28A745; color: white;";
            case "ENTREGADA": return "background-color: #6C757D; color: white;";
            case "CANCELADA": return "background-color: #DC3545; color: white;";
            default: return "background-color: #6C757D; color: white;";
        }
    }

    public static class OrdenFase {
        private int idFase, idOrden, faseOrden;
        private String faseNombre, estado, fechaInicio, fechaFin;
        private String usuarioInicia, usuarioCompleta, observaciones;

        public OrdenFase() {}
        public OrdenFase(int faseOrden, String faseNombre) {
            this.faseOrden = faseOrden;
            this.faseNombre = faseNombre;
            this.estado = "PENDIENTE";
        }
        public int getIdFase() { return idFase; }
        public void setIdFase(int idFase) { this.idFase = idFase; }
        public int getIdOrden() { return idOrden; }
        public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
        public int getFaseOrden() { return faseOrden; }
        public void setFaseOrden(int faseOrden) { this.faseOrden = faseOrden; }
        public String getFaseNombre() { return faseNombre; }
        public void setFaseNombre(String faseNombre) { this.faseNombre = faseNombre; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
        public String getFechaFin() { return fechaFin; }
        public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
        public String getUsuarioInicia() { return usuarioInicia; }
        public void setUsuarioInicia(String usuarioInicia) { this.usuarioInicia = usuarioInicia; }
        public String getUsuarioCompleta() { return usuarioCompleta; }
        public void setUsuarioCompleta(String usuarioCompleta) { this.usuarioCompleta = usuarioCompleta; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
        public String getEstadoBadge() {
            switch (estado != null ? estado : "") {
                case "EN CURSO": return "-fx-background-color: #FF9800; -fx-text-fill: white;";
                case "COMPLETADA": return "-fx-background-color: #28A745; -fx-text-fill: white;";
                default: return "-fx-background-color: #6C757D; -fx-text-fill: white;";
            }
        }
    }

    public static class OrdenHistorial {
        private int idHistorial, idOrden;
        private String accion, detalle, usuario;
        private LocalDateTime fechaHora;

        public OrdenHistorial() {}
        public OrdenHistorial(String accion, String detalle, String usuario) {
            this.accion = accion;
            this.detalle = detalle;
            this.usuario = usuario;
        }
        public int getIdHistorial() { return idHistorial; }
        public void setIdHistorial(int idHistorial) { this.idHistorial = idHistorial; }
        public int getIdOrden() { return idOrden; }
        public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
        public String getAccion() { return accion; }
        public void setAccion(String accion) { this.accion = accion; }
        public String getDetalle() { return detalle; }
        public void setDetalle(String detalle) { this.detalle = detalle; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public LocalDateTime getFechaHora() { return fechaHora; }
        public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
        public String getFechaHoraStr() {
            return fechaHora != null ? fechaHora.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
        }
    }

    public static class OrdenIngrediente {
        private int id, idOrden, idIngrediente;
        private String nombreIngrediente, unidad;
        private double cantidadRequerida, cantidadDescontada;
        private boolean descontado;

        public OrdenIngrediente() {}
        public OrdenIngrediente(int idIngrediente, String nombre, String unidad, double cantidad) {
            this.idIngrediente = idIngrediente;
            this.nombreIngrediente = nombre;
            this.unidad = unidad;
            this.cantidadRequerida = cantidad;
        }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getIdOrden() { return idOrden; }
        public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
        public int getIdIngrediente() { return idIngrediente; }
        public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }
        public String getNombreIngrediente() { return nombreIngrediente; }
        public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public double getCantidadRequerida() { return cantidadRequerida; }
        public void setCantidadRequerida(double cantidadRequerida) { this.cantidadRequerida = cantidadRequerida; }
        public double getCantidadDescontada() { return cantidadDescontada; }
        public void setCantidadDescontada(double cantidadDescontada) { this.cantidadDescontada = cantidadDescontada; }
        public boolean isDescontado() { return descontado; }
        public void setDescontado(boolean descontado) { this.descontado = descontado; }
    }
}
