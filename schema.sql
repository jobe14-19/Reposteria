-- ============================================
-- SCRIPT PARA CREAR/ACTUALIZAR LA BD Reposteria
-- ============================================
-- ADVERTENCIA: Este script NO borra la BD existente.
-- Todas las tablas usan IF NOT EXISTS para ser
-- ejecutado multiples veces sin perder datos.
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

-- 1. Usuarios
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

-- 4. Productos
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

-- Migracion: agregar tipo_pago y estado_pago a pedidos
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[pedidos]') AND type in (N'U'))
BEGIN
    IF EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='forma_pago') AND NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='tipo_pago')
        EXEC sp_rename 'pedidos.forma_pago', 'tipo_pago', 'COLUMN';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='tipo_pago')
        ALTER TABLE pedidos ADD tipo_pago NVARCHAR(50) DEFAULT 'Efectivo';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('pedidos') AND name='estado_pago')
        ALTER TABLE pedidos ADD estado_pago NVARCHAR(20) DEFAULT 'Pendiente';
END
GO

-- 6. Pagos
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

-- 7. Proveedores
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

-- 8. Compras
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

-- 9. Detalles de compra
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

-- 10. Ingredientes
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

-- 11. Maquinas
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

-- 12. Mantenimiento
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

-- 13. Limpieza
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

-- 14. Capacitaciones
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

-- 15. Chefs Box
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

-- 16. Actividad (auditoria)
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

-- 17. Recetas
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[recetas]') AND type in (N'U'))
BEGIN
CREATE TABLE recetas (
    id_receta INT IDENTITY(1,1) PRIMARY KEY,
    id_producto INT NOT NULL,
    nombre_receta NVARCHAR(200),
    descripcion NVARCHAR(MAX),
    categoria NVARCHAR(100),
    tiempo_preparacion INT DEFAULT 0,
    cantidad_producida DECIMAL(10,2) DEFAULT 1,
    imagen_ref NVARCHAR(500),
    porciones DECIMAL(10,2) DEFAULT 1,
    costo_estimado DECIMAL(12,2) DEFAULT 0,
    rendimiento DECIMAL(5,2) DEFAULT 100,
    desperdicio DECIMAL(5,2) DEFAULT 0,
    estado NVARCHAR(10) DEFAULT 'Activo' CHECK (estado IN ('Activo', 'Inactivo')),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
END
GO

-- 18. Receta Ingredientes
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[receta_ingredientes]') AND type in (N'U'))
BEGIN
CREATE TABLE receta_ingredientes (
    id_receta INT NOT NULL,
    id_ingrediente INT NOT NULL,
    cantidad DECIMAL(12,2) DEFAULT 0,
    PRIMARY KEY (id_receta, id_ingrediente),
    FOREIGN KEY (id_receta) REFERENCES recetas(id_receta),
    FOREIGN KEY (id_ingrediente) REFERENCES ingredientes(id_ingrediente)
);
END
GO

-- 19. Receta Pasos
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[receta_pasos]') AND type in (N'U'))
BEGIN
CREATE TABLE receta_pasos (
    id_paso INT IDENTITY(1,1) PRIMARY KEY,
    id_receta INT NOT NULL,
    numero_paso INT NOT NULL,
    titulo NVARCHAR(200),
    descripcion NVARCHAR(MAX),
    tiempo_estimado INT DEFAULT 0,
    imagen_ref NVARCHAR(500),
    FOREIGN KEY (id_receta) REFERENCES recetas(id_receta)
);
END
GO

-- 20. Ordenes de Produccion
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ordenes_produccion]') AND type in (N'U'))
BEGIN
CREATE TABLE ordenes_produccion (
    id_orden INT IDENTITY(1,1) PRIMARY KEY,
    numero_orden NVARCHAR(20) NOT NULL UNIQUE,
    estado NVARCHAR(20) DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'EN PRODUCCION', 'COMPLETADA', 'ENTREGADA', 'CANCELADA')),
    categoria NVARCHAR(100),
    revestimiento NVARCHAR(100),
    sucursal NVARCHAR(100),
    fecha_entrega DATE,
    hora_entrega NVARCHAR(10),
    cliente NVARCHAR(200),
    direccion NVARCHAR(500),
    telefono NVARCHAR(20),
    vendedor NVARCHAR(100),
    libras DECIMAL(10,2) DEFAULT 0,
    base_tipo NVARCHAR(100),
    masa_tipo NVARCHAR(100),
    forma NVARCHAR(100),
    pisos INT DEFAULT 1,
    lustres NVARCHAR(200),
    decoracion NVARCHAR(500),
    camuflajes NVARCHAR(200),
    flores NVARCHAR(200),
    mensaje NVARCHAR(500),
    observaciones NVARCHAR(MAX),
    adornos NVARCHAR(500),
    rellenos NVARCHAR(500),
    costo_estimado DECIMAL(12,2) DEFAULT 0,
    costo_real DECIMAL(12,2) DEFAULT 0,
    precio_venta DECIMAL(12,2) DEFAULT 0,
    anticipo DECIMAL(12,2) DEFAULT 0,
    saldo DECIMAL(12,2) DEFAULT 0,
    id_receta INT,
    fecha_creacion DATETIME DEFAULT GETDATE(),
    fecha_inicio DATETIME,
    fecha_completado DATETIME,
    usuario_crea NVARCHAR(100),
    progreso INT DEFAULT 0,
    pausado BIT DEFAULT 0,
    tipo_entrega NVARCHAR(2) DEFAULT 'L',
    costo_delivery DECIMAL(12,2) DEFAULT 0,
    tipo_pago NVARCHAR(50) DEFAULT 'Efectivo',
    estado_pago NVARCHAR(20) DEFAULT 'Pendiente',
    id_pedido INT,
    FOREIGN KEY (id_receta) REFERENCES recetas(id_receta),
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
);
END
GO

