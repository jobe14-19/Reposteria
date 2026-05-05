package com.example.demo;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static SessionManager instance;

    // Datos de sesión
    private int idUsuario;
    private String nombreUsuario;
    private String perfil;

    // Constantes de perfil
    public static final String PERFIL_CLIENTE = "CLIENTE";
    public static final String PERFIL_EMPLEADO = "EMPLEADO";
    public static final String PERFIL_ADMIN = "ADMIN";

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

        LOGGER.log(Level.INFO, "Sesión iniciada - Usuario: {0} (ID: {1}, Perfil: {2})",
                new Object[]{nombre, id, perfil});
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
    }

    // Getters
    public int getIdUsuarioActual() { return idUsuario; }
    public String getUsuarioActual() { return nombreUsuario; }
    public String getPerfilActual() { return perfil; }

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

    private boolean esPerfilValido(String perfil) {
        return PERFIL_CLIENTE.equals(perfil) || PERFIL_EMPLEADO.equals(perfil) || PERFIL_ADMIN.equals(perfil);
    }

    public String getSessionInfo() {
        if (!isLoggedIn()) {
            return "No hay sesión activa";
        }
        return String.format("Usuario: %s (ID: %d, Perfil: %s)", nombreUsuario, idUsuario, perfil);
    }
}