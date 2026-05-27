-- ============================================================
-- SCRIPT COMPLETO: Crear tablas faltantes + 30 recetas de ejemplo
-- IDEMPOTENTE: Se puede ejecutar multiples veces sin perder datos
-- ============================================================

-- 0. Asegurar columnas necesarias en tabla productos
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'precio_base')
    ALTER TABLE productos ADD precio_base DECIMAL(12,2) DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'precio_unitario')
    ALTER TABLE productos ADD precio_unitario DECIMAL(12,2) DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'costo_disenio')
    ALTER TABLE productos ADD costo_disenio DECIMAL(12,2) DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'descripcion')
    ALTER TABLE productos ADD descripcion NVARCHAR(MAX);
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('productos') AND name = 'estado')
    ALTER TABLE productos ADD estado NVARCHAR(10) DEFAULT 'Activo';
GO

-- 0b. Crear ingredientes si no existe
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ingredientes' AND xtype='U')
    CREATE TABLE ingredientes (
        id_ingrediente INT IDENTITY(1,1) PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL,
        categoria NVARCHAR(50),
        unidad NVARCHAR(20),
        stock_actual DECIMAL(12,2) DEFAULT 0,
        stock_minimo DECIMAL(12,2) DEFAULT 0,
        fecha_registro DATETIME DEFAULT GETDATE(),
        estado NVARCHAR(10) DEFAULT 'Activo');
GO

-- 0c. Crear recetas si no existe
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='recetas' AND xtype='U')
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
        estado NVARCHAR(10) DEFAULT 'Activo',
        FOREIGN KEY (id_producto) REFERENCES productos(id_producto));
GO

-- 0d. Crear receta_ingredientes si no existe
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='receta_ingredientes' AND xtype='U')
    CREATE TABLE receta_ingredientes (
        id_receta INT NOT NULL,
        id_ingrediente INT NOT NULL,
        cantidad DECIMAL(12,2) DEFAULT 0,
        PRIMARY KEY (id_receta, id_ingrediente),
        FOREIGN KEY (id_receta) REFERENCES recetas(id_receta),
        FOREIGN KEY (id_ingrediente) REFERENCES ingredientes(id_ingrediente));
GO

-- 0e. Crear receta_pasos si no existe
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='receta_pasos' AND xtype='U')
    CREATE TABLE receta_pasos (
        id_paso INT IDENTITY(1,1) PRIMARY KEY,
        id_receta INT NOT NULL,
        numero_paso INT NOT NULL,
        titulo NVARCHAR(200),
        descripcion NVARCHAR(MAX),
        tiempo_estimado INT DEFAULT 0,
        imagen_ref NVARCHAR(500),
        FOREIGN KEY (id_receta) REFERENCES recetas(id_receta));
GO

-- 1. Insertar ingredientes base si no existen
IF NOT EXISTS (SELECT 1 FROM ingredientes)
BEGIN
    INSERT INTO ingredientes (nombre, categoria, unidad, stock_actual, stock_minimo) VALUES
    ('Harina de Trigo', 'Harinas', 'libras', 50, 20),
    ('Azucar Blanca', 'Endulzantes', 'libras', 40, 15),
    ('Mantequilla', 'Lacteos', 'libras', 25, 10),
    ('Huevos', 'Frescos', 'unidades', 120, 60),
    ('Leche Entera', 'Lacteos', 'litros', 15, 8),
    ('Chocolate en Polvo', 'Sabores', 'libras', 8, 5),
    ('Vainilla Liquida', 'Sabores', 'ml', 500, 200),
    ('Crema de Leche', 'Lacteos', 'litros', 10, 5),
    ('Fresas', 'Frutas', 'libras', 3, 5),
    ('Queso Crema', 'Lacteos', 'libras', 12, 6);
END
GO