-- Migracion: agregar columnas si la tabla ya existia
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ordenes_produccion]') AND type in (N'U'))
BEGIN
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='tipo_entrega')
        ALTER TABLE ordenes_produccion ADD tipo_entrega NVARCHAR(2) DEFAULT 'L';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='costo_delivery')
        ALTER TABLE ordenes_produccion ADD costo_delivery DECIMAL(12,2) DEFAULT 0;
    IF EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='forma_pago')
        AND NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='tipo_pago')
        EXEC sp_rename 'ordenes_produccion.forma_pago', 'tipo_pago', 'COLUMN';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='tipo_pago')
        ALTER TABLE ordenes_produccion ADD tipo_pago NVARCHAR(50) DEFAULT 'Efectivo';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='estado_pago')
        ALTER TABLE ordenes_produccion ADD estado_pago NVARCHAR(20) DEFAULT 'Pendiente';
    IF NOT EXISTS (SELECT * FROM syscolumns WHERE id=OBJECT_ID('ordenes_produccion') AND name='id_pedido')
        ALTER TABLE ordenes_produccion ADD id_pedido INT;
END
GO

-- 21. Orden Fases
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[orden_fases]') AND type in (N'U'))
BEGIN
CREATE TABLE orden_fases (
    id_fase INT IDENTITY(1,1) PRIMARY KEY,
    id_orden INT NOT NULL,
    fase_nombre NVARCHAR(50) NOT NULL,
    fase_orden INT NOT NULL,
    estado NVARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_inicio DATETIME,
    fecha_fin DATETIME,
    usuario_inicia NVARCHAR(100),
    usuario_completa NVARCHAR(100),
    observaciones NVARCHAR(MAX),
    FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden)
);
END
GO

-- 22. Orden Historial
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[orden_historial]') AND type in (N'U'))
BEGIN
CREATE TABLE orden_historial (
    id_historial INT IDENTITY(1,1) PRIMARY KEY,
    id_orden INT NOT NULL,
    accion NVARCHAR(200) NOT NULL,
    detalle NVARCHAR(MAX),
    usuario NVARCHAR(100),
    fecha_hora DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden)
);
END
GO

-- 23. Orden Ingredientes
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[orden_ingredientes]') AND type in (N'U'))
BEGIN
CREATE TABLE orden_ingredientes (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_orden INT NOT NULL,
    id_ingrediente INT NOT NULL,
    cantidad_requerida DECIMAL(12,2) DEFAULT 0,
    cantidad_descontada DECIMAL(12,2) DEFAULT 0,
    descontado BIT DEFAULT 0,
    FOREIGN KEY (id_orden) REFERENCES ordenes_produccion(id_orden),
    FOREIGN KEY (id_ingrediente) REFERENCES ingredientes(id_ingrediente)
);
END
GO

-- 24. Stock Movimientos
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[stock_movimientos]') AND type in (N'U'))
BEGIN
CREATE TABLE stock_movimientos (
    id_movimiento INT IDENTITY(1,1) PRIMARY KEY,
    id_ingrediente INT NOT NULL,
    tipo_movimiento NVARCHAR(20) NOT NULL,
    cantidad DECIMAL(12,2) NOT NULL,
    stock_anterior DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock_nuevo DECIMAL(12,2) NOT NULL DEFAULT 0,
    motivo NVARCHAR(200),
    referencia_tipo NVARCHAR(50),
    referencia_id INT,
    usuario_registra NVARCHAR(100) NOT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT GETDATE()
);
END
GO

-- 25. Materiales
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[materiales]') AND type in (N'U'))
BEGIN
CREATE TABLE materiales (
    id_material INT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    unidad VARCHAR(50) DEFAULT 'unidad',
    stock_actual INT DEFAULT 0,
    stock_minimo INT DEFAULT 1,
    estado VARCHAR(10) DEFAULT 'Activo'
);
END
GO

-- 26. Checklist items (limpieza)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[checklist_items]') AND type in (N'U'))
BEGIN
CREATE TABLE checklist_items (
    id_checklist INT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    estado VARCHAR(10) DEFAULT 'Activo'
);
END
GO

-- 27. Facturas
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[facturas]') AND type in (N'U'))
BEGIN
CREATE TABLE facturas (
    id_factura INT IDENTITY(1,1) PRIMARY KEY,
    id_orden INT,
    cliente NVARCHAR(200),
    telefono NVARCHAR(20),
    direccion NVARCHAR(500),
    fecha DATE,
    subtotal DECIMAL(12,2),
    costo_delivery DECIMAL(12,2) DEFAULT 0,
    itbis DECIMAL(12,2) DEFAULT 0,
    descuento DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2),
    estado NVARCHAR(20) DEFAULT 'EMITIDA',
    detalles NVARCHAR(MAX),
    usuario_genera NVARCHAR(100),
    fecha_generacion DATETIME DEFAULT GETDATE(),
    metodo_pago NVARCHAR(30) DEFAULT 'Efectivo',
    pagado NVARCHAR(2) DEFAULT 'NO'
);
END
GO

-- ============================================
-- DATOS INICIALES (SEED)
-- ============================================

