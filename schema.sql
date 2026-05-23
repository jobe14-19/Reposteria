-- ============================================
-- SCRIPT PARA CREAR/ACTUALIZAR LA BD Reposteria
-- ============================================
-- ADVERTENCIA: Este script NO borra la BD existente.
-- Todas las tablas usan IF NOT EXISTS para ser
-- ejecutado múltiples veces sin perder datos.
-- Si necesitas reiniciar desde cero, borra la BD
-- manualmente: DROP DATABASE Reposteria;
-- ============================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'Reposteria')
BEGIN
    CREATE DATABASE Reposteria;
END
GO

USE Reposteria;
GO

-- ============================================
-- TABLAS DEL SISTEMA
-- ============================================

-- 1. Usuarios (admin/employee login)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[usuarios]') AND type in (N'U'))
BEGIN
CREATE TABLE usuarios (
    id_usuario INT IDENTITY(1,1) PRIMARY KEY,
    usuario NVARCHAR(50) NOT NULL UNIQUE,
    contrasena NVARCHAR(100) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    perfil NVARCHAR(20) NOT NULL CHECK (perfil IN ('ADMIN', 'RECEPCION', 'PLANIFICADOR', 'ALMACEN', 'PRODUCCION', 'DECORACION', 'CONTABILIDAD', 'REPARTIDOR', 'RRHH', 'AUDITOR', 'CLIENTE')),
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo')),
    fecha_registro DATETIME DEFAULT GETDATE()
);
END
GO

-- 2. Clientes
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[clientes]') AND type in (N'U'))
BEGIN
CREATE TABLE clientes (
    id_cliente INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    apellido NVARCHAR(100),
    telefono NVARCHAR(20),
    email NVARCHAR(100),
    direccion NVARCHAR(200),
    rnc NVARCHAR(20),
    usuario NVARCHAR(50),
    contrasena NVARCHAR(100),
    fecha_registro DATETIME DEFAULT GETDATE(),
    fecha_modificacion DATETIME,
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo'))
);
END
GO

-- 3. Empleados
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[empleados]') AND type in (N'U'))
BEGIN
CREATE TABLE empleados (
    id_empleado INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    cedula NVARCHAR(20) UNIQUE,
    telefono NVARCHAR(20),
    edad INT,
    genero NVARCHAR(10),
    area NVARCHAR(50),
    disponibilidad NVARCHAR(30),
    salario DECIMAL(12,2),
    fecha_contratacion DATE,
    fecha_prueba_embarazo DATE,
    fecha_modificacion DATETIME,
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo')),
    contrasena NVARCHAR(100)
);
END
GO

-- 4. Productos (catálogo de pasteles/postres)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[productos]') AND type in (N'U'))
BEGIN
CREATE TABLE productos (
    id_producto INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    precio_base DECIMAL(12,2) DEFAULT 0,
    precio_unitario DECIMAL(12,2) DEFAULT 0,
    costo_disenio DECIMAL(12,2) DEFAULT 0,
    descripcion NVARCHAR(500),
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo'))
);
END
GO

