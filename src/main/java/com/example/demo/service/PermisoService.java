package com.example.demo.service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PermisoService {

 private static final Map<String, Set<Permiso>> PERMISOS_POR_ROL = new HashMap<>();

 static {
 // ADMIN: todos los permisos
 PERMISOS_POR_ROL.put("ADMIN", Collections.unmodifiableSet(EnumSet.allOf(Permiso.class)));

 // RECEPCION: CRUD Clientes, CRUD Pedidos (sin DELETE)
 PERMISOS_POR_ROL.put("RECEPCION", Collections.unmodifiableSet(EnumSet.of(
 Permiso.CLIENTES_CREAR, Permiso.CLIENTES_LEER, Permiso.CLIENTES_ACTUALIZAR, Permiso.CLIENTES_ELIMINAR,
 Permiso.PEDIDOS_CREAR, Permiso.PEDIDOS_LEER, Permiso.PEDIDOS_ACTUALIZAR,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // PLANIFICADOR: READ/UPDATE Producción
 PERMISOS_POR_ROL.put("PLANIFICADOR", Collections.unmodifiableSet(EnumSet.of(
 Permiso.PRODUCCION_LEER, Permiso.PRODUCCION_ACTUALIZAR,
 Permiso.PEDIDOS_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // ALMACEN: CRUD Inventario, READ Proveedores
 PERMISOS_POR_ROL.put("ALMACEN", Collections.unmodifiableSet(EnumSet.of(
 Permiso.INVENTARIO_CREAR, Permiso.INVENTARIO_LEER, Permiso.INVENTARIO_ACTUALIZAR, Permiso.INVENTARIO_ELIMINAR,
 Permiso.PROVEEDORES_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // PRODUCCION: READ/UPDATE Producción, NO acceso a pagos
 PERMISOS_POR_ROL.put("PRODUCCION", Collections.unmodifiableSet(EnumSet.of(
 Permiso.PRODUCCION_LEER, Permiso.PRODUCCION_ACTUALIZAR,
 Permiso.PEDIDOS_LEER,
 Permiso.INVENTARIO_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // DECORACION: READ/UPDATE Decoración asignada
 PERMISOS_POR_ROL.put("DECORACION", Collections.unmodifiableSet(EnumSet.of(
 Permiso.DECORACION_LEER, Permiso.DECORACION_ACTUALIZAR,
 Permiso.PEDIDOS_LEER,
 Permiso.INVENTARIO_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // CONTABILIDAD: CRUD Pagos + Facturación, READ Pedidos, NO modificar pedidos
 PERMISOS_POR_ROL.put("CONTABILIDAD", Collections.unmodifiableSet(EnumSet.of(
 Permiso.PAGOS_CREAR, Permiso.PAGOS_LEER, Permiso.PAGOS_ACTUALIZAR, Permiso.PAGOS_ELIMINAR,
 Permiso.FACTURACION_CREAR, Permiso.FACTURACION_LEER,
 Permiso.PEDIDOS_LEER,
 Permiso.REPORTES_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // REPARTIDOR: READ/UPDATE Entregas asignadas
 PERMISOS_POR_ROL.put("REPARTIDOR", Collections.unmodifiableSet(EnumSet.of(
 Permiso.ENTREGAS_LEER, Permiso.ENTREGAS_ACTUALIZAR,
 Permiso.PEDIDOS_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // RRHH: CRUD Empleados + Capacitaciones
 PERMISOS_POR_ROL.put("RRHH", Collections.unmodifiableSet(EnumSet.of(
 Permiso.PERSONAL_CREAR, Permiso.PERSONAL_LEER, Permiso.PERSONAL_ACTUALIZAR, Permiso.PERSONAL_ELIMINAR,
 Permiso.CAPACITACIONES_CREAR, Permiso.CAPACITACIONES_LEER, Permiso.CAPACITACIONES_ACTUALIZAR, Permiso.CAPACITACIONES_ELIMINAR,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_EMPLEADO_LEER
 )));

 // AUDITOR: solo permisos READ
 PERMISOS_POR_ROL.put("AUDITOR", Collections.unmodifiableSet(EnumSet.of(
 Permiso.CLIENTES_LEER,
 Permiso.PEDIDOS_LEER,
 Permiso.INVENTARIO_LEER,
 Permiso.PRODUCCION_LEER,
 Permiso.DECORACION_LEER,
 Permiso.ENTREGAS_LEER,
 Permiso.PAGOS_LEER,
 Permiso.FACTURACION_LEER,
 Permiso.PERSONAL_LEER,
 Permiso.CAPACITACIONES_LEER,
 Permiso.PROVEEDORES_LEER,
 Permiso.REPORTES_LEER,
 Permiso.LIMPIEZA_LEER,
 Permiso.MANTENIMIENTO_LEER,
 Permiso.DASHBOARD_ADMIN_LEER,
 Permiso.PERFIL_LEER
 )));

 // CLIENTE: solo sus propios pedidos y perfil
 PERMISOS_POR_ROL.put("CLIENTE", Collections.unmodifiableSet(EnumSet.of(
 Permiso.PEDIDOS_CREAR, Permiso.PEDIDOS_LEER,
 Permiso.PERFIL_LEER, Permiso.PERFIL_ACTUALIZAR,
 Permiso.DASHBOARD_CLIENTE_LEER
 )));
 }

 public static boolean tienePermiso(String rol, Permiso permiso) {
 Set<Permiso> permisos = PERMISOS_POR_ROL.get(rol);
 if (permisos == null) {
 return false;
 }
 return permisos.contains(permiso);
 }

 public static Set<Permiso> getPermisos(String rol) {
 Set<Permiso> permisos = PERMISOS_POR_ROL.get(rol);
 return permisos != null ? permisos : Collections.emptySet();
 }

 public static boolean esRolValido(String rol) {
 return PERMISOS_POR_ROL.containsKey(rol);
 }
}
