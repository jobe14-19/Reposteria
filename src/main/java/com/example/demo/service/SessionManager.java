package com.example.demo.service;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

 private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

 private static SessionManager instance;

 private int idUsuario;
 private String nombreUsuario;
 private String perfil;
 private String area;

 // Constantes de perfil (roles)
 public static final String PERFIL_CLIENTE = "CLIENTE";
 public static final String PERFIL_ADMIN = "ADMIN";
 public static final String PERFIL_RECEPCION = "RECEPCION";
 public static final String PERFIL_PLANIFICADOR = "PLANIFICADOR";
 public static final String PERFIL_ALMACEN = "ALMACEN";
 public static final String PERFIL_PRODUCCION = "PRODUCCION";
 public static final String PERFIL_DECORACION = "DECORACION";
 public static final String PERFIL_CONTABILIDAD = "CONTABILIDAD";
 public static final String PERFIL_REPARTIDOR = "REPARTIDOR";
 public static final String PERFIL_RRHH = "RRHH";
 public static final String PERFIL_AUDITOR = "AUDITOR";

 // Constantes de Area (para compatibilidad)
 public static final String AREA_PRODUCCION = "Producción";
 public static final String AREA_DECORACION = "Decoración";
 public static final String AREA_DELIVERY = "Delivery";
 public static final String AREA_VENTAS = "Ventas";
 public static final String AREA_ATENCION_CLIENTE = "Atención al Cliente";
 public static final String AREA_LIMPIEZA = "Limpieza";
 public static final String AREA_ADMINISTRACION = "Administración";

 private SessionManager() {
 limpiarSesion();
 }

 public static synchronized SessionManager getInstance() {
 if (instance == null) {
 instance = new SessionManager();
 }
 return instance;
 }

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

 public int getIdUsuarioActual() { return idUsuario; }
 public String getUsuarioActual() { return nombreUsuario; }
 public String getPerfilActual() { return perfil; }
 public String getAreaActual() { return area; }

 public boolean isLoggedIn() {
 return idUsuario > 0 && !nombreUsuario.trim().isEmpty() && !perfil.trim().isEmpty();
 }

 public boolean isCliente() {
 return isLoggedIn() && PERFIL_CLIENTE.equals(perfil);
 }

 public boolean isAdmin() {
 return isLoggedIn() && PERFIL_ADMIN.equals(perfil);
 }

 public boolean tienePermiso(Permiso permiso) {
 return isLoggedIn() && PermisoService.tienePermiso(perfil, permiso);
 }

 // Métodos de compatibilidad con el sistema de áreas anterior
 public boolean isEmpleado() {
 return isLoggedIn() && !PERFIL_CLIENTE.equals(perfil) && !PERFIL_ADMIN.equals(perfil);
 }

 public boolean isAreaProduccion() {
 return tienePermiso(Permiso.PRODUCCION_LEER) || tienePermiso(Permiso.DECORACION_LEER);
 }

 public boolean isAreaDelivery() {
 return tienePermiso(Permiso.ENTREGAS_LEER);
 }

 public boolean isAreaLimpieza() {
 return tienePermiso(Permiso.LIMPIEZA_LEER) || tienePermiso(Permiso.MANTENIMIENTO_LEER);
 }

 public boolean isAreaVentas() {
 return PERFIL_RECEPCION.equals(perfil);
 }

 public boolean isAreaAtencionCliente() {
 return PERFIL_RECEPCION.equals(perfil);
 }

 public boolean isAreaAdministracion() {
 return PERFIL_ADMIN.equals(perfil) || PERFIL_CONTABILIDAD.equals(perfil) ||
 PERFIL_RRHH.equals(perfil) || PERFIL_AUDITOR.equals(perfil) ||
 PERFIL_ALMACEN.equals(perfil);
 }

 private boolean esPerfilValido(String perfil) {
 return PermisoService.esRolValido(perfil);
 }

 public String getSessionInfo() {
 if (!isLoggedIn()) {
 return "No hay sesión activa";
 }
 return String.format("Usuario: %s (ID: %d, Perfil: %s, Area: %s)", nombreUsuario, idUsuario, perfil, area);
 }
}
