package com.example.demo.dao;

import com.example.demo.model.Receta;
import com.example.demo.model.Receta.PasoReceta;
import com.example.demo.model.Receta.RecetaIngrediente;
import com.example.demo.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecetaDAO {

    private static final Logger LOGGER = Logger.getLogger(RecetaDAO.class.getName());
    private final DatabaseConnection dbConnection;

    public RecetaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        asegurarTablas();
    }

    private void asegurarTablas() {
        String sqlRecetas = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='recetas' AND xtype='U') "
            + "CREATE TABLE recetas ("
            + "id_receta INT IDENTITY(1,1) PRIMARY KEY, "
            + "id_producto INT NOT NULL, "
            + "nombre_receta NVARCHAR(200), "
            + "descripcion NVARCHAR(MAX), "
            + "categoria NVARCHAR(100), "
            + "tiempo_preparacion INT DEFAULT 0, "
            + "cantidad_producida DECIMAL(10,2) DEFAULT 1, "
            + "imagen_ref NVARCHAR(500), "
            + "porciones DECIMAL(10,2) DEFAULT 1, "
            + "costo_estimado DECIMAL(12,2) DEFAULT 0, "
            + "rendimiento DECIMAL(5,2) DEFAULT 100, "
            + "desperdicio DECIMAL(5,2) DEFAULT 0, "
            + "estado NVARCHAR(10) DEFAULT 'Activo', "
            + "FOREIGN KEY (id_producto) REFERENCES productos(id_producto))";
        String sqlPasos = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='receta_pasos' AND xtype='U') "
            + "CREATE TABLE receta_pasos ("
            + "id_paso INT IDENTITY(1,1) PRIMARY KEY, "
            + "id_receta INT NOT NULL, "
            + "numero_paso INT NOT NULL, "
            + "titulo NVARCHAR(200), "
            + "descripcion NVARCHAR(MAX), "
            + "tiempo_estimado INT DEFAULT 0, "
            + "imagen_ref NVARCHAR(500), "
            + "FOREIGN KEY (id_receta) REFERENCES recetas(id_receta))";
        String sqlIngredientes = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='receta_ingredientes' AND xtype='U') "
            + "CREATE TABLE receta_ingredientes ("
            + "id_receta INT NOT NULL, id_ingrediente INT NOT NULL, "
            + "cantidad DECIMAL(12,2) DEFAULT 0, "
            + "PRIMARY KEY (id_receta, id_ingrediente), "
            + "FOREIGN KEY (id_receta) REFERENCES recetas(id_receta), "
            + "FOREIGN KEY (id_ingrediente) REFERENCES ingredientes(id_ingrediente))";

        // Add new columns to existing recetas table
        String alterNombre = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'nombre_receta') ALTER TABLE recetas ADD nombre_receta NVARCHAR(200)";
        String alterCategoria = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'categoria') ALTER TABLE recetas ADD categoria NVARCHAR(100)";
        String alterTiempo = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'tiempo_preparacion') ALTER TABLE recetas ADD tiempo_preparacion INT DEFAULT 0";
        String alterCantidad = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'cantidad_producida') ALTER TABLE recetas ADD cantidad_producida DECIMAL(10,2) DEFAULT 1";
        String alterImagen = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'imagen_ref') ALTER TABLE recetas ADD imagen_ref NVARCHAR(500)";
        String alterCosto = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'costo_estimado') ALTER TABLE recetas ADD costo_estimado DECIMAL(12,2) DEFAULT 0";
        String alterRendimiento = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'rendimiento') ALTER TABLE recetas ADD rendimiento DECIMAL(5,2) DEFAULT 100";
        String alterDesperdicio = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('recetas') AND name = 'desperdicio') ALTER TABLE recetas ADD desperdicio DECIMAL(5,2) DEFAULT 0";

        try (Connection conn = dbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlRecetas);
            stmt.execute(sqlPasos);
            stmt.execute(sqlIngredientes);
            // Add new columns if not exist (safe to run multiple times)
            for (String alter : new String[]{alterNombre, alterCategoria, alterTiempo, alterCantidad,
                alterImagen, alterCosto, alterRendimiento, alterDesperdicio}) {
                try { stmt.execute(alter); } catch (SQLException ignored) {}
            }
            // Update nombre_receta where null
            try { stmt.execute("UPDATE recetas SET nombre_receta = nombre_producto FROM (SELECT r.id_receta, p.nombre as nombre_producto FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto) src WHERE recetas.id_receta = src.id_receta AND recetas.nombre_receta IS NULL"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron crear tablas de recetas: {0}", e.getMessage());
        }
    }

  public List<Receta> buscarRecetas(String query) {
  List<Receta> lista = new ArrayList<>();
  String sql = "SELECT r.*, p.nombre as nombre_producto "
             + "FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto "
             + "WHERE r.estado = 'Activo' AND (r.nombre_receta LIKE ? OR p.nombre LIKE ?) "
             + "ORDER BY ISNULL(r.nombre_receta, p.nombre)";
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(sql)) {
  String like = "%" + query + "%";
  stmt.setString(1, like);
  stmt.setString(2, like);
  try (ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  Receta receta = mapearReceta(rs);
  receta.setIngredientes(obtenerIngredientes(conn, rs.getInt("id_receta")));
  receta.setPasos(obtenerPasos(conn, rs.getInt("id_receta")));
  lista.add(receta);
  }
  }
  } catch (SQLException e) {
  LOGGER.log(Level.SEVERE, "Error al buscar recetas: {0}", e.getMessage());
  }
  return lista;
  }

  public List<Receta> listarPorCategoria(String categoria) {
  List<Receta> lista = new ArrayList<>();
  String sql = "SELECT r.*, p.nombre as nombre_producto "
             + "FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto "
             + "WHERE r.estado = 'Activo' AND r.categoria = ? ORDER BY ISNULL(r.nombre_receta, p.nombre)";
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(sql)) {
  stmt.setString(1, categoria);
  try (ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  Receta receta = mapearReceta(rs);
  receta.setIngredientes(obtenerIngredientes(conn, rs.getInt("id_receta")));
  receta.setPasos(obtenerPasos(conn, rs.getInt("id_receta")));
  lista.add(receta);
  }
  }
  } catch (SQLException e) {
  LOGGER.log(Level.SEVERE, "Error al listar recetas por categoria: {0}", e.getMessage());
  }
  return lista;
  }

  public List<String> obtenerCategorias() {
  List<String> categorias = new ArrayList<>();
  String sql = "SELECT DISTINCT categoria FROM recetas WHERE estado = 'Activo' AND categoria IS NOT NULL ORDER BY categoria";
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(sql);
  ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) categorias.add(rs.getString("categoria"));
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "Error al obtener categorias: {0}", e.getMessage());
  }
  return categorias;
  }

  public int duplicarReceta(int idReceta) {
  Receta original = obtenerPorId(idReceta);
  if (original == null) return -1;
  Receta copia = new Receta(0, original.getIdProducto(), original.getNombreProducto(),
  original.getNombreReceta() + " (Copia)", original.getDescripcion(),
  original.getCategoria(), original.getTiempoPreparacion(),
  original.getCantidadProducida(), original.getImagenRef(), original.getPorciones(),
  original.getCostoEstimado(), original.getRendimiento(), original.getDesperdicio(), "Activo");
  copia.setIngredientes(original.getIngredientes());
  copia.setPasos(original.getPasos());
  return insertar(copia, copia.getIngredientes(), copia.getPasos());
  }

  public List<Receta> listarTodas() {
  List<Receta> lista = new ArrayList<>();
  String sql = "SELECT r.*, p.nombre as nombre_producto "
             + "FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto "
             + "WHERE r.estado = 'Activo' ORDER BY ISNULL(r.nombre_receta, p.nombre)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Receta receta = mapearReceta(rs);
                receta.setIngredientes(obtenerIngredientes(conn, rs.getInt("id_receta")));
                receta.setPasos(obtenerPasos(conn, rs.getInt("id_receta")));
                lista.add(receta);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar recetas: {0}", e.getMessage());
        }
        return lista;
    }

    public List<Receta> listarTodasConProducto() {
        return listarTodas();
    }

    public Receta obtenerPorId(int idReceta) {
        String sql = "SELECT r.*, p.nombre as nombre_producto "
                   + "FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto "
                   + "WHERE r.id_receta = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idReceta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Receta receta = mapearReceta(rs);
                    receta.setIngredientes(obtenerIngredientes(conn, idReceta));
                    receta.setPasos(obtenerPasos(conn, idReceta));
                    return receta;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener receta: {0}", e.getMessage());
        }
        return null;
    }

    public Receta obtenerPorNombreProducto(String nombreProducto) {
        String sql = "SELECT TOP 1 r.*, p.nombre as nombre_producto "
                   + "FROM recetas r INNER JOIN productos p ON r.id_producto = p.id_producto "
                   + "WHERE p.nombre LIKE ? AND r.estado = 'Activo' ORDER BY r.id_receta";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + nombreProducto + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_receta");
                    Receta receta = mapearReceta(rs);
                    receta.setIngredientes(obtenerIngredientes(conn, id));
                    receta.setPasos(obtenerPasos(conn, id));
                    return receta;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener receta por producto: {0}", e.getMessage());
        }
        return null;
    }

    private Receta mapearReceta(ResultSet rs) throws SQLException {
        return new Receta(
            rs.getInt("id_receta"), rs.getInt("id_producto"),
            rs.getString("nombre_producto"),
            rs.getString("nombre_receta") != null ? rs.getString("nombre_receta") : rs.getString("nombre_producto"),
            rs.getString("descripcion"),
            rs.getString("categoria"),
            rs.getInt("tiempo_preparacion"),
            rs.getDouble("cantidad_producida"),
            rs.getString("imagen_ref"),
            rs.getDouble("porciones"),
            rs.getDouble("costo_estimado"),
            rs.getDouble("rendimiento"),
            rs.getDouble("desperdicio"),
            rs.getString("estado")
        );
    }

    private List<RecetaIngrediente> obtenerIngredientes(Connection conn, int idReceta) throws SQLException {
        List<RecetaIngrediente> lista = new ArrayList<>();
        String sql = "SELECT ri.id_ingrediente, i.nombre as nombre_ingrediente, ri.cantidad, i.unidad "
                   + "FROM receta_ingredientes ri INNER JOIN ingredientes i ON ri.id_ingrediente = i.id_ingrediente "
                   + "WHERE ri.id_receta = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idReceta);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RecetaIngrediente(rs.getInt("id_ingrediente"),
                        rs.getString("nombre_ingrediente"), rs.getDouble("cantidad"),
                        rs.getString("unidad")));
                }
            }
        }
        return lista;
    }

    private List<PasoReceta> obtenerPasos(Connection conn, int idReceta) throws SQLException {
        List<PasoReceta> lista = new ArrayList<>();
        String sql = "SELECT * FROM receta_pasos WHERE id_receta = ? ORDER BY numero_paso";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idReceta);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PasoReceta paso = new PasoReceta(
                        rs.getInt("numero_paso"), rs.getString("titulo"),
                        rs.getString("descripcion"), rs.getInt("tiempo_estimado"),
                        rs.getString("imagen_ref"));
                    paso.setIdPaso(rs.getInt("id_paso"));
                    lista.add(paso);
                }
            }
        }
        return lista;
    }

    public int insertar(Receta receta, List<RecetaIngrediente> ingredientes, List<PasoReceta> pasos) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO recetas (id_producto, nombre_receta, descripcion, categoria, "
                + "tiempo_preparacion, cantidad_producida, imagen_ref, porciones, costo_estimado, "
                + "rendimiento, desperdicio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, receta.getIdProducto());
                stmt.setString(2, receta.getNombreReceta());
                stmt.setString(3, receta.getDescripcion());
                stmt.setString(4, receta.getCategoria());
                stmt.setInt(5, receta.getTiempoPreparacion());
                stmt.setDouble(6, receta.getCantidadProducida());
                stmt.setString(7, receta.getImagenRef());
                stmt.setDouble(8, receta.getPorciones());
                stmt.setDouble(9, receta.getCostoEstimado());
                stmt.setDouble(10, receta.getRendimiento());
                stmt.setDouble(11, receta.getDesperdicio());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        insertarIngredientes(conn, id, ingredientes);
                        insertarPasos(conn, id, pasos);
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar receta: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
        return -1;
    }

    public boolean actualizar(Receta receta, List<RecetaIngrediente> ingredientes, List<PasoReceta> pasos) {
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            String sql = "UPDATE recetas SET id_producto = ?, nombre_receta = ?, descripcion = ?, categoria = ?, "
                + "tiempo_preparacion = ?, cantidad_producida = ?, imagen_ref = ?, porciones = ?, "
                + "costo_estimado = ?, rendimiento = ?, desperdicio = ? WHERE id_receta = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, receta.getIdProducto());
                stmt.setString(2, receta.getNombreReceta());
                stmt.setString(3, receta.getDescripcion());
                stmt.setString(4, receta.getCategoria());
                stmt.setInt(5, receta.getTiempoPreparacion());
                stmt.setDouble(6, receta.getCantidadProducida());
                stmt.setString(7, receta.getImagenRef());
                stmt.setDouble(8, receta.getPorciones());
                stmt.setDouble(9, receta.getCostoEstimado());
                stmt.setDouble(10, receta.getRendimiento());
                stmt.setDouble(11, receta.getDesperdicio());
                stmt.setInt(12, receta.getId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM receta_ingredientes WHERE id_receta = ?")) {
                stmt.setInt(1, receta.getId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM receta_pasos WHERE id_receta = ?")) {
                stmt.setInt(1, receta.getId());
                stmt.executeUpdate();
            }
            insertarIngredientes(conn, receta.getId(), ingredientes);
            insertarPasos(conn, receta.getId(), pasos);
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar receta: {0}", e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
        return false;
    }

    public boolean eliminar(int id) {
        String sql = "UPDATE recetas SET estado = 'Inactivo' WHERE id_receta = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar receta: {0}", e.getMessage());
            return false;
        }
    }

    public List<RecetaIngrediente> obtenerIngredientesDisponibles() {
        List<RecetaIngrediente> lista = new ArrayList<>();
        String sql = "SELECT id_ingrediente, nombre, unidad FROM ingredientes WHERE estado = 'Activo' ORDER BY nombre";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new RecetaIngrediente(rs.getInt("id_ingrediente"),
                    rs.getString("nombre"), 0, rs.getString("unidad")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ingredientes disponibles: {0}", e.getMessage());
        }
        return lista;
    }

    private void insertarIngredientes(Connection conn, int idReceta, List<RecetaIngrediente> ingredientes) throws SQLException {
        String sql = "INSERT INTO receta_ingredientes (id_receta, id_ingrediente, cantidad) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (RecetaIngrediente ing : ingredientes) {
                stmt.setInt(1, idReceta);
                stmt.setInt(2, ing.getIdIngrediente());
                stmt.setDouble(3, ing.getCantidad());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertarPasos(Connection conn, int idReceta, List<PasoReceta> pasos) throws SQLException {
        String sql = "INSERT INTO receta_pasos (id_receta, numero_paso, titulo, descripcion, tiempo_estimado, imagen_ref) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PasoReceta paso : pasos) {
                stmt.setInt(1, idReceta);
                stmt.setInt(2, paso.getNumeroPaso());
                stmt.setString(3, paso.getTitulo());
                stmt.setString(4, paso.getDescripcion());
                stmt.setInt(5, paso.getTiempoEstimado());
                stmt.setString(6, paso.getImagenRef());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