-- Usuarios
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'admin')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('admin', 'admin123', N'Administrador', 'ADMIN', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'recepcion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('recepcion', 'rec123', N'Recepcion', 'RECEPCION', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'planificador')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('planificador', 'plan123', N'Planificador', 'PLANIFICADOR', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'almacen')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('almacen', 'alm123', N'Almacen', 'ALMACEN', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'produccion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('produccion', 'prod123', N'Produccion', 'PRODUCCION', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'decoracion')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('decoracion', 'dec123', N'Decoracion', 'DECORACION', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'contabilidad')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('contabilidad', 'cont123', N'Contabilidad', 'CONTABILIDAD', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'repartidor')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('repartidor', 'rep123', N'Repartidor', 'REPARTIDOR', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'rrhh')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('rrhh', 'rrhh123', N'RRHH', 'RRHH', 'Activo');
END
GO

IF NOT EXISTS (SELECT 1 FROM usuarios WHERE usuario = 'auditor')
BEGIN
    INSERT INTO usuarios (usuario, contrasena, nombre, perfil, estado)
    VALUES ('auditor', 'aud123', N'Auditor', 'AUDITOR', 'Activo');
END
GO

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
    (N'Distribuidora La Torre', N'Juan Perez', '809-555-0101', 'jperez@ltorre.com', N'Av. Independencia 123'),
    (N'Insumos del Chef', N'Maria Garcia', '809-555-0102', 'mgarcia@insumoschef.com', N'Calle El Sol 45'),
    (N'Productos Dona Juana', N'Ana Martinez', '809-555-0103', 'amartinez@donajuana.com', N'Av. Duarte 78');
END
GO

-- Maquinas iniciales
IF NOT EXISTS (SELECT 1 FROM maquinas)
BEGIN
    INSERT INTO maquinas (nombre, utilidad, estado) VALUES
    (N'Horno Rotatorio', N'Horneado de pasteles y panes', 'Operativo'),
    (N'Batidora Industrial', N'Mezcla de masas y cremas', 'Operativo'),
    (N'Refrigerador', N'Conservacion de ingredientes', 'Operativo'),
    (N'Congelador', N'Congelacion de productos', 'Operativo'),
    (N'Amasadora', N'Amasado de masas pesadas', 'Operativo');
END
GO

-- Clientes iniciales
IF NOT EXISTS (SELECT 1 FROM clientes WHERE id_cliente = 3)
BEGIN
    SET IDENTITY_INSERT clientes ON;
    INSERT INTO clientes (id_cliente, nombre, apellido, telefono, email, usuario, contrasena, estado) VALUES
    (3, N'Cliente', N'Demo', '809-555-1001', 'cliente@demo.com', 'cliente', 'cli123', 'Activo');
    SET IDENTITY_INSERT clientes OFF;
END
GO

-- Empleados iniciales
IF NOT EXISTS (SELECT 1 FROM empleados WHERE id_empleado = 2)
BEGIN
    SET IDENTITY_INSERT empleados ON;
    INSERT INTO empleados (id_empleado, nombre, cedula, telefono, area, estado) VALUES
    (2, N'Recepcion Demo', '001-0000000-2', '809-555-2001', N'Ventas', 'Activo'),
    (3, N'Planificador Demo', '001-0000000-3', '809-555-2002', N'Produccion', 'Activo'),
    (4, N'Almacen Demo', '001-0000000-4', '809-555-2003', N'Administracion', 'Activo'),
    (5, N'Produccion Demo', '001-0000000-5', '809-555-2004', N'Produccion', 'Activo'),
    (6, N'Decoracion Demo', '001-0000000-6', '809-555-2005', N'Decoracion', 'Activo'),
    (7, N'Contabilidad Demo', '001-0000000-7', '809-555-2006', N'Administracion', 'Activo'),
    (8, N'Repartidor Demo', '001-0000000-8', '809-555-2007', N'Delivery', 'Activo'),
    (9, N'RRHH Demo', '001-0000000-9', '809-555-2008', N'Administracion', 'Activo'),
    (10, N'Auditor Demo', '001-0000000-0', '809-555-2009', N'Administracion', 'Activo');
    SET IDENTITY_INSERT empleados OFF;
END
GO

-- Ingredientes iniciales
IF NOT EXISTS (SELECT 1 FROM ingredientes)
BEGIN
    INSERT INTO ingredientes (nombre, categoria, unidad, stock_actual, stock_minimo) VALUES
    (N'Harina de Trigo', N'Harinas', N'libras', 50, 20),
    (N'Azucar Blanca', N'Endulzantes', N'libras', 40, 15),
    (N'Mantequilla', N'Lacteos', N'libras', 25, 10),
    (N'Huevos', N'Frescos', N'unidades', 120, 60),
    (N'Leche Entera', N'Lacteos', N'litros', 15, 8),
    (N'Chocolate en Polvo', N'Sabores', N'libras', 8, 5),
    (N'Vainilla Liquida', N'Sabores', N'ml', 500, 200),
    (N'Crema de Leche', N'Lacteos', N'litros', 10, 5),
    (N'Fresas', N'Frutas', N'libras', 3, 5),
    (N'Queso Crema', N'Lacteos', N'libras', 12, 6);
END
GO

-- Pedidos de ejemplo para planificacion
IF NOT EXISTS (SELECT 1 FROM pedidos)
BEGIN
    INSERT INTO pedidos (id_cliente, id_producto, fecha_pedido, fecha_entrega, libras, total, adelanto, observaciones, estado) VALUES
    (3, 1, GETDATE(), DATEADD(DAY, 2, GETDATE()), 2.5, 2000, 1000, '', 'Confirmado'),
    (3, 2, GETDATE(), DATEADD(DAY, 3, GETDATE()), 1.5, 1050, 500, '', 'En produccion'),
    (3, 3, GETDATE(), DATEADD(DAY, 5, GETDATE()), 3.0, 2700, 1350, '', 'Confirmado'),
    (3, 4, GETDATE(), DATEADD(DAY, -1, GETDATE()), 2.0, 2000, 2000, '', 'Entregado'),
    (3, 5, GETDATE(), DATEADD(DAY, 4, GETDATE()), 1.8, 1530, 765, '', 'Pendiente');
END
GO

-- ============================================
-- DATOS ADICIONALES
-- ============================================

-- Mas clientes
IF NOT EXISTS (SELECT 1 FROM clientes WHERE id_cliente = 4)
BEGIN
    SET IDENTITY_INSERT clientes ON;
    INSERT INTO clientes (id_cliente, nombre, apellido, telefono, email, usuario, contrasena, estado) VALUES
    (4, N'Maria', N'Lopez', '809-555-1002', 'mlopez@email.com', 'maria', '123456', 'Activo'),
    (5, N'Juan', N'Rodriguez', '809-555-1003', 'jrodriguez@email.com', 'juan', '123456', 'Activo'),
    (6, N'Ana', N'Martinez', '809-555-1004', 'amartinez@email.com', 'ana', '123456', 'Activo'),
    (7, N'Carlos', N'Sanchez', '809-555-1005', 'csanchez@email.com', 'carlos', '123456', 'Activo'),
    (8, N'Rosa', N'Ramirez', '809-555-1006', 'rramirez@email.com', 'rosa', '123456', 'Inactivo');
    SET IDENTITY_INSERT clientes OFF;
END
GO

-- Mas empleados
IF NOT EXISTS (SELECT 1 FROM empleados WHERE id_empleado = 11)
BEGIN
    SET IDENTITY_INSERT empleados ON;
    INSERT INTO empleados (id_empleado, nombre, cedula, telefono, area, estado) VALUES
    (11, N'Pedro Perez', '001-1111111-1', '809-555-2010', N'Decoracion', 'Activo'),
    (12, N'Luisa Gomez', '001-2222222-2', '809-555-2011', N'Delivery', 'Activo'),
    (13, N'Jose Hernandez', '001-3333333-3', '809-555-2012', N'Limpieza', 'Activo'),
    (14, N'Carmen Diaz', '001-4444444-4', '809-555-2013', N'Produccion', 'Inactivo');
    SET IDENTITY_INSERT empleados OFF;
END
GO

-- Mas maquinas
IF NOT EXISTS (SELECT 1 FROM maquinas WHERE id_maquina = 6)
BEGIN
    INSERT INTO maquinas (nombre, utilidad, estado) VALUES
    (N'Laminadora', N'Laminado de masas para pasteleria', 'Operativo'),
    (N'Camara de Fermentacion', N'Control de temperatura para fermentacion', 'Mantenimiento');
END
GO

-- Mas proveedores
IF NOT EXISTS (SELECT 1 FROM proveedores WHERE id_proveedor = 4)
BEGIN
    INSERT INTO proveedores (nombre, contacto, telefono, email, direccion) VALUES
    (N'Distribuidora Ozama', N'Roberto Santos', '809-555-0104', 'rsantos@ozama.com', N'Av. Mella 200'),
    (N'Comercial del Este', N'Laura Fernandez', '809-555-0105', 'lfernandez@deleste.com', N'Calle 27 de Febrero 150');
END
GO

-- Registros de actividad de ejemplo
IF NOT EXISTS (SELECT 1 FROM actividad)
BEGIN
    INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES
    (DATEADD(DAY, -1, GETDATE()), 'admin', 'CREAR PEDIDO', 'Nuevo pedido creado: #1'),
    (DATEADD(HOUR, -20, GETDATE()), 'admin', 'ACTUALIZAR INVENTARIO', 'Stock actualizado: Harina de Trigo'),
    (DATEADD(HOUR, -18, GETDATE()), 'empleado', 'INICIAR PRODUCCION', 'Produccion iniciada para pedido #1'),
    (DATEADD(HOUR, -6, GETDATE()), 'admin', 'ACTUALIZAR ESTADO', 'Pedido #1 marcado como Listo'),
    (DATEADD(HOUR, -2, GETDATE()), 'cliente', 'CREAR PEDIDO', 'Nuevo pedido creado: #5'),
    (GETDATE(), 'admin', 'INICIAR SESION', 'Administrador ha iniciado sesion');
END
GO

-- Registros de limpieza de ejemplo
IF NOT EXISTS (SELECT 1 FROM limpieza)
BEGIN
    INSERT INTO limpieza (area, descripcion, responsable, fecha_limpieza) VALUES
    (N'Cocina Principal', N'Limpieza general de superficies y pisos', N'Jose Hernandez', DATEADD(DAY, -3, GETDATE())),
    (N'Area de Hornos', N'Limpieza de hornos y extractores', N'Jose Hernandez', DATEADD(DAY, -2, GETDATE())),
    (N'Camara de Frio', N'Limpieza y organizacion de camaras', N'Luisa Gomez', DATEADD(DAY, -1, GETDATE())),
    (N'Salon de Ventas', N'Aseo de vitrinas y mostrador', N'Carmen Diaz', GETDATE());
END
GO

-- Mas pedidos (data historica)
IF NOT EXISTS (SELECT 1 FROM pedidos WHERE id_pedido = 6)
BEGIN
    INSERT INTO pedidos (id_cliente, id_producto, fecha_pedido, fecha_entrega, libras, total, adelanto, observaciones, estado) VALUES
    (4, 6, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -8, GETDATE()), 2.0, 1500, 1500, N'Pastel para cumpleanos', 'Entregado'),
    (5, 7, DATEADD(DAY, -8, GETDATE()), DATEADD(DAY, -6, GETDATE()), 1.5, 1425, 700, N'Cheesecake con frutos rojos', 'Entregado'),
    (6, 8, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -3, GETDATE()), 1.0, 600, 600, N'Docena de macarons variados', 'Entregado'),
    (7, 9, DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -2, GETDATE()), 0.5, 450, 450, N'Cupcakes de vainilla', 'Entregado'),
    (3, 10, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, 1, GETDATE()), 3.5, 4200, 2000, N'Pastel personalizado con diseno de flores', 'En produccion'),
    (8, 1, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, 2, GETDATE()), 2.0, 1600, 800, N'', 'Confirmado'),
    (4, 3, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, 7, GETDATE()), 4.0, 3600, 1800, N'Pastel de fresas para boda', 'Pendiente'),
    (5, 4, GETDATE(), DATEADD(DAY, 5, GETDATE()), 2.5, 2500, 0, N'', 'Pendiente'),
    (6, 5, GETDATE(), DATEADD(DAY, 4, GETDATE()), 1.8, 1530, 765, N'Pastel de zanahoria sin nueces', 'Confirmado'),
    (7, 7, GETDATE(), DATEADD(DAY, 6, GETDATE()), 2.0, 1900, 0, N'', 'Pendiente');