-- 2. Insertar productos base si no existen
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Chocolate')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Chocolate', 450, 450, 'Pastel clasico de chocolate con cobertura de ganache');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Vainilla')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Vainilla', 400, 400, 'Pastel esponjoso de vainilla con relleno de crema');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Fresa')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Fresa', 480, 480, 'Pastel de fresa con crema y fresas naturales');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Zanahoria')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Zanahoria', 420, 420, 'Pastel humedo de zanahoria con nueces y frosting de queso');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Cheesecake')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Cheesecake', 520, 520, 'Cheesecake cremoso estilo Nueva York');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Red Velvet')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Red Velvet', 500, 500, 'Pastel Red Velvet con frosting de queso crema');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Tres Leches')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Tres Leches', 380, 380, 'Pastel Tres Leches bañado en tres tipos de leche');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Ron')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Ron', 460, 460, 'Pastel borracho con ron y pasas');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Galletas Decoradas')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Galletas Decoradas', 250, 250, 'Galletas de mantequilla decoradas con fondant');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Cupcakes')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Cupcakes', 150, 150, 'Cupcakes variados con topping de crema');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Macarons')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Macarons', 350, 350, 'Macarons franceses de colores');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Brownies')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Brownies', 180, 180, 'Brownies de chocolate con nueces');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pie de Limon')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pie de Limon', 320, 320, 'Pie de limon con merengue');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Tiramisu')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Tiramisu', 550, 550, 'Tiramisu clasico italiano');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Mousse de Chocolate')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Mousse de Chocolate', 280, 280, 'Mousse aireado de chocolate belga');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Flan Napolitano')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Flan Napolitano', 300, 300, 'Flan cremoso de caramelo');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Queso')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Queso', 490, 490, 'Pastel de queso horneado con frambuesas');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Hoja de Menta')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Hoja de Menta', 360, 360, 'Pastel de menta con chocolate blanco');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Coco')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Coco', 410, 410, 'Pastel de coco rallado con crema');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Cafe')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Cafe', 430, 430, 'Pastel de cafe con frosting de moka');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Roll de Canela')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Roll de Canela', 220, 220, 'Roll de canela con glaseado');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Panqueques')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Panqueques', 160, 160, 'Panqueques con sirope de arce');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Waffles')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Waffles', 200, 200, 'Waffles belgas con frutas');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Churros Rellenos')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Churros Rellenos', 180, 180, 'Churros rellenos de dulce de leche');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Manzana')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Manzana', 440, 440, 'Pastel de manzana con crumble');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Tarta de Frutas')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Tarta de Frutas', 480, 480, 'Tarta de frutas mixtas con crema pastelera');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pavlova')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pavlova', 560, 560, 'Pavlova de merengue con frutas');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Creme Brulee')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Creme Brulee', 340, 340, 'Creme brulee clasico de vainilla');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Profiteroles')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Profiteroles', 290, 290, 'Profiteroles rellenos de crema');
IF NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pastel de Cumpleaños')
    INSERT INTO productos (nombre, precio_base, precio_unitario, descripcion) VALUES ('Pastel de Cumpleaños', 600, 600, 'Pastel decorado para cumpleaños');
GO