-- 5. Pedidos
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[pedidos]') AND type in (N'U'))
BEGIN
CREATE TABLE pedidos (
    id_pedido INT IDENTITY(1,1) PRIMARY KEY,
    id_cliente INT,
    id_producto INT,
    fecha_pedido DATETIME DEFAULT GETDATE(),
    fecha_creacion DATETIME DEFAULT GETDATE(),
    fecha_entrega DATETIME,
    fecha_entrega_real DATETIME,
    libras DECIMAL(10,2),
    diseno NVARCHAR(MAX),
    total DECIMAL(12,2),
    adelanto DECIMAL(12,2),
    observaciones NVARCHAR(MAX),
    estado NVARCHAR(30) DEFAULT 'Pendiente',
    prioridad NVARCHAR(10) DEFAULT 'Normal' CHECK (prioridad IN ('ALTA', 'Normal', 'Baja')),
    tipo_entrega NVARCHAR(10) DEFAULT 'L' CHECK (tipo_entrega IN ('L', 'D')),
    direccion_entrega NVARCHAR(200),
    costo_delivery DECIMAL(12,2) DEFAULT 0,
    username NVARCHAR(50),
    producto NVARCHAR(100),
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
END
GO

-- 6. Detalles del pedido
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[detalles_pedido]') AND type in (N'U'))
BEGIN
CREATE TABLE detalles_pedido (
    id_detalle INT IDENTITY(1,1) PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad DECIMAL(10,2) DEFAULT 1,
    precio_unitario DECIMAL(12,2),
    subtotal DECIMAL(12,2),
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
END
GO

-- 7. Pagos
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[pagos]') AND type in (N'U'))
BEGIN
CREATE TABLE pagos (
    id_pago INT IDENTITY(1,1) PRIMARY KEY,
    id_pedido INT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    fecha_pago DATETIME DEFAULT GETDATE(),
    metodo_pago NVARCHAR(30) DEFAULT 'Efectivo',
    referencia NVARCHAR(100),
    estado NVARCHAR(20) DEFAULT 'Pagado' CHECK (estado IN ('Pagado', 'Pendiente', 'Reembolsado')),
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
);
END
GO

-- 8. Proveedores
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[proveedores]') AND type in (N'U'))
BEGIN
CREATE TABLE proveedores (
    id_proveedor INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    contacto NVARCHAR(100),
    telefono NVARCHAR(20),
    email NVARCHAR(100),
    direccion NVARCHAR(200),
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo'))
);
END
GO

-- 9. Compras (órdenes de compra a proveedores)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[compras]') AND type in (N'U'))
BEGIN
CREATE TABLE compras (
    id_compra INT IDENTITY(1,1) PRIMARY KEY,
    id_proveedor INT NOT NULL,
    fecha_compra DATETIME DEFAULT GETDATE(),
    usuario_registra INT,
    total DECIMAL(12,2) DEFAULT 0,
    FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
);
END
GO

-- 10. Detalles de compra
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[compra_detalles]') AND type in (N'U'))
BEGIN
CREATE TABLE compra_detalles (
    id_detalle INT IDENTITY(1,1) PRIMARY KEY,
    id_compra INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2),
    descuento DECIMAL(12,2) DEFAULT 0,
    subtotal DECIMAL(12,2),
    FOREIGN KEY (id_compra) REFERENCES compras(id_compra),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
END
GO

-- 11. Ingredientes (inventario de materia prima)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ingredientes]') AND type in (N'U'))
BEGIN
CREATE TABLE ingredientes (
    id_ingrediente INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    categoria NVARCHAR(50),
    unidad NVARCHAR(20),
    stock_actual DECIMAL(12,2) DEFAULT 0,
    stock_minimo DECIMAL(12,2) DEFAULT 0,
    fecha_registro DATETIME DEFAULT GETDATE(),
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo'))
);
END
GO

-- 12. Inventario (visión general - puede ser vista o tabla separada)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[inventario]') AND type in (N'U'))
BEGIN
CREATE TABLE inventario (
    id_inventario INT IDENTITY(1,1) PRIMARY KEY,
    ingrediente NVARCHAR(100) NOT NULL,
    stock_actual DECIMAL(12,2) DEFAULT 0,
    stock_minimo DECIMAL(12,2) DEFAULT 0,
    unidad NVARCHAR(20)
);
END
GO

-- 13. Equipos
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[equipos]') AND type in (N'U'))
BEGIN
CREATE TABLE equipos (
    id_equipo INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    estado NVARCHAR(30) DEFAULT 'Operativo' CHECK (estado IN ('Operativo', 'Mantenimiento', 'Fuera de servicio'))
);
END
GO

-- 14. Máquinas
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[maquinas]') AND type in (N'U'))
BEGIN
CREATE TABLE maquinas (
    id_maquina INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    utilidad NVARCHAR(200),
    estado NVARCHAR(30) DEFAULT 'Operativo' CHECK (estado IN ('Operativo', 'Mantenimiento', 'Fuera de servicio')),
    ultimo_mantenimiento DATE,
    proximo_mantenimiento DATE
);
END
GO

-- 15. Mantenimiento (registros de mantenimiento)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[mantenimiento]') AND type in (N'U'))
BEGIN
CREATE TABLE mantenimiento (
    id_mantenimiento INT IDENTITY(1,1) PRIMARY KEY,
    equipo NVARCHAR(100) NOT NULL,
    descripcion NVARCHAR(MAX),
    tecnico NVARCHAR(100),
    fecha_mantenimiento DATE,
    proximo_mantenimiento DATE
);
END
GO

-- 16. Limpieza (registros de limpieza)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[limpieza]') AND type in (N'U'))
BEGIN
CREATE TABLE limpieza (
    id_limpieza INT IDENTITY(1,1) PRIMARY KEY,
    area NVARCHAR(100) NOT NULL,
    descripcion NVARCHAR(MAX),
    responsable NVARCHAR(100),
    fecha_limpieza DATE
);
END
GO

-- 17. Capacitaciones
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[capacitaciones]') AND type in (N'U'))
BEGIN
CREATE TABLE capacitaciones (
    id_capacitacion INT IDENTITY(1,1) PRIMARY KEY,
    id_empleado INT NOT NULL,
    tema NVARCHAR(200) NOT NULL,
    fecha DATE,
    duracion DECIMAL(5,2),
    capacitador NVARCHAR(100),
    usuario_registra INT,
    FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado)
);
END
GO

