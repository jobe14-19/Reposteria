# Repostería Rosato - Sistema de Gestión

## 📋 Descripción del Proyecto

Sistema integral de gestión para "Repostería Rosato", desarrollado en **JavaFX + SQL Server**. Permite administrar pedidos, inventario, producción, entregas, personal, limpieza, mantenimiento y generar reportes con JasperReports. Incluye control de acceso por roles (Administrador, Empleado por área, Cliente).

## 🏗️ Arquitectura - MVC

El proyecto sigue el patrón **Modelo-Vista-Controlador (MVC)**:

| Capa | Tecnología | Ubicación |
|------|-----------|-----------|
| **Vista** | FXML + CSS | `src/main/resources/com/example/demo/*.fxml` |
| **Controlador** | JavaFX Controllers | `com.example.demo.controller.*` |
| **Modelo** | POJOs / DTOs | `com.example.demo.model.*` |
| **DAO** | JDBC + SQL Server | `com.example.demo.dao.*` |
| **Servicio** | Lógica de negocio | `com.example.demo.service.*` |

## 🗄️ Base de Datos

- **Motor**: SQL Server
- **Base de datos**: `Reposteria`
- **Esquema**: `schema.sql` (20 tablas)
- **Script de inicialización**: Ejecutar `schema.sql` en SQL Server Management Studio

## 🔐 Roles y Permisos

| Rol | Acceso |
|-----|--------|
| **ADMIN** | Todas las funcionalidades |
| **EMPLEADO - Producción** | Planificación, Inventario, Calendario |
| **EMPLEADO - Decoración** | Planificación, Inventario, Calendario |
| **EMPLEADO - Delivery** | Entregas |
| **EMPLEADO - Ventas** | Pedidos, Clientes, Inventario |
| **EMPLEADO - Atención al Cliente** | Pedidos, Clientes, Entregas |
| **EMPLEADO - Limpieza** | Limpieza y Mantenimiento |
| **EMPLEADO - Administración** | Acceso completo |
| **CLIENTE** | Mis pedidos, Perfil |

## ✨ Funcionalidades Principales

- **Dashboard Administrativo**: KPIs, gráficos de ventas (JavaFX Charts), productos más vendidos (BarChart)
- **Gestión de Pedidos**: CRUD completo, cálculo automático de totales y adelantos
- **Gestión de Inventario**: Control de stock mínimo, alertas de reabastecimiento
- **Gestión de Entregas**: Registro de entregas, cálculo de saldos, facturación
- **Gestión de Personal**: Registro de empleados por área, capacitaciones
- **Gestión de Limpieza y Mantenimiento**: Programación y registro
- **Reportes JasperReports**: Facturas, Inventario, Pedidos, Personal, Limpieza, Dashboard de Ventas (Vista Previa, PDF, Excel)
- **Control de Acceso**: Login con roles y permisos por área

## 🛠️ Stack Tecnológico

- **Java 17** + JavaFX 21.0.2
- **SQL Server** + JDBC Driver 13.4.0
- **Maven** (gestión de dependencias)
- **JasperReports 6.21.4** (reportes)
- **JavaMail** (envío de correos)
- **Git + GitHub** (control de versiones)
- **GitHub Projects** (gestión de proyecto)

## 📦 Instalación y Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/jobe14-19/Reposteria.git
   ```
2. Ejecutar `schema.sql` en SQL Server Management Studio
3. Configurar credenciales en `DatabaseConnection.java` (usuario SQL: `AnelizEr`, contraseña: `12345678`)
4. Compilar y ejecutar:
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

## 👥 Roles del Equipo (Planificación)

| Miembro | Rol | Responsabilidad |
|---------|-----|-----------------|
| *Equipo* | Desarrollador | Implementación de módulos y reportes |
| *Equipo* | Diseñador BD | Modelado y mantenimiento de esquema |
| *Equipo* | Tester | Pruebas de funcionalidad |

## 📊 Análisis del Proyecto

### Fortalezas
- Interfaz moderna con JavaFX y FXML
- Roles y permisos granulares por área
- Reportes profesionales con JasperReports (PDF/Excel)
- Gráficos estadísticos integrados (JavaFX Charts)
- Envío de correos notificando eventos
- Manejo amigable de errores con Alertas JavaFX
- Base de datos relacional normalizada
- Control de versiones con Git/GitHub

### Debilidades y Mejoras Futuras
- Sin pruebas unitarias automatizadas
- La configuración de BD está hardcodeada
- Sin implementación de REST API
- La interfaz no es responsive
- Sin caché de datos

## 📁 Estructura del Proyecto

```
Reposteria/
├── .github/              # Configuración de GitHub
├── src/
│   ├── main/java/com/example/demo/
│   │   ├── controller/   # Controladores JavaFX
│   │   ├── dao/          # Acceso a datos (JDBC)
│   │   ├── model/        # Clases de modelo
│   │   ├── service/      # Lógica de negocio
│   │   └── util/         # Utilidades
│   └── main/resources/
│       ├── com/example/demo/  # Vistas FXML
│       └── reportes/          # Reportes JasperReports (.jrxml)
├── schema.sql            # Script de base de datos
└── pom.xml               # Configuración Maven
```

## 🧪 Pruebas Realizadas

- Login con credenciales (admin/empleado/cliente)
- CRUD completo de pedidos, clientes, empleados, ingredientes
- Cálculo automático de totales y adelantos
- Registro de entregas y facturación
- Generación de reportes (Vista Previa, PDF, Excel)
- Control de acceso por rol y área
- Envío de correos de prueba
- Exportación de inventario y personal