END
GO

-- ====================================================================
-- RECETAS - 32 recetas semilla con ingredientes y pasos
-- ====================================================================
IF NOT EXISTS (SELECT 1 FROM recetas)
BEGIN
    SET IDENTITY_INSERT recetas ON;

    INSERT INTO recetas (id_receta, id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio) VALUES
    (1, 1, N'Pastel de Chocolate Clasico', N'Pastel de chocolate humedo con frosting de chocolate', N'Pasteles', 90, 1, 12, 350.00, 95, 5),
    (2, 2, N'Pastel de Vainilla con Frosting', N'Pastel de vainilla esponjoso con frosting de mantequilla', N'Pasteles', 80, 1, 12, 300.00, 95, 5),
    (3, 3, N'Pastel de Fresas con Crema', N'Pastel con fresas naturales y crema batida', N'Pasteles', 85, 1, 10, 400.00, 93, 7),
    (4, 4, N'Red Velvet Cream Cheese', N'Pastel red velvet con cobertura de queso crema', N'Pasteles', 100, 1, 12, 420.00, 94, 6),
    (5, 5, N'Pastel de Zanahoria con Nueces', N'Pastel de zanahoria con nueces y frosting de queso', N'Pasteles', 90, 1, 12, 380.00, 95, 5),
    (6, 6, N'Tres Leches Tradicional', N'Pastel tres leches banado en leches evaporada, condensada y crema', N'Pasteles', 70, 1, 14, 320.00, 96, 4),
    (7, 7, N'Cheesecake de Frutos Rojos', N'Cheesecake cremoso con cobertura de frutos rojos', N'Pasteles', 120, 1, 12, 450.00, 95, 5),
    (8, 1, N'Pastel de Chocolate Oscuro', N'Pastel de chocolate semiamargo con ganache', N'Pasteles', 95, 1, 10, 380.00, 94, 6),
    (9, 2, N'Pastel de Vainilla y Frutas', N'Pastel de vainilla con relleno de frutas mixtas', N'Pasteles', 85, 1, 12, 350.00, 95, 5),
    (10, 3, N'Pastel Fresa con Chocolate Blanco', N'Pastel de fresas cubierto de chocolate blanco', N'Pasteles', 90, 1, 10, 420.00, 93, 7),
    (11, 4, N'Red Velvet con Chocolate Blanco', N'Red velvet con chispas y cobertura de chocolate blanco', N'Pasteles', 105, 1, 12, 440.00, 94, 6),
    (12, 5, N'Pastel de Zanahoria y Coco', N'Pastel de zanahoria con coco rallado y frosting', N'Pasteles', 85, 1, 12, 360.00, 95, 5),
    (13, 6, N'Tres Leches con Cajeta', N'Pastel tres leches banado en cajeta y caramelo', N'Pasteles', 80, 1, 14, 380.00, 95, 5),
    (14, 7, N'Cheesecake de Oreo', N'Cheesecake con base de galleta Oreo y trozos', N'Pasteles', 130, 1, 12, 480.00, 94, 6),
    (15, 8, N'Macarons de Chocolate', N'Macarons rellenos de ganache de chocolate', N'Reposteria Fina', 60, 12, 12, 250.00, 92, 8),
    (16, 8, N'Macarons de Vainilla', N'Macarons rellenos de crema de vainilla francesa', N'Reposteria Fina', 60, 12, 12, 240.00, 92, 8),
    (17, 8, N'Macarons de Fresa', N'Macarons rellenos de buttercream de fresa natural', N'Reposteria Fina', 65, 12, 12, 260.00, 91, 9),
    (18, 9, N'Cupcakes Red Velvet', N'Cupcakes red velvet con frosting de queso crema', N'Reposteria Fina', 50, 6, 6, 180.00, 95, 5),
    (19, 9, N'Cupcakes de Vainilla', N'Cupcakes de vainilla con buttercream de colores', N'Reposteria Fina', 45, 6, 6, 150.00, 96, 4),
    (20, 9, N'Cupcakes de Chocolate', N'Cupcakes de chocolate con frosting de chocolate', N'Reposteria Fina', 50, 6, 6, 170.00, 95, 5),
    (21, 10, N'Pastel Personalizado Bodas', N'Pastel de bodas de 3 pisos con diseno personalizado', N'Decoracion', 240, 1, 50, 1500.00, 95, 5),
    (22, 10, N'Pastel Personalizado Cumpleanos', N'Pastel tematico de cumpleanos con figuras de fondant', N'Decoracion', 180, 1, 20, 850.00, 94, 6),
    (23, 7, N'Cheesecake de Maracuya', N'Cheesecake ligero con pulpa de maracuya', N'Postres', 110, 1, 12, 430.00, 95, 5),
    (24, 1, N'Pastel Marmoleado', N'Pastel con vetas de chocolate y vainilla', N'Pasteles', 75, 1, 12, 320.00, 95, 5),
    (25, 1, N'Pastel Aleman de Chocolate', N'Pastel de chocolate con coco y nueces', N'Pasteles', 100, 1, 12, 400.00, 93, 7),
    (26, 2, N'Pastel de Coco y Limon', N'Pastel de coco con toque de limon y glaseado', N'Pasteles', 70, 1, 10, 310.00, 95, 5),
    (27, 2, N'Pastel de Limon Merengado', N'Pastel de limon con merengue suizo tostado', N'Pasteles', 90, 1, 10, 340.00, 94, 6),
    (28, 1, N'Pastel de Cafe con Nueces', N'Pastel de cafe con nueces caramelizadas y frosting', N'Pasteles', 85, 1, 12, 370.00, 95, 5),
    (29, 7, N'Cheesecake de Mango', N'Cheesecake cremoso con pure de mango', N'Postres', 120, 1, 12, 440.00, 95, 5),
    (30, 1, N'Pastel de Chocolate Blanco y Frambuesa', N'Pastel de chocolate blanco con frambuesas frescas', N'Pasteles', 95, 1, 10, 420.00, 93, 7),
    (31, 2, N'Pastel de Elote', N'Pastel dulce de elote con crema y canela', N'Panaderia', 65, 1, 10, 260.00, 95, 5),
    (32, 2, N'Pastel de Platano Maduro', N'Pastel de platano maduro con canela y nueces', N'Panaderia', 65, 1, 10, 280.00, 95, 5);

    SET IDENTITY_INSERT recetas OFF;

    -- Receta Ingredientes
    INSERT INTO receta_ingredientes (id_receta, id_ingrediente, cantidad) VALUES
    (1, 1, 0.5), (1, 2, 0.4), (1, 3, 0.25), (1, 4, 3), (1, 5, 0.15), (1, 6, 0.2),
    (2, 1, 0.5), (2, 2, 0.4), (2, 3, 0.25), (2, 4, 3), (2, 5, 0.15), (2, 7, 5),
    (3, 1, 0.4), (3, 2, 0.35), (3, 3, 0.2), (3, 4, 2), (3, 8, 0.2), (3, 9, 0.5),
    (4, 1, 0.5), (4, 2, 0.45), (4, 3, 0.3), (4, 4, 3), (4, 5, 0.1), (4, 10, 0.3),
    (5, 1, 0.4), (5, 2, 0.4), (5, 3, 0.2), (5, 4, 3), (5, 7, 5), (5, 10, 0.2),
    (6, 1, 0.4), (6, 2, 0.35), (6, 3, 0.2), (6, 4, 4), (6, 5, 0.3), (6, 8, 0.2),
    (7, 10, 0.5), (7, 2, 0.3), (7, 4, 3), (7, 8, 0.2), (7, 9, 0.3), (7, 3, 0.15),
    (8, 1, 0.5), (8, 2, 0.35), (8, 3, 0.25), (8, 4, 3), (8, 6, 0.25), (8, 8, 0.15),
    (9, 1, 0.5), (9, 2, 0.4), (9, 3, 0.25), (9, 4, 3), (9, 7, 5), (9, 9, 0.3),
    (10, 1, 0.45), (10, 2, 0.4), (10, 3, 0.25), (10, 4, 3), (10, 9, 0.5), (10, 5, 0.1),
    (11, 1, 0.5), (11, 2, 0.45), (11, 3, 0.3), (11, 4, 3), (11, 5, 0.1), (11, 10, 0.25),
    (12, 1, 0.4), (12, 2, 0.4), (12, 3, 0.2), (12, 4, 3), (12, 7, 5), (12, 8, 0.1),
    (13, 1, 0.4), (13, 2, 0.4), (13, 3, 0.2), (13, 4, 4), (13, 5, 0.3), (13, 8, 0.2),
    (14, 10, 0.5), (14, 2, 0.3), (14, 3, 0.2), (14, 4, 3), (14, 8, 0.2),
    (15, 1, 0.2), (15, 2, 0.25), (15, 4, 2), (15, 6, 0.1), (15, 8, 0.1),
    (16, 1, 0.2), (16, 2, 0.25), (16, 4, 2), (16, 7, 5), (16, 3, 0.1),
    (17, 1, 0.2), (17, 2, 0.25), (17, 4, 2), (17, 9, 0.15), (17, 3, 0.1),
    (18, 1, 0.3), (18, 2, 0.25), (18, 3, 0.15), (18, 4, 2), (18, 10, 0.15), (18, 7, 3),
    (19, 1, 0.3), (19, 2, 0.25), (19, 3, 0.15), (19, 4, 2), (19, 5, 0.1), (19, 7, 3),
    (20, 1, 0.3), (20, 2, 0.25), (20, 3, 0.15), (20, 4, 2), (20, 6, 0.1), (20, 5, 0.08),
    (21, 1, 3.0), (21, 2, 2.5), (21, 3, 1.5), (21, 4, 18), (21, 5, 1.0), (21, 7, 30),
    (22, 1, 1.5), (22, 2, 1.0), (22, 3, 0.75), (22, 4, 8), (22, 5, 0.4), (22, 7, 15),
    (23, 10, 0.5), (23, 2, 0.3), (23, 4, 3), (23, 8, 0.2), (23, 9, 0.25),
    (24, 1, 0.5), (24, 2, 0.4), (24, 3, 0.25), (24, 4, 3), (24, 6, 0.1), (24, 7, 3),
    (25, 1, 0.5), (25, 2, 0.4), (25, 3, 0.3), (25, 4, 3), (25, 6, 0.2), (25, 5, 0.1),
    (26, 1, 0.4), (26, 2, 0.35), (26, 3, 0.2), (26, 4, 3), (26, 8, 0.15), (26, 5, 0.1),
    (27, 1, 0.4), (27, 2, 0.4), (27, 3, 0.2), (27, 4, 3), (27, 5, 0.1), (27, 7, 3),
    (28, 1, 0.5), (28, 2, 0.4), (28, 3, 0.25), (28, 4, 3), (28, 8, 0.15), (28, 5, 0.1),
    (29, 10, 0.5), (29, 2, 0.3), (29, 4, 3), (29, 8, 0.2), (29, 9, 0.3),
    (30, 1, 0.45), (30, 2, 0.35), (30, 3, 0.25), (30, 4, 3), (30, 5, 0.1), (30, 8, 0.15),
    (31, 1, 0.3), (31, 2, 0.3), (31, 3, 0.2), (31, 4, 3), (31, 5, 0.2), (31, 8, 0.1),
    (32, 1, 0.4), (32, 2, 0.35), (32, 3, 0.2), (32, 4, 3), (32, 7, 5), (32, 5, 0.1);

    -- Receta Pasos
    INSERT INTO receta_pasos (id_receta, numero_paso, titulo, descripcion, tiempo_estimado) VALUES
    (1, 1, N'Preparar ingredientes', N'Pesar y medir todos los ingredientes a temperatura ambiente', 15),
    (1, 2, N'Mezclar secos', N'Tamizar harina, polvo de hornear y sal. Reservar', 10),
    (1, 3, N'Crema de mantequilla', N'Batir mantequilla y azucar hasta crema esponjosa. Agregar huevos uno a uno', 10),
    (1, 4, N'Incorporar secos', N'Agregar ingredientes secos alternando con leche. Mezclar hasta integrar', 10),
    (1, 5, N'Hornear', N'Verter en molde engrasado. Hornear a 350F por 35 min', 35),
    (1, 6, N'Enfriar y decorar', N'Enfriar sobre rejilla. Decorar con frosting y toppings', 15),
    (2, 1, N'Preparar ingredientes', N'Pesar y medir todos los ingredientes a temperatura ambiente', 15),
    (2, 2, N'Batir mantequilla', N'Batir mantequilla y azucar hasta blanquear. Agregar vainilla', 10),
    (2, 3, N'Incorporar huevos', N'Agregar huevos uno a uno batiendo bien despues de cada uno', 5),
    (2, 4, N'Agregar secos', N'Alternar harina tamizada con leche. Mezclar suavemente', 10),
    (2, 5, N'Hornear', N'Hornear a 350F por 30-35 minutos. Probar con palillo', 35),
    (2, 6, N'Frosting y decorar', N'Preparar frosting de mantequilla. Decorar al gusto', 15),
    (3, 1, N'Preparar fresas', N'Lavar, cortar y macerar fresas con un poco de azucar', 15),
    (3, 2, N'Batir base', N'Batir mantequilla y azucar. Agregar huevos y vainilla', 10),
    (3, 3, N'Mezclar', N'Incorporar harina y leche alternadamente', 10),
    (3, 4, N'Hornear', N'Hornear en molde preparado a 350F por 30 min', 30),
    (3, 5, N'Montar pastel', N'Cortar en capas, rellenar con crema y fresas. Decorar', 15),
    (4, 1, N'Preparar ingredientes', N'Sacar mantequilla y huevos a temperatura ambiente', 15),
    (4, 2, N'Batir color', N'Batir mantequilla, azucar y colorante rojo hasta cremosa', 10),
    (4, 3, N'Agregar secos', N'Alternar harina con suero de leche. Agregar vinagre y vainilla', 10),
    (4, 4, N'Hornear', N'Hornear a 350F por 30-35 minutos', 35),
    (4, 5, N'Decorar con queso crema', N'Preparar frosting de queso crema. Decorar pastel', 15),
    (5, 1, N'Preparar zanahoria', N'Rallar zanahorias finamente y picar nueces', 15),
    (5, 2, N'Mezclar humedos', N'Batir aceite, azucar, huevos y vainilla', 10),
    (5, 3, N'Incorporar secos', N'Agregar harina, canela y polvo de hornear. Agregar zanahoria y nueces', 10),
    (5, 4, N'Hornear', N'Hornear a 350F por 35-40 minutos', 40),
    (5, 5, N'Frosting y servir', N'Preparar frosting de queso crema. Cubrir y decorar', 15),
    (6, 1, N'Hornear bizcocho', N'Preparar y hornear bizcocho de vainilla en molde rectangular', 35),
    (6, 2, N'Preparar mezcla de leches', N'Mezclar leche evaporada, condensada y crema de leche. Agregar vainilla', 10),
    (6, 3, N'Baniar bizcocho', N'Pinchar bizcocho con tenedor. Verter mezcla de leches lentamente', 10),
    (6, 4, N'Refrigerar', N'Refrigerar minimo 4 horas o toda la noche', 240),
    (6, 5, N'Decorar y servir', N'Cubrir con crema batida y canela al gusto', 10),
    (7, 1, N'Preparar base', N'Triturar galletas, mezclar con mantequilla derretida. Prensar en molde', 15),
    (7, 2, N'Batir relleno', N'Batir queso crema con azucar. Agregar huevos y crema', 15),
    (7, 3, N'Hornear cheesecake', N'Verter sobre base. Hornear a 325F en bano maria por 50 min', 50),
    (7, 4, N'Enfriar', N'Enfriar gradualmente. Refrigerar 4 horas minimo', 240),
    (7, 5, N'Agregar frutos rojos', N'Cubrir con salsa de frutos rojos. Servir frio', 10),
    (8, 1, N'Preparar y hornear', N'Seguir metodo de pastel de chocolate. Agregar chocolate derretido extra', 15),
    (8, 2, N'Ganache', N'Preparar ganache con chocolate semiamargo y crema', 10),
    (8, 3, N'Cubrir y decorar', N'Cubrir pastel con ganache. Decorar con virutas de chocolate', 10),
    (9, 1, N'Preparar y hornear', N'Preparar masa de vainilla y hornear', 40),
    (9, 2, N'Preparar frutas', N'Cortar frutas mixtas en cubos pequenos', 10),
    (9, 3, N'Montar pastel', N'Cortar en capas, rellenar con crema y frutas', 15),
    (10, 1, N'Preparar pastel fresa', N'Preparar masa de fresa y hornear', 40),
    (10, 2, N'Derretir chocolate', N'Derretir chocolate blanco al bano maria', 10),
    (10, 3, N'Cubrir y decorar', N'Cubrir con chocolate blanco y decorar con fresas', 10),
    (11, 1, N'Preparar red velvet', N'Preparar masa red velvet con chispas de chocolate blanco', 15),
    (11, 2, N'Hornear', N'Hornear a 350F por 30 min', 30),
    (11, 3, N'Decorar', N'Cubrir con frosting de chocolate blanco', 15),
    (12, 1, N'Preparar zanahoria', N'Rallar zanahoria y tostar coco rallado', 10),
    (12, 2, N'Mezclar y hornear', N'Preparar masa con coco y zanahoria. Hornear', 40),
    (12, 3, N'Frosting y coco', N'Cubrir con frosting de queso crema y espolvorear coco', 10),
    (13, 1, N'Hornear bizcocho', N'Preparar bizcocho tres leches y hornear', 35),
    (13, 2, N'Mezclar con cajeta', N'Mezclar leches con cajeta. Banar bizcocho', 15),
    (13, 3, N'Refrigerar', N'Refrigerar y decorar con cajeta extra', 240),
    (14, 1, N'Preparar base oreo', N'Triturar oreos con mantequilla. Prensar en molde', 15),
    (14, 2, N'Batir y hornear', N'Batir relleno con trozos de oreo. Hornear', 50),
    (14, 3, N'Decorar', N'Cubrir con crema y trozos de oreo', 10),
    (15, 1, N'Hacer macarons', N'Preparar masa de macarons con harina de almendra. Formar circulos', 20),
    (15, 2, N'Reposar y hornear', N'Reposar 30 min hasta formar piel. Hornear a 300F', 40),
    (15, 3, N'Rellenar', N'Preparar ganache de chocolate. Rellenar y armar macarons', 15),
    (16, 1, N'Hacer macarons vainilla', N'Preparar masa de macarons. Agregar extracto de vainilla', 20),
    (16, 2, N'Reposar y hornear', N'Reposar y hornear macarons', 40),
    (16, 3, N'Relleno vainilla', N'Preparar crema de vainilla francesa. Rellenar', 15),
    (17, 1, N'Hacer macarons fresa', N'Preparar masa de macarons. Agregar colorante rosa', 20),
    (17, 2, N'Reposar y hornear', N'Reposar y hornear macarons', 40),
    (17, 3, N'Relleno fresa', N'Preparar buttercream de fresa natural. Rellenar', 15),
    (18, 1, N'Preparar masa cupcakes', N'Preparar masa red velvet para 6 cupcakes', 15),
    (18, 2, N'Hornear', N'Hornear a 350F por 18-20 min', 20),
    (18, 3, N'Decorar', N'Decorar con frosting de queso crema y chispas', 10),
    (19, 1, N'Preparar masa vainilla', N'Preparar masa de vainilla para 6 cupcakes', 10),
    (19, 2, N'Hornear', N'Hornear a 350F por 18 min', 18),
    (19, 3, N'Buttercream y decorar', N'Preparar buttercream de colores. Decorar cupcakes', 15),
    (20, 1, N'Preparar masa chocolate', N'Preparar masa de chocolate para 6 cupcakes', 10),
    (20, 2, N'Hornear', N'Hornear a 350F por 18 min', 18),
    (20, 3, N'Frosting chocolate', N'Preparar frosting de chocolate. Decorar', 15),
    (21, 1, N'Diseniar estructura', N'Diseniar pastel de 3 pisos con estructura de soporte', 30),
    (21, 2, N'Hornear capas', N'Hornear capas para los 3 pisos. Enfriar completamente', 90),
    (21, 3, N'Rellenar y forrar', N'Rellenar cada piso. Forrar con fondant', 60),
    (21, 4, N'Montar y decorar', N'Montar pisos con soportes. Decorar segun diseno acordado', 90),
    (22, 1, N'Diseniar tematica', N'Diseniar pastel segun tematica del cumpleanos', 20),
    (22, 2, N'Hornear base', N'Hornear base de 2 pisos. Enfriar', 60),
    (22, 3, N'Decorar con fondant', N'Cubrir con fondant y crear figuras decorativas', 60),
    (22, 4, N'Detalles finales', N'Agregar velas, letrero y detalles personalizados', 30),
    (23, 1, N'Base galleta', N'Preparar base de galleta con mantequilla', 10),
    (23, 2, N'Relleno maracuya', N'Batir queso crema con pulpa de maracuya y huevos', 15),
    (23, 3, N'Hornear y enfriar', N'Hornear a 325F en bano maria. Enfriar 4 horas', 240),
    (29, 1, N'Base galleta', N'Preparar base de galleta con mantequilla', 10),
    (29, 2, N'Relleno mango', N'Batir queso crema con pure de mango. Agregar huevos', 15),
    (29, 3, N'Hornear y enfriar', N'Hornear a 325F en bano maria. Refrigerar 4 horas', 240),
    (28, 1, N'Preparar cafe', N'Preparar cafe fuerte y dejar enfriar', 10),
    (28, 2, N'Mezclar y hornear', N'Batir masa con cafe. Agregar nueces. Hornear', 40),
    (28, 3, N'Frosting cafe', N'Preparar frosting de cafe. Decorar con nueces', 10),
    (26, 1, N'Preparar coco', N'Mezclar harina con coco rallado. Rayar limon', 10),
    (26, 2, N'Hornear', N'Preparar masa y hornear a 350F por 30 min', 30),
    (26, 3, N'Glaseado limon', N'Preparar glaseado de limon. Cubrir pastel', 10),
    (27, 1, N'Preparar limon', N'Exprimir y rallar limones. Reservar', 10),
    (27, 2, N'Pastel y hornear', N'Preparar masa de limon. Hornear', 35),
    (27, 3, N'Merengue suizo', N'Preparar merengue suizo. Tostar con soplete', 15),
    (24, 1, N'Preparar dos masas', N'Preparar masa de vainilla y masa de chocolate por separado', 15),
    (24, 2, N'Marmolear', N'Verter masas alternando. Hacer vetas con cuchillo', 5),
    (24, 3, N'Hornear', N'Hornear a 350F por 35 min', 35),
    (25, 1, N'Preparar masa', N'Batir mantequilla, azucar y chocolate derretido', 15),
    (25, 2, N'Agregar coco y nueces', N'Incorporar coco rallado y nueces picadas a la masa', 5),
    (25, 3, N'Hornear y decorar', N'Hornear. Cubrir con ganache y coco tostado', 40),
    (30, 1, N'Derretir chocolate', N'Derretir chocolate blanco al bano maria', 10),
    (30, 2, N'Preparar masa', N'Batir mantequilla, azucar y chocolate blanco derretido', 15),
    (30, 3, N'Hornear y decorar', N'Hornear. Decorar con frambuesas y chocolate blanco rallado', 35),
    (31, 1, N'Licuar elote', N'Licuar granos de elote con leche y canela', 10),
    (31, 2, N'Mezclar y hornear', N'Mezclar con harina, azucar y mantequilla. Hornear', 35),
    (31, 3, N'Servir', N'Enfriar y espolvorear con canela y azucar glass', 10),
    (32, 1, N'Machacar platanos', N'Machacar platanos maduros con tenedor', 10),
    (32, 2, N'Mezclar y hornear', N'Batir mantequilla, azucar, platano y harina. Hornear', 35),
    (32, 3, N'Decorar', N'Cubrir con frosting de canela y decorar con rodajas de platano', 10);
END
GO

PRINT N'Base de datos Reposteria creada exitosamente.';
GO