-- 18. Producción
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[produccion]') AND type in (N'U'))
BEGIN
CREATE TABLE produccion (
    id_produccion INT IDENTITY(1,1) PRIMARY KEY,
    producto NVARCHAR(100) NOT NULL,
    cantidad INT DEFAULT 0,
    progreso DECIMAL(5,2) DEFAULT 0,
    estado NVARCHAR(30) DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'En Progreso', 'Completado', 'Cancelado'))
);
END
GO

-- 19. Entregas
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[entregas]') AND type in (N'U'))
BEGIN
CREATE TABLE entregas (
    id_entrega INT IDENTITY(1,1) PRIMARY KEY,
    fecha_entrega DATE,
    hora_entrega TIME,
    cliente NVARCHAR(100),
    direccion NVARCHAR(200),
    producto NVARCHAR(100),
    estado NVARCHAR(20) DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'En ruta', 'Entregado', 'Cancelado'))
);
END
GO

-- 20. Chef's Box (cajas especiales del chef)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[chefs_box]') AND type in (N'U'))
BEGIN
CREATE TABLE chefs_box (
    id_chef_box INT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(100) NOT NULL,
    descripcion NVARCHAR(500),
    precio DECIMAL(12,2) DEFAULT 0,
    disponible BIT DEFAULT 1,
    fecha_creacion DATETIME DEFAULT GETDATE(),
    fecha_modificacion DATETIME,
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo'))
);
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[chef_box_productos]') AND type in (N'U'))
BEGIN
CREATE TABLE chef_box_productos (
    id_chef_box INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT DEFAULT 1,
    PRIMARY KEY (id_chef_box, id_producto),
    FOREIGN KEY (id_chef_box) REFERENCES chefs_box(id_chef_box),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
END
GO

-- 21. Actividad (auditoría)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[actividad]') AND type in (N'U'))
BEGIN
CREATE TABLE actividad (
    id_actividad INT IDENTITY(1,1) PRIMARY KEY,
    fecha_hora DATETIME DEFAULT GETDATE(),
    usuario NVARCHAR(50),
    accion NVARCHAR(200),
    detalle NVARCHAR(MAX)
);
END
GO

-- ============================================
-- VISTA: inventario (si se desea como vista de ingredientes)
-- ============================================
-- Descomentar si se prefiere usar una vista en lugar de la tabla inventario
-- GO
-- CREATE OR ALTER VIEW vista_inventario AS
-- SELECT i.nombre as ingrediente, i.stock_actual, i.stock_minimo, i.unidad
-- FROM ingredientes i;
-- GO

-- ============================================
-- DATOS INICIALES (SEED)
-- ============================================

-- Usuario administrador
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'admin')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('admin', 'admin123', N'Administrador', 'ADMIN', 'Activo');
END
GO

