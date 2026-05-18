package com.example.demo.service;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static SessionManager instance;

    // Datos de sesión
    private int idUsuario;
    private String nombreUsuario;
    private String perfil;
    private String area;

    // Constantes de perfil
    public static final String PERFIL_CLIENTE = "CLIENTE";
    public static final String PERFIL_EMPLEADO = "EMPLEADO";
    public static final String PERFIL_ADMIN = "ADMIN";

    // Constantes de Area
    public static final String AREA_PRODUCCION = "Producción";
    public static final String AREA_DECORACION = "Decoración";
    public static final String AREA_DELIVERY = "Delivery";
    public static final String AREA_VENTAS = "Ventas";
    public static final String AREA_ATENCION_CLIENTE = "Atención al Cliente";
    public static final String AREA_LIMPIEZA = "Limpieza";
    public static final String AREA_ADMINISTRACION = "Administración";

    // Constructor privado
    private SessionManager() {
        limpiarSesion();
    }

    // Singleton
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Iniciar sesión
    public void iniciarSesion(int id, String nombre, String perfil) {
        iniciarSesion(id, nombre, perfil, "");
    }

    public void iniciarSesion(int id, String nombre, String perfil, String area) {
        if (!esPerfilValido(perfil)) {
            LOGGER.log(Level.WARNING, "Perfil inválido: {0}", perfil);
            throw new IllegalArgumentException("Perfil inválido: " + perfil);
        }

        if (id <= 0) {
            LOGGER.log(Level.WARNING, "ID inválido: {0}", id);
            throw new IllegalArgumentException("ID de usuario inválido");
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Nombre de usuario inválido");
            throw new IllegalArgumentException("Nombre de usuario inválido");
        }

        this.idUsuario = id;
        this.nombreUsuario = nombre;
        this.perfil = perfil;
        this.area = area != null ? area : "";

        LOGGER.log(Level.INFO, "Sesión iniciada - Usuario: {0} (ID: {1}, Perfil: {2}, Area: {3})",
                new Object[]{nombre, id, perfil, this.area});
    }

    // Cerrar sesión
    public void cerrarSesion() {
        if (isLoggedIn()) {
            LOGGER.log(Level.INFO, "Cerrando sesión de: {0}", nombreUsuario);
        }
        limpiarSesion();
    }

    private void limpiarSesion() {
        this.idUsuario = 0;
        this.nombreUsuario = "";
        this.perfil = "";
        this.area = "";
    }

    // Getters
    public int getIdUsuarioActual() { return idUsuario; }
    public String getUsuarioActual() { return nombreUsuario; }
    public String getPerfilActual() { return perfil; }
    public String getAreaActual() { return area; }

    // Verificadores
    public boolean isLoggedIn() {
        return idUsuario > 0 && !nombreUsuario.trim().isEmpty() && !perfil.trim().isEmpty();
    }

    public boolean isCliente() {
        return isLoggedIn() && PERFIL_CLIENTE.equals(perfil);
    }

    public boolean isEmpleado() {
        return isLoggedIn() && PERFIL_EMPLEADO.equals(perfil);
    }

    public boolean isAdmin() {
        return isLoggedIn() && PERFIL_ADMIN.equals(perfil);
    }

    public boolean isAreaProduccion() {
        return isEmpleado() && (AREA_PRODUCCION.equals(area) || AREA_DECORACION.equals(area));
    }

    public boolean isAreaDelivery() {
        return isEmpleado() && AREA_DELIVERY.equals(area);
    }

    public boolean isAreaLimpieza() {
        return isEmpleado() && AREA_LIMPIEZA.equals(area);
    }

    public boolean isAreaVentas() {
        return isEmpleado() && AREA_VENTAS.equals(area);
    }

    public boolean isAreaAtencionCliente() {
        return isEmpleado() && AREA_ATENCION_CLIENTE.equals(area);
    }

    public boolean isAreaAdministracion() {
        return isEmpleado() && AREA_ADMINISTRACION.equals(area);
    }

    private boolean esPerfilValido(String perfil) {
        return PERFIL_CLIENTE.equals(perfil) || PERFIL_EMPLEADO.equals(perfil) || PERFIL_ADMIN.equals(perfil);
    }

    public String getSessionInfo() {
        if (!isLoggedIn()) {
            return "No hay sesión activa";
        }
        return String.format("Usuario: %s (ID: %d, Perfil: %s, Area: %s)", nombreUsuario, idUsuario, perfil, area);
    }
}