-- 3. Insertar 30 recetas
DECLARE @cat_pastel NVARCHAR(50) = 'Pasteles';
DECLARE @cat_galleta NVARCHAR(50) = 'Galletas';
DECLARE @cat_postre NVARCHAR(50) = 'Postres';
DECLARE @cat_especial NVARCHAR(50) = 'Especialidades';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Receta Clasica de Chocolate')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Receta Clasica de Chocolate', 'Bizcocho de chocolate humedo con ganache', @cat_pastel, 90, 1, 12, 180.00, 95, 5 FROM productos WHERE nombre = 'Pastel de Chocolate';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Receta Tradicional de Vainilla')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Receta Tradicional de Vainilla', 'Bizcocho esponjoso de vainilla con relleno de crema', @cat_pastel, 75, 1, 12, 150.00, 96, 4 FROM productos WHERE nombre = 'Pastel de Vainilla';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Receta Fresca de Fresa')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Receta Fresca de Fresa', 'Bizcocho de fresa con crema y fruta natural', @cat_pastel, 80, 1, 10, 200.00, 93, 7 FROM productos WHERE nombre = 'Pastel de Fresa';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Receta Humeda de Zanahoria')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Receta Humeda de Zanahoria', 'Bizcocho de zanahoria con nueces y frosting', @cat_pastel, 85, 1, 12, 170.00, 94, 6 FROM productos WHERE nombre = 'Pastel de Zanahoria';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Cheesecake New York')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Cheesecake New York', 'Cheesecake cremoso horneado sobre base de galleta', @cat_pastel, 120, 1, 14, 250.00, 98, 2 FROM productos WHERE nombre = 'Cheesecake';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Red Velvet Clasico')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Red Velvet Clasico', 'Bizcocho rojo terciopelo con frosting de queso crema', @cat_pastel, 95, 1, 12, 220.00, 95, 5 FROM productos WHERE nombre = 'Red Velvet';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Tres Leches Tradicional')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Tres Leches Tradicional', 'Bizcocho bañado en tres leches con crema batida', @cat_pastel, 70, 1, 12, 160.00, 97, 3 FROM productos WHERE nombre = 'Tres Leches';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel Borracho de Ron')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel Borracho de Ron', 'Bizcocho empapado en almibar de ron con pasas', @cat_pastel, 90, 1, 12, 195.00, 94, 6 FROM productos WHERE nombre = 'Pastel de Ron';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Galletas de Mantequilla Decoradas')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Galletas de Mantequilla Decoradas', 'Galletas de mantequilla con decoracion de fondant', @cat_galleta, 60, 24, 24, 90.00, 96, 4 FROM productos WHERE nombre = 'Galletas Decoradas';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Cupcakes Variados')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Cupcakes Variados', 'Cupcakes de vainilla con topping de crema de mantequilla', @cat_galleta, 45, 12, 12, 60.00, 97, 3 FROM productos WHERE nombre = 'Cupcakes';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Macarons Franceses')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Macarons Franceses', 'Macarons de almendra rellenos de ganache', @cat_galleta, 120, 20, 20, 180.00, 85, 15 FROM productos WHERE nombre = 'Macarons';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Brownies de Chocolate con Nueces')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Brownies de Chocolate con Nueces', 'Brownies densos y humedos con nueces pecanas', @cat_galleta, 50, 16, 16, 75.00, 98, 2 FROM productos WHERE nombre = 'Brownies';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pie de Limon con Merengue')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pie de Limon con Merengue', 'Base de galleta con crema de limon y merengue italiano', @cat_postre, 100, 1, 10, 130.00, 95, 5 FROM productos WHERE nombre = 'Pie de Limon';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Tiramisu Clasico')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Tiramisu Clasico', 'Capas de bizcocho empapado en cafe con mascarpone', @cat_postre, 40, 1, 10, 280.00, 98, 2 FROM productos WHERE nombre = 'Tiramisu';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Mousse Aireado de Chocolate')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Mousse Aireado de Chocolate', 'Mousse ligero de chocolate belga con nata montada', @cat_postre, 35, 1, 8, 140.00, 97, 3 FROM productos WHERE nombre = 'Mousse de Chocolate';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Flan Napolitano Cremoso')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Flan Napolitano Cremoso', 'Flan de vainilla con caramelo liquido y queso crema', @cat_postre, 90, 1, 10, 110.00, 96, 4 FROM productos WHERE nombre = 'Flan Napolitano';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel de Queso con Frambuesas')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel de Queso con Frambuesas', 'Pastel de queso horneado con coulis de frambuesa', @cat_pastel, 110, 1, 12, 260.00, 95, 5 FROM productos WHERE nombre = 'Pastel de Queso';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel de Menta y Chocolate Blanco')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel de Menta y Chocolate Blanco', 'Bizcocho de menta con capas de chocolate blanco', @cat_especial, 85, 1, 10, 190.00, 94, 6 FROM productos WHERE nombre = 'Hoja de Menta';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel de Coco Rallado')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel de Coco Rallado', 'Bizcocho de coco con crema de coco y ralladura', @cat_pastel, 75, 1, 10, 175.00, 95, 5 FROM productos WHERE nombre = 'Pastel de Coco';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel de Cafe con Moka')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel de Cafe con Moka', 'Bizcocho de cafe con frosting de moka y granos caramelizados', @cat_pastel, 80, 1, 12, 185.00, 94, 6 FROM productos WHERE nombre = 'Pastel de Cafe';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Roll de Canela con Glaseado')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Roll de Canela con Glaseado', 'Masa suave enrollada con canela y glaseado de queso', @cat_galleta, 120, 12, 12, 95.00, 96, 4 FROM productos WHERE nombre = 'Roll de Canela';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Panqueques Clasicos')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Panqueques Clasicos', 'Panqueques esponjosos con sirope de arce', @cat_postre, 25, 8, 4, 55.00, 98, 2 FROM productos WHERE nombre = 'Panqueques';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Waffles Belgas')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Waffles Belgas', 'Waffles crujientes con frutas del bosque', @cat_postre, 30, 4, 2, 85.00, 97, 3 FROM productos WHERE nombre = 'Waffles';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Churros Rellenos de Dulce de Leche')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Churros Rellenos de Dulce de Leche', 'Churros fritos rellenos de dulce de leche argentino', @cat_especial, 45, 12, 6, 70.00, 95, 5 FROM productos WHERE nombre = 'Churros Rellenos';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel de Manzana con Crumble')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel de Manzana con Crumble', 'Bizcocho de manzana con cobertura crujiente de crumble', @cat_pastel, 90, 1, 10, 190.00, 95, 5 FROM productos WHERE nombre = 'Pastel de Manzana';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Tarta de Frutas Mixtas')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Tarta de Frutas Mixtas', 'Base de masa quebrada con crema pastelera y frutas', @cat_postre, 100, 1, 10, 230.00, 93, 7 FROM productos WHERE nombre = 'Tarta de Frutas';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pavlova de Merengue')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pavlova de Merengue', 'Base de merengue crujiente con crema y frutas', @cat_especial, 130, 1, 8, 210.00, 90, 10 FROM productos WHERE nombre = 'Pavlova';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Creme Brulee de Vainilla')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Creme Brulee de Vainilla', 'Natilla horneada con capa de azucar caramelizado', @cat_postre, 40, 4, 4, 95.00, 98, 2 FROM productos WHERE nombre = 'Creme Brulee';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Profiteroles con Crema')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Profiteroles con Crema', 'Masa choux rellena de crema pastelera con chocolate', @cat_postre, 70, 12, 6, 110.00, 94, 6 FROM productos WHERE nombre = 'Profiteroles';