-- Usuario recepcion
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'recepcion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('recepcion', 'rec123', N'Recepción', 'RECEPCION', 'Activo');
END
GO

-- Usuario planificador
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'planificador')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('planificador', 'plan123', N'Planificador', 'PLANIFICADOR', 'Activo');
END
GO

-- Usuario almacen
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'almacen')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('almacen', 'alm123', N'Almacén', 'ALMACEN', 'Activo');
END
GO

-- Usuario produccion
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'produccion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('produccion', 'prod123', N'Producción', 'PRODUCCION', 'Activo');
END
GO

-- Usuario decoracion
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'decoracion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('decoracion', 'dec123', N'Decoración', 'DECORACION', 'Activo');
END
GO

-- Usuario contabilidad
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'contabilidad')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('contabilidad', 'cont123', N'Contabilidad', 'CONTABILIDAD', 'Activo');
END
GO

-- Usuario repartidor
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'repartidor')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('repartidor', 'rep123', N'Repartidor', 'REPARTIDOR', 'Activo');
END
GO

-- Usuario rrhh
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'rrhh')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('rrhh', 'rrhh123', N'RRHH', 'RRHH', 'Activo');
END
GO

-- Usuario auditor
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'auditor')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('auditor', 'aud123', N'Auditor', 'AUDITOR', 'Activo');
END
GO

-- Usuario cliente
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'cliente')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('cliente', 'cli123', N'Cliente', 'CLIENTE', 'Activo');
END
GO

-- Productos iniciales
IF NOT EXISTS (SELECT 1 FROM productos)
BEGIN
    INSERT INTO productos (nombre, precio_base, costo_disenio, descripcion) VALUES
    (N'Pastel de Chocolate', 800, 200, N'Pastel de chocolate con relleno de crema'),
    (N'Pastel de Vainilla', 700, 200, N'Pastel de vainilla con frosting'),
    (N'Pastel de Fresas', 900, 300, N'Pastel con fresas naturales y crema batida'),
    (N'Pastel Red Velvet', 1000, 250, N'Pastel red velvet con queso crema'),
    (N'Pastel de Zanahoria', 850, 200, N'Pastel de zanahoria con nueces'),
    (N'Tres Leches', 750, 150, N'Pastel tres leches tradicional'),
    (N'Cheesecake', 950, 250, N'Cheesecake con frutos rojos'),
    (N'Macarons (docena)', 600, 0, N'Docena de macarons surtidos'),
    (N'Cupcakes (6 unidades)', 450, 100, N'Cupcakes decorados'),
    (N'Pastel Personalizado', 1200, 500, N'Pastel completamente personalizable');
END
GO

-- Proveedores iniciales
IF NOT EXISTS (SELECT 1 FROM proveedores)
BEGIN
    INSERT INTO proveedores (nombre, contacto, telefono, email, direccion) VALUES
    (N'Distribuidora La Torre', N'Juan Pérez', '809-555-0101', 'jperez@ltorre.com', N'Av. Independencia 123'),
    (N'Insumos del Chef', N'María García', '809-555-0102', 'mgarcia@insumoschef.com', N'Calle El Sol 45'),
    (N'Productos Doña Juana', N'Ana Martínez', '809-555-0103', 'amartinez@donajuana.com', N'Av. Duarte 78');
END
GO

-- Máquinas iniciales
IF NOT EXISTS (SELECT 1 FROM maquinas)
BEGIN
    INSERT INTO maquinas (nombre, utilidad, estado) VALUES
    (N'Horno Rotatorio', N'Horneado de pasteles y panes', 'Operativo'),
    (N'Batidora Industrial', N'Mezcla de masas y cremas', 'Operativo'),
    (N'Refrigerador', N'Conservación de ingredientes', 'Operativo'),
    (N'Congelador', N'Congelación de productos', 'Operativo'),
    (N'Amasadora', N'Amasado de masas pesadas', 'Operativo');
END
GO

-- Equipos iniciales
IF NOT EXISTS (SELECT 1 FROM equipos)
BEGIN
    INSERT INTO equipos (nombre, estado) VALUES
    (N'Vitrina Pastelera', 'Operativo'),
    (N'Cámara de Frío', 'Operativo'),
    (N'Extractor de Aire', 'Operativo'),
    (N'Termómetro Digital', 'Operativo');
END
GO

-- Clientes iniciales (ID debe coincidir con usuarios.id_usuario para CLIENTE)
IF NOT EXISTS (SELECT 1 FROM clientes WHERE id_cliente = 3)
BEGIN
    SET IDENTITY_INSERT clientes ON;
    INSERT INTO clientes (id_cliente, nombre, apellido, telefono, email, usuario, contrasena, estado) VALUES
    (3, N'Cliente', N'Demo', '809-555-1001', 'cliente@demo.com', 'cliente', 'cli123', 'Activo');
    SET IDENTITY_INSERT clientes OFF;
END
GO

-- Empleados iniciales (ID debe coincidir con usuarios.id_usuario)
IF NOT EXISTS (SELECT 1 FROM empleados WHERE id_empleado = 2)
BEGIN
    SET IDENTITY_INSERT empleados ON;
    INSERT INTO empleados (id_empleado, nombre, cedula, telefono, area, estado) VALUES
    (2, N'Recepción Demo', '001-0000000-2', '809-555-2001', N'Ventas', 'Activo'),
    (3, N'Planificador Demo', '001-0000000-3', '809-555-2002', N'Producción', 'Activo'),
    (4, N'Almacén Demo', '001-0000000-4', '809-555-2003', N'Administración', 'Activo'),
    (5, N'Producción Demo', '001-0000000-5', '809-555-2004', N'Producción', 'Activo'),
    (6, N'Decoración Demo', '001-0000000-6', '809-555-2005', N'Decoración', 'Activo'),
    (7, N'Contabilidad Demo', '001-0000000-7', '809-555-2006', N'Administración', 'Activo'),
    (8, N'Repartidor Demo', '001-0000000-8', '809-555-2007', N'Delivery', 'Activo'),
    (9, N'RRHH Demo', '001-0000000-9', '809-555-2008', N'Administración', 'Activo'),
    (10, N'Auditor Demo', '001-0000000-0', '809-555-2009', N'Administración', 'Activo');
    SET IDENTITY_INSERT empleados OFF;
END
GO

-- Ingredientes iniciales
IF NOT EXISTS (SELECT 1 FROM ingredientes)
BEGIN
    INSERT INTO ingredientes (nombre, categoria, unidad, stock_actual, stock_minimo) VALUES
    (N'Harina de Trigo', N'Harinas', N'libras', 50, 20),
    (N'Azúcar Blanca', N'Endulzantes', N'libras', 40, 15),
    (N'Mantequilla', N'Lácteos', N'libras', 25, 10),
    (N'Huevos', N'Frescos', N'unidades', 120, 60),
    (N'Leche Entera', N'Lácteos', N'litros', 15, 8),
    (N'Chocolate en Polvo', N'Sabores', N'libras', 8, 5),
    (N'Vainilla Líquida', N'Sabores', N'ml', 500, 200),
    (N'Crema de Leche', N'Lácteos', N'litros', 10, 5),
    (N'Fresas', N'Frutas', N'libras', 3, 5),
    (N'Queso Crema', N'Lácteos', N'libras', 12, 6);
END
GO

-- Inventario inicial (visión general)
IF NOT EXISTS (SELECT 1 FROM inventario)
BEGIN
    INSERT INTO inventario (ingrediente, stock_actual, stock_minimo, unidad) VALUES
    (N'Harina de Trigo', 50, 20, N'libras'),
    (N'Azúcar Blanca', 40, 15, N'libras'),
    (N'Mantequilla', 25, 10, N'libras'),
    (N'Huevos', 120, 60, N'unidades'),
    (N'Leche Entera', 15, 8, N'litros'),
    (N'Chocolate en Polvo', 8, 5, N'libras'),
    (N'Vainilla Líquida', 500, 200, N'ml'),
    (N'Crema de Leche', 10, 5, N'litros'),
    (N'Fresas', 3, 5, N'libras'),
    (N'Queso Crema', 12, 6, N'libras');