IF NOT EXISTS (SELECT 1 FROM recetas WHERE nombre_receta = 'Pastel Decorado de Cumpleaños')
    INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, tiempo_preparacion, cantidad_producida, porciones, costo_estimado, rendimiento, desperdicio)
    SELECT id_producto, 'Pastel Decorado de Cumpleaños', 'Bizcocho de vainilla decorado con fondant y figuras', @cat_especial, 180, 1, 20, 320.00, 96, 4 FROM productos WHERE nombre = 'Pastel de Cumpleaños';
GO

-- 4. Agregar pasos basicos para cada receta
INSERT INTO receta_pasos (id_receta, numero_paso, titulo, descripcion, tiempo_estimado)
SELECT r.id_receta, 1, 'Preparacion de ingredientes', 'Reunir y medir todos los ingredientes segun la receta. Precalentar el horno a 350°F (180°C). Engrasar y enharinar los moldes.', 15
FROM recetas r WHERE NOT EXISTS (SELECT 1 FROM receta_pasos p WHERE p.id_receta = r.id_receta AND p.numero_paso = 1);

INSERT INTO receta_pasos (id_receta, numero_paso, titulo, descripcion, tiempo_estimado)
SELECT r.id_receta, 2, 'Preparacion de la mezcla', 'Batir los ingredientes secos y humedos por separado. Combinar gradualmente hasta obtener una mezcla homogenea. Verter en los moldes preparados.', 30
FROM recetas r WHERE NOT EXISTS (SELECT 1 FROM receta_pasos p WHERE p.id_receta = r.id_receta AND p.numero_paso = 2);

INSERT INTO receta_pasos (id_receta, numero_paso, titulo, descripcion, tiempo_estimado)
SELECT r.id_receta, 3, 'Horneado y Enfriado', 'Hornear por 30-35 minutos o hasta que al insertar un palillo salga limpio. Dejar enfriar 10 minutos en el molde, luego desmoldar y enfriar completamente sobre una rejilla.', 35
FROM recetas r WHERE NOT EXISTS (SELECT 1 FROM receta_pasos p WHERE p.id_receta = r.id_receta AND p.numero_paso = 3);
GO

PRINT 'Seed de recetas completado exitosamente.';
GO