END
GO

-- Muestras de pedidos para planificación
IF NOT EXISTS (SELECT 1 FROM pedidos)
BEGIN
    INSERT INTO pedidos (id_cliente, id_producto, fecha_pedido, fecha_entrega, libras, total, adelanto, observaciones, estado) VALUES
    (3, 1, GETDATE(), DATEADD(DAY, 2, GETDATE()), 2.5, 2000, 1000, '', 'Confirmado'),
    (3, 2, GETDATE(), DATEADD(DAY, 3, GETDATE()), 1.5, 1050, 500, '', 'En producción'),
    (3, 3, GETDATE(), DATEADD(DAY, 5, GETDATE()), 3.0, 2700, 1350, '', 'Confirmado'),
    (3, 4, GETDATE(), DATEADD(DAY, -1, GETDATE()), 2.0, 2000, 2000, '', 'Entregado'),
    (3, 5, GETDATE(), DATEADD(DAY, 4, GETDATE()), 1.8, 1530, 765, '', 'Pendiente');
END
GO

-- ============================================
-- DATOS ADICIONALES (20+ inserts de ejemplo)
-- ============================================

-- Más clientes
IF NOT EXISTS (SELECT 1 FROM clientes WHERE id_cliente = 4)
BEGIN
    SET IDENTITY_INSERT clientes ON;
    INSERT INTO clientes (id_cliente, nombre, apellido, telefono, email, usuario, contrasena, estado) VALUES
    (4, N'María', N'López', '809-555-1002', 'mlopez@email.com', 'maria', '123456', 'Activo'),
    (5, N'Juan', N'Rodríguez', '809-555-1003', 'jrodriguez@email.com', 'juan', '123456', 'Activo'),
    (6, N'Ana', N'Martínez', '809-555-1004', 'amartinez@email.com', 'ana', '123456', 'Activo'),
    (7, N'Carlos', N'Sánchez', '809-555-1005', 'csanchez@email.com', 'carlos', '123456', 'Activo'),
    (8, N'Rosa', N'Ramírez', '809-555-1006', 'rramirez@email.com', 'rosa', '123456', 'Inactivo');
    SET IDENTITY_INSERT clientes OFF;
END
GO

-- Más empleados
IF NOT EXISTS (SELECT 1 FROM empleados WHERE id_empleado = 11)
BEGIN
    SET IDENTITY_INSERT empleados ON;
    INSERT INTO empleados (id_empleado, nombre, cedula, telefono, area, estado) VALUES
    (11, N'Pedro Pérez', '001-1111111-1', '809-555-2010', N'Decoración', 'Activo'),
    (12, N'Luisa Gómez', '001-2222222-2', '809-555-2011', N'Delivery', 'Activo'),
    (13, N'José Hernández', '001-3333333-3', '809-555-2012', N'Limpieza', 'Activo'),
    (14, N'Carmen Díaz', '001-4444444-4', '809-555-2013', N'Producción', 'Inactivo');
    SET IDENTITY_INSERT empleados OFF;
END
GO

-- Más máquinas
IF NOT EXISTS (SELECT 1 FROM maquinas WHERE id_maquina = 6)
BEGIN
    INSERT INTO maquinas (nombre, utilidad, estado) VALUES
    (N'Laminadora', N'Laminado de masas para pastelería', 'Operativo'),
    (N'Cámara de Fermentación', N'Control de temperatura para fermentación', 'Mantenimiento');
END
GO

-- Más equipos
IF NOT EXISTS (SELECT 1 FROM equipos WHERE id_equipo = 5)
BEGIN
    INSERT INTO equipos (nombre, estado) VALUES
    (N'Balanza Digital', 'Operativo'),
    (N'Licuadora Industrial', 'Fuera de servicio');
END
GO

-- Más proveedores
IF NOT EXISTS (SELECT 1 FROM proveedores WHERE id_proveedor = 4)
BEGIN
    INSERT INTO proveedores (nombre, contacto, telefono, email, direccion) VALUES
    (N'Distribuidora Ozama', N'Roberto Santos', '809-555-0104', 'rsantos@ozama.com', N'Av. Mella 200'),
    (N'Comercial del Este', N'Laura Fernández', '809-555-0105', 'lfernandez@deleste.com', N'Calle 27 de Febrero 150');
END
GO

-- Registros de actividad de ejemplo
IF NOT EXISTS (SELECT 1 FROM actividad)
BEGIN
    INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES
    (DATEADD(DAY, -1, GETDATE()), 'admin', 'CREAR PEDIDO', 'Nuevo pedido creado: #1'),
    (DATEADD(HOUR, -20, GETDATE()), 'admin', 'ACTUALIZAR INVENTARIO', 'Stock actualizado: Harina de Trigo'),
    (DATEADD(HOUR, -18, GETDATE()), 'empleado', 'INICIAR PRODUCCIÓN', 'Producción iniciada para pedido #1'),
    (DATEADD(HOUR, -6, GETDATE()), 'admin', 'ACTUALIZAR ESTADO', 'Pedido #1 marcado como Listo'),
    (DATEADD(HOUR, -2, GETDATE()), 'cliente', 'CREAR PEDIDO', 'Nuevo pedido creado: #5'),
    (GETDATE(), 'admin', 'INICIAR SESIÓN', 'Administrador ha iniciado sesión');
END
GO

-- Registros de limpieza de ejemplo
IF NOT EXISTS (SELECT 1 FROM limpieza)
BEGIN
    INSERT INTO limpieza (area, descripcion, responsable, fecha_limpieza) VALUES
    (N'Cocina Principal', N'Limpieza general de superficies y pisos', N'José Hernández', DATEADD(DAY, -3, GETDATE())),
    (N'Área de Hornos', N'Limpieza de hornos y extractores', N'José Hernández', DATEADD(DAY, -2, GETDATE())),
    (N'Cámara de Frío', N'Limpieza y organización de cámaras', N'Luisa Gómez', DATEADD(DAY, -1, GETDATE())),
    (N'Salón de Ventas', N'Aseo de vitrinas y mostrador', N'Carmen Díaz', GETDATE());
END
GO

-- Más pedidos (para data histórica)
IF NOT EXISTS (SELECT 1 FROM pedidos WHERE id_pedido = 6)
BEGIN
    INSERT INTO pedidos (id_cliente, id_producto, fecha_pedido, fecha_entrega, libras, total, adelanto, observaciones, estado) VALUES
    (4, 6, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -8, GETDATE()), 2.0, 1500, 1500, N'Pastel para cumpleaños', 'Entregado'),
    (5, 7, DATEADD(DAY, -8, GETDATE()), DATEADD(DAY, -6, GETDATE()), 1.5, 1425, 700, N'Cheesecake con frutos rojos', 'Entregado'),
    (6, 8, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -3, GETDATE()), 1.0, 600, 600, N'Docena de macarons variados', 'Entregado'),
    (7, 9, DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -2, GETDATE()), 0.5, 450, 450, N'Cupcakes de vainilla', 'Entregado'),
    (3, 10, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, 1, GETDATE()), 3.5, 4200, 2000, N'Pastel personalizado con diseño de flores', 'En producción'),
    (8, 1, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, 2, GETDATE()), 2.0, 1600, 800, N'', 'Confirmado'),
    (4, 3, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 7, GETDATE()), 4.0, 3600, 1800, N'Pastel de fresas para boda', 'Pendiente'),
    (5, 4, GETDATE(), DATEADD(DAY, 5, GETDATE()), 2.5, 2500, 0, N'', 'Pendiente'),
    (6, 5, GETDATE(), DATEADD(DAY, 4, GETDATE()), 1.8, 1530, 765, N'Pastel de zanahoria sin nueces', 'Confirmado'),
    (7, 7, GETDATE(), DATEADD(DAY, 6, GETDATE()), 2.0, 1900, 0, N'', 'Pendiente');
END
GO

PRINT N'Base de datos Reposteria creada exitosamente.';
GO
