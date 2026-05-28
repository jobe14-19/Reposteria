package com.example.demo.controller;
import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.Receta;
import com.example.demo.model.Receta.RecetaIngrediente;
import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

public class PlanificacionController {

 private static final Logger LOGGER = Logger.getLogger(PlanificacionController.class.getName());

 public void setModoEmbedded(boolean b) {
 this.modoEmbedded = b;
 if (cerrarSesionButton != null) {
 cerrarSesionButton.setVisible(!b);
 cerrarSesionButton.setManaged(!b);
 }
 if (usuarioLabel != null) {
 usuarioLabel.setVisible(!b);
 usuarioLabel.setManaged(!b);
 }
 }

 @FXML private Button pestana1Button;
 @FXML private Button pestana2Button;
 @FXML private Button pestana3Button;
 @FXML private Label usuarioLabel;
 @FXML private Button cerrarSesionButton;
 @FXML private VBox planificacionView;
 @FXML private VBox seguimientoView;
   @FXML private VBox recetasView;
   @FXML private GridPane semanaGridPane;
   private boolean modoEmbedded;
 @FXML private Label totalOrdenesLabel;
 @FXML private Label enProduccionLabel;
 @FXML private Label listosEntregarLabel;
 @FXML private ListView<String> alertasListView;
 @FXML private TextField buscarOrdenField;
 @FXML private Button buscarOrdenButton;
 @FXML private Button verDetallesButton;
 @FXML private Label ordenIdLabel;
 @FXML private Label clienteLabel;
 @FXML private Label estadoActualLabel;
 @FXML private ListView<String> timelineListView;
 @FXML private GridPane pasosGridPane;
 @FXML private Button actualizarButton;
 @FXML private Button marcarListoButton;
 @FXML private Button nuevaRecetaButton;
 @FXML private Button editarRecetaButton;
 @FXML private Button eliminarRecetaButton;
 @FXML private TableView<Receta> recetasTable;
 @FXML private TableColumn<Receta, String> recetaProductoColumn;
 @FXML private TableColumn<Receta, String> recetaDescripcionColumn;
 @FXML private TableColumn<Receta, Double> recetaPorcionesColumn;
 @FXML private TableColumn<Receta, String> recetaIngredientesColumn;

 private SessionManager sessionManager;
 private DatabaseConnection dbConnection;
 private RecetaDAO recetaDAO;
 private OrdenProduccionDAO ordenDAO;
 private Orden ordenSeleccionada;
 private ObservableList<Orden> ordenesList;
 private ObservableList<Receta> recetasList;
 private Timer autoRefreshTimer;

 @FXML
 public void initialize() {
 sessionManager = SessionManager.getInstance();
 dbConnection = DatabaseConnection.getInstance();
  recetaDAO = new RecetaDAO();
  ordenDAO = new OrdenProduccionDAO();

 if (!sessionManager.tienePermiso(Permiso.PRODUCCION_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso para acceder a la planificaci\u00f3n de producci\u00f3n.");
 return;
 }

 planificacionView.setVisible(true);
 seguimientoView.setVisible(false);
 recetasView.setVisible(false);

  configurarTablaRecetas();
  cargarDatosPlanificacion();
  setupEventHandlers();
  actualizarInfoUsuario();
  iniciarAutoRefresh();
  }

  private void iniciarAutoRefresh() {
  autoRefreshTimer = new Timer(true);
  autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
  @Override public void run() {
  Platform.runLater(() -> {
  if (planificacionView != null && planificacionView.isVisible()) {
  cargarDatosPlanificacion();
  }
  });
  }
  }, 30000, 30000);
  }

 private void configurarTablaRecetas() {
 recetaProductoColumn.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
 recetaDescripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
 recetaPorcionesColumn.setCellValueFactory(new PropertyValueFactory<>("porciones"));
 recetaIngredientesColumn.setCellValueFactory(new PropertyValueFactory<>("ingredientesStr"));
 }

 private void setupEventHandlers() {
 pestana1Button.setOnAction(event -> mostrarPlanificacion());
 pestana2Button.setOnAction(event -> mostrarSeguimiento());
 pestana3Button.setOnAction(event -> mostrarRecetas());
 cerrarSesionButton.setOnAction(event -> cerrarSesion());

 buscarOrdenField.textProperty().addListener((obs, oldVal, newVal) -> {
 if (newVal == null || newVal.isEmpty()) {
 limpiarSeguimiento();
 } else {
 buscarOrden(newVal);
 }
 });

 buscarOrdenButton.setOnAction(event -> buscarOrden(buscarOrdenField.getText()));
 verDetallesButton.setOnAction(event -> verDetallesOrden());
 actualizarButton.setOnAction(event -> actualizarEstadoOrden());
 marcarListoButton.setOnAction(event -> marcarComoListo());

   nuevaRecetaButton.setOnAction(event -> abrirModalReceta(null));
   editarRecetaButton.setOnAction(event -> {
   Receta seleccionada = recetasTable.getSelectionModel().getSelectedItem();
   if (seleccionada != null) {
   abrirModalReceta(seleccionada);
   } else {
   mostrarMensaje("Sin Seleccion", "Seleccione una receta para editar.");
   }
   });
   eliminarRecetaButton.setOnAction(event -> {
   Receta seleccionada = recetasTable.getSelectionModel().getSelectedItem();
   if (seleccionada != null) {
   eliminarReceta(seleccionada);
   } else {
   mostrarMensaje("Sin Seleccion", "Seleccione una receta para eliminar.");
   }
   });
   recetasTable.setOnMouseClicked(event -> {
   if (event.getClickCount() == 2) {
   Receta seleccionada = recetasTable.getSelectionModel().getSelectedItem();
   if (seleccionada != null) {
   abrirRecetaViewer(seleccionada);
   }
   }
   });
 }

 private void mostrarPlanificacion() {
 planificacionView.setVisible(true);
 planificacionView.setManaged(true);
 seguimientoView.setVisible(false);
 seguimientoView.setManaged(false);
 recetasView.setVisible(false);
 recetasView.setManaged(false);
 pestana1Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");
 pestana2Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 pestana3Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 cargarDatosPlanificacion();
 }

 private void mostrarSeguimiento() {
 planificacionView.setVisible(false);
 planificacionView.setManaged(false);
 seguimientoView.setVisible(true);
 seguimientoView.setManaged(true);
 recetasView.setVisible(false);
 recetasView.setManaged(false);
 pestana1Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 pestana2Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");
 pestana3Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 cargarOrdenesActivas();
 }

 private void mostrarRecetas() {
 planificacionView.setVisible(false);
 planificacionView.setManaged(false);
 seguimientoView.setVisible(false);
 seguimientoView.setManaged(false);
 recetasView.setVisible(true);
 recetasView.setManaged(true);
 pestana1Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 pestana2Button.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white;");
 pestana3Button.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white;");
 cargarRecetas();
 }

 private void cargarRecetas() {
 recetasList = FXCollections.observableArrayList(recetaDAO.listarTodas());
 recetasTable.setItems(recetasList);
 }

   private void abrirModalReceta(Receta receta) {
   try {
   FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaWizard.fxml"));
   Parent root = loader.load();
   RecetaWizardController wizardController = loader.getController();
   wizardController.setRecetaDAO(recetaDAO);
   if (receta != null) {
   wizardController.setReceta(receta);
   }
   Stage stage = new Stage();
   stage.setScene(new Scene(root, 800, 700));
   stage.setTitle(receta == null ? "Nueva Receta" : "Editar Receta");
   stage.initModality(Modality.APPLICATION_MODAL);
   stage.setOnHidden(e -> cargarRecetas());
   stage.showAndWait();
   } catch (Exception e) {
   LOGGER.log(Level.SEVERE, "Error al abrir wizard receta: {0}", e.getMessage());
   mostrarError("Error", "No se pudo abrir el wizard de recetas.");
   }
   }

   private void eliminarReceta(Receta receta) {
   Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
   confirm.setTitle("Confirmar Eliminacion");
   confirm.setHeaderText(null);
   confirm.setContentText("Esta seguro de eliminar la receta de " + receta.getNombreProducto() + "?");
   if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
   if (recetaDAO.eliminar(receta.getId())) {
   mostrarMensaje("Eliminado", "Receta eliminada correctamente.");
   cargarRecetas();
   } else {
   mostrarError("Error", "No se pudo eliminar la receta.");
   }
   }
   }

   private void abrirRecetaViewer(Receta receta) {
   try {
   Receta completa = recetaDAO.obtenerPorId(receta.getId());
   if (completa == null) { mostrarError("Error", "No se pudo cargar la receta."); return; }
   FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaViewer.fxml"));
   Parent root = loader.load();
   RecetaViewerController viewer = loader.getController();
   viewer.setReceta(completa);
   viewer.setModoCapacitacion(false);
   viewer.initialize();
   Stage stage = new Stage();
   stage.setScene(new Scene(root, 750, 650));
   stage.setTitle("Receta: " + completa.getNombreReceta());
   stage.initModality(Modality.APPLICATION_MODAL);
   stage.showAndWait();
   } catch (Exception e) {
   LOGGER.log(Level.SEVERE, "Error al abrir visor receta: {0}", e.getMessage());
   mostrarError("Error", "No se pudo abrir la receta.");
   }
   }

  private void cargarDatosPlanificacion() {
   try (Connection conn = dbConnection.getConnection()) {
   String sql = 
   "SELECT id, cliente, producto, libras, dia_semana, fecha_entrega, estado, tipo, " +
   "total, anticipo, tipo_pago, estado_pago FROM (" +
   "SELECT op.id_orden as id, op.cliente, op.categoria as producto, op.libras, " +
   "DATEPART(WEEKDAY, op.fecha_entrega) as dia_semana, " +
   "op.fecha_entrega, op.estado, 'PRODUCCION' as tipo, " +
   "ISNULL(op.precio_venta, 0) as total, ISNULL(op.anticipo, 0) as anticipo, " +
   "ISNULL(op.tipo_pago, 'Efectivo') as tipo_pago, ISNULL(op.estado_pago, 'Pendiente') as estado_pago " +
    "FROM ordenes_produccion op " +
    "WHERE op.estado IN ('ACTIVA', 'EN PRODUCCION') " +
    "AND op.fecha_entrega >= DATEADD(DAY, -DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
   "AND op.fecha_entrega <= DATEADD(DAY, 6 - DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
   "UNION ALL " +
    "SELECT p.id_pedido as id, ISNULL(c.nombre + ' ' + c.apellido, p.username) as cliente, COALESCE(pr.nombre, p.producto) as producto, " +
    "ISNULL(p.libras, 0) as libras, " +
    "DATEPART(WEEKDAY, p.fecha_entrega) as dia_semana, " +
    "p.fecha_entrega, p.estado, 'PEDIDO' as tipo, " +
    "ISNULL(p.total, 0) as total, ISNULL(p.adelanto, 0) as anticipo, " +
    "ISNULL(p.tipo_pago, 'Efectivo') as tipo_pago, ISNULL(p.estado_pago, 'Pendiente') as estado_pago " +
    "FROM pedidos p " +
    "LEFT JOIN clientes c ON p.id_cliente = c.id_cliente " +
    "LEFT JOIN productos pr ON p.id_producto = pr.id_producto " +
   "LEFT JOIN ordenes_produccion op ON p.id_pedido = op.id_pedido " +
   "WHERE op.id_orden IS NULL " +
   "AND p.estado NOT IN ('Entregado', 'Cancelado') " +
   "AND p.fecha_entrega >= DATEADD(DAY, -DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
   "AND p.fecha_entrega <= DATEADD(DAY, 6 - DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
   ") combined ORDER BY fecha_entrega";

  ordenesList = FXCollections.observableArrayList();

  try (PreparedStatement stmt = conn.prepareStatement(sql);
  ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  Orden orden = new Orden(
  rs.getInt("id"),
  rs.getString("cliente"),
  rs.getString("producto"),
  rs.getDouble("libras"),
  rs.getInt("dia_semana"),
  rs.getString("fecha_entrega"),
  rs.getString("estado"),
  rs.getString("tipo"),
  rs.getDouble("total"),
  rs.getDouble("anticipo"),
  rs.getString("tipo_pago"),
  rs.getString("estado_pago")
  );
  ordenesList.add(orden);
  }
  }

  construirSemanalGrid();
  cargarEstadisticas();
  cargarAlertas();

  } catch (SQLException e) {
  LOGGER.log(Level.INFO, "Modo offline: usando datos de demostracion en planificacion");
  ordenesList = FXCollections.observableArrayList();
  construirSemanalGrid();
  totalOrdenesLabel.setText("--");
  enProduccionLabel.setText("--");
  listosEntregarLabel.setText("--");
  alertasListView.setItems(FXCollections.observableArrayList());
  }
  }

 private void construirSemanalGrid() {
 semanaGridPane.getChildren().clear();

 String[] dias = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
 Label diaLabel = new Label("");
 semanaGridPane.add(diaLabel, 0, 0);

 for (int i = 0; i < dias.length; i++) {
 VBox cell = new VBox(5);
   cell.setStyle("-fx-padding: 10; -fx-border-width: 1;");
   cell.getStyleClass().addAll("border-light", "bg-input");

  int diaActual = LocalDate.now().getDayOfWeek().getValue();
  if (i + 1 == diaActual) cell.setStyle(cell.getStyle() + "; -fx-background-color: #fdf0f3;");

  for (Orden orden : ordenesList) {
  int diaSemana = orden.getDiaSemana();
  if (diaSemana == i + 2) {
  boolean esPedido = "PEDIDO".equals(orden.getTipo());
  String estadoColor;
  if (esPedido) {
  estadoColor = switch (orden.getEstado().toLowerCase()) {
  case "pendiente" -> "#F39C12";
  case "confirmado" -> "#007BFF";
  case "en producci\u00f3n" -> "#FF9800";
  case "listo" -> "#17A2B8";
  case "entregado" -> "#28A745";
  default -> "#6C757D";
  };
  } else {
  estadoColor = switch (orden.getEstado()) {
  case "ACTIVA" -> "#007BFF";
  case "EN PRODUCCION" -> "#FF9800";
  case "COMPLETADA" -> "#28A745";
  default -> "#6C757D";
  };
  }
  Label iconoLbl = new Label(esPedido ? "\uD83D\uDCE6 " : "\u2699\uFE0F ");
  iconoLbl.setStyle("-fx-font-size: 10px;");
  Label clienteLbl = new Label(orden.getNombreCliente());
  clienteLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
  Label productoLbl = new Label(orden.getProducto());
  productoLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
   Label librasLbl = new Label(orden.getLibras() + " lbs");
   librasLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
   Label estadoBadge = new Label(esPedido ? orden.getEstado() : orden.getEstado());
   estadoBadge.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: " + estadoColor + "; -fx-background-radius: 999px; -fx-padding: 1 8;");
   String pagoBg;
    switch (orden.getEstadoPago()) {
    case "PAGADO": pagoBg = "#28A745"; break;
    case "PAGADO_PARCIAL": pagoBg = "#FF9800"; break;
   default: pagoBg = "#DC3545";
   }
   Label pagoBadge = new Label(orden.getEstadoPago());
   pagoBadge.setStyle("-fx-font-size: 8px; -fx-text-fill: white; -fx-background-color: " + pagoBg + "; -fx-background-radius: 999px; -fx-padding: 1 6;");
   Label saldoLbl = new Label();
   if (orden.getTotal() > 0) {
   double saldo = orden.getSaldo();
   saldoLbl.setText("RD$" + String.format("%.2f", saldo));
   saldoLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + (saldo > 0 ? "#DC3545" : "#28A745") + ";");
   } else {
   saldoLbl.setText("RD$0.00");
   saldoLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");
   }
   HBox footer = new HBox(5, pagoBadge, saldoLbl);
   HBox header = new HBox(3, iconoLbl, clienteLbl);
   VBox card = new VBox(2, header, productoLbl, librasLbl, estadoBadge, footer);
  String bgColor;
  if (esPedido) {
  bgColor = switch (orden.getEstado().toLowerCase()) {
  case "pendiente" -> "#fef9e7";
  case "confirmado" -> "#eaf2f8";
  case "en producci\u00f3n" -> "#fdebd0";
  case "listo" -> "#d5f5e3";
  default -> "#f5f5f5";
  };
  } else {
  bgColor = orden.getEstado().equals("ACTIVA") ? "#e8f4fd" : orden.getEstado().equals("EN PRODUCCION") ? "#fff3e0" : orden.getEstado().equals("COMPLETADA") ? "#e8f5e9" : "#f5f5f5";
  }
    card.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-left: 3px solid " + (esPedido ? "#9B59B6" : estadoColor) + ";");
   card.setCursor(javafx.scene.Cursor.HAND);
   Orden ordenFinal = orden;
   card.setOnMouseClicked(event -> abrirDetalleOrden(ordenFinal));
   cell.getChildren().add(card);
  }
  }

 semanaGridPane.add(cell, i + 1, 1);
 }
 }

  private void cargarEstadisticas() {
  try (Connection conn = dbConnection.getConnection()) {
  String sql = "SELECT " +
  "(SELECT COUNT(*) FROM ordenes_produccion WHERE estado IN ('ACTIVA','EN PRODUCCION')) as total_ordenes, " +
  "(SELECT COUNT(*) FROM ordenes_produccion WHERE estado = 'EN PRODUCCION') as en_produccion, " +
  "(SELECT COUNT(*) FROM pedidos WHERE estado NOT IN ('Entregado','Cancelado')) as pedidos_pendientes";

  try (PreparedStatement stmt = conn.prepareStatement(sql);
  ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) {
  int ops = rs.getInt("total_ordenes");
  int prod = rs.getInt("en_produccion");
  int peds = rs.getInt("pedidos_pendientes");
  totalOrdenesLabel.setText(ops + " ops / " + peds + " peds");
  enProduccionLabel.setText(String.valueOf(prod));
  listosEntregarLabel.setText(String.valueOf(peds));
  }
  }

  } catch (SQLException e) {
  LOGGER.log(Level.INFO, "Modo offline: estadisticas no disponibles");
  }
  }

  private void cargarAlertas() {
  try (Connection conn = dbConnection.getConnection()) {
  String sql = "SELECT TOP 10 alerta FROM ( " +
  "SELECT 'Orden #' + CAST(op.id_orden AS VARCHAR) + ' - ' + op.cliente + ' - ' + ISNULL(op.categoria, 'General') + ' - ' + op.estado as alerta " +
  "FROM ordenes_produccion op " +
  "WHERE op.estado IN ('EN PRODUCCION', 'COMPLETADA') " +
  "UNION ALL " +
  "SELECT 'Pedido #' + CAST(p.id_pedido AS VARCHAR) + ' - ' + c.nombre + ' ' + c.apellido + ' - ' + p.estado " +
  "FROM pedidos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
  "WHERE p.estado NOT IN ('Entregado', 'Cancelado') " +
  ") alerts ORDER BY alerta";

  ObservableList<String> alertas = FXCollections.observableArrayList();

  try (PreparedStatement stmt = conn.prepareStatement(sql);
  ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  alertas.add(rs.getString("alerta"));
  }
  }

  alertasListView.setItems(alertas);

  } catch (SQLException e) {
  LOGGER.log(Level.INFO, "Modo offline: alertas no disponibles");
  alertasListView.setItems(FXCollections.observableArrayList());
  }
  }

 private void cargarOrdenesActivas() {
 try (Connection conn = dbConnection.getConnection()) {
   String sql = "SELECT op.id_orden as id, op.cliente as nombre_cliente, " +
   "op.estado, op.fecha_entrega, ISNULL(op.categoria, 'General') as producto, op.libras " +
   "FROM ordenes_produccion op " +
   "WHERE op.estado IN ('ACTIVA', 'EN PRODUCCION') " +
   "ORDER BY op.fecha_entrega";

  ordenesList = FXCollections.observableArrayList();

  try (PreparedStatement stmt = conn.prepareStatement(sql);
  ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  Orden orden = new Orden(
  rs.getInt("id"),
  rs.getString("nombre_cliente"),
  rs.getString("producto"),
  rs.getDouble("libras"),
  0,
  rs.getString("fecha_entrega"),
  rs.getString("estado"),
  "PRODUCCION"
  );
  ordenesList.add(orden);
  }
  }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: ordenes activas no disponibles");
 ordenesList = FXCollections.observableArrayList();
 }
 }

 private void buscarOrden(String textoBusqueda) {
 try (Connection conn = dbConnection.getConnection()) {
  String sql = "SELECT TOP 1 op.id_orden as id, op.cliente as nombre_cliente, " +
  "op.estado, op.fecha_entrega, ISNULL(op.categoria, 'General') as producto, op.libras " +
  "FROM ordenes_produccion op " +
  "WHERE (CAST(op.id_orden AS VARCHAR) LIKE ? OR " +
  "op.cliente LIKE ?) " +
   "AND op.estado IN ('ACTIVA', 'EN PRODUCCION')";

  try (PreparedStatement stmt = conn.prepareStatement(sql)) {
  String busqueda = "%" + textoBusqueda + "%";
  stmt.setString(1, busqueda);
  stmt.setString(2, busqueda);

  try (ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) {
  ordenSeleccionada = new Orden(
  rs.getInt("id"),
  rs.getString("nombre_cliente"),
  rs.getString("producto"),
  rs.getDouble("libras"),
  0,
  rs.getString("fecha_entrega"),
  rs.getString("estado"),
  "PRODUCCION"
  );
 mostrarDetallesOrden(ordenSeleccionada);
 } else {
 limpiarSeguimiento();
 mostrarMensaje("No Encontrado", "No se encontr\u00f3 ninguna orden con ese criterio.");
 }
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: busqueda de ordenes no disponible");
 mostrarMensaje("Busqueda no disponible", "La busqueda de ordenes no est\u00e1 disponible en modo offline.");
 }
 }

 private void verDetallesOrden() {
 if (ordenSeleccionada != null) {
 mostrarDetallesOrden(ordenSeleccionada);
 } else {
 mostrarMensaje("Sin Seleccion", "Por favor seleccione una orden para ver sus detalles.");
 }
 }

 private void mostrarDetallesOrden(Orden orden) {
 ordenIdLabel.setText(String.valueOf(orden.getId()));
 clienteLabel.setText(orden.getNombreCliente());
 estadoActualLabel.setText(orden.getEstado());
 construirTimeline(orden);
 }

 private void construirTimeline(Orden orden) {
 timelineListView.getItems().clear();
 String[] pasos = {
 "1. Preparacion de masas",
 "2. Horneado",
 "3. Enfriado controlado",
 "4. Preparacion de rellenos",
 "5. Decoracion",
 "6. Empaque y control de calidad"
 };
 for (String paso : pasos) {
 timelineListView.getItems().add(paso);
 }
 }

 @FXML
 private void actualizarEstadoOrden() {
 if (ordenSeleccionada != null) {
 mostrarMensaje("Actualizar Estado", "Funcion de actualizacion de estado en desarrollo.");
 } else {
 mostrarMensaje("Sin Seleccion", "Por favor seleccione una orden para actualizar su estado.");
 }
 }

 @FXML
 private void marcarComoListo() {
 if (!sessionManager.tienePermiso(Permiso.PRODUCCION_ACTUALIZAR)) {
 mostrarError("Acceso Denegado", "No tienes permiso para actualizar el estado de producci\u00f3n.");
 return;
 }
 if (ordenSeleccionada != null) {
   String usuario = sessionManager.getUsuarioActual();
   if (ordenDAO.cambiarEstado(ordenSeleccionada.getId(), "COMPLETADA", usuario)) {
     mostrarMensaje("Orden Actualizada", "La orden ha sido marcada como completada.");
     cargarOrdenesActivas();
   } else {
     mostrarError("Error al Actualizar", "No se pudo actualizar el estado de la orden.");
   }
 } else {
 mostrarMensaje("Sin Seleccion", "Por favor seleccione una orden para marcar como completada.");
 }
 }

  private void abrirDetalleOrden(Orden orden) {
  try {
  if ("PRODUCCION".equals(orden.getTipo())) {
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/OrdenProduccionDetalle.fxml"));
  Parent root = loader.load();
  OrdenProduccionDetalleController controller = loader.getController();
  controller.setOrdenDAO(ordenDAO);
  controller.setOrdenId(orden.getId());
  Stage stage = new Stage();
  stage.setScene(new Scene(root, 1000, 750));
  stage.setTitle("Orden #" + orden.getId() + " - " + orden.getNombreCliente());
  stage.initModality(Modality.APPLICATION_MODAL);
  stage.setOnHidden(e -> cargarDatosPlanificacion());
  stage.showAndWait();
  } else {
  abrirDetallePedido(orden);
  }
  } catch (Exception e) {
  LOGGER.log(Level.SEVERE, "Error al abrir detalle", e);
  mostrarError("Error", "No se pudo abrir el detalle de la orden.");
  }
  }

  private void abrirDetallePedido(Orden orden) {
  Dialog<Void> dialog = new Dialog<>();
  dialog.setTitle("Pedido #" + orden.getId());
  dialog.setHeaderText("Detalle del Pedido");

  VBox content = new VBox(8);
  content.setPadding(new javafx.geometry.Insets(15));

  double total = orden.getTotal();
  double saldo = orden.getSaldo();
  String saldoText;
  if (total == 0) {
  saldoText = "Sin precio (pendiente del cajero)";
  } else if (saldo > 0) {
  saldoText = String.format("Pendiente $%.2f", saldo);
  } else {
  saldoText = "Pagado";
  }
  Label saldoLbl = new Label("Saldo: " + saldoText);

  Button verRecetaBtn = new Button("Ver Receta");
  verRecetaBtn.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 4; -fx-cursor: hand;");
  verRecetaBtn.setOnAction(e -> {
  if (orden.getProducto() != null && !orden.getProducto().isEmpty()) {
  Receta receta = recetaDAO.obtenerPorNombreProducto(orden.getProducto());
  if (receta != null) {
  abrirRecetaViewer(receta);
  } else {
  mostrarMensaje("Sin Receta", "No hay receta registrada para: " + orden.getProducto());
  }
  }
  });

  Label tipoPagoLbl = new Label("Tipo Pago: " + orden.getTipoPago());
  Label estadoPagoLbl = new Label("Estado Pago: " + orden.getEstadoPago());

  content.getChildren().addAll(
  new Label("Cliente: " + orden.getNombreCliente()),
  new Label("Producto: " + orden.getProducto()),
  new Label("Libras: " + orden.getLibras()),
  new Label("Fecha entrega: " + orden.getFechaEntrega()),
  new Label("Estado: " + orden.getEstado()),
  tipoPagoLbl,
  estadoPagoLbl,
  saldoLbl,
  verRecetaBtn
  );

  dialog.getDialogPane().setContent(content);
  dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
  dialog.getDialogPane().setPrefSize(400, 380);
  dialog.showAndWait();
  }

  private void limpiarSeguimiento() {
 ordenSeleccionada = null;
 ordenIdLabel.setText("-");
 clienteLabel.setText("-");
 estadoActualLabel.setText("-");
 timelineListView.getItems().clear();
 }

 private void actualizarInfoUsuario() {
 if (sessionManager != null && sessionManager.isLoggedIn()) {
 String nombre = sessionManager.getUsuarioActual();
 String perfil = sessionManager.getPerfilActual();
 usuarioLabel.setText(" " + nombre + " (" + perfil + ")");
 } else {
 usuarioLabel.setText(" Usuario");
 }
 }

   private void cerrarSesion() {
   if (sessionManager != null) {
   sessionManager.cerrarSesion();
   }
   if (modoEmbedded) return;
   Stage stage = (Stage) cerrarSesionButton.getScene().getWindow();
   stage.close();
   }

 private void mostrarError(String titulo, String mensaje) {
 Alert alert = new Alert(Alert.AlertType.ERROR);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }

 private void mostrarMensaje(String titulo, String mensaje) {
 Alert alert = new Alert(Alert.AlertType.INFORMATION);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }

  public static class Orden {
  private int id;
  private String nombreCliente;
  private String producto;
  private double libras;
  private int diaSemana;
  private String fechaEntrega;
  private String estado;
  private String tipo;
  private double total;
  private double anticipo;
  private String tipoPago;
  private String estadoPago;

  public Orden(int id, String nombreCliente, String producto, double libras, int diaSemana, String fechaEntrega, String estado, String tipo) {
  this(id, nombreCliente, producto, libras, diaSemana, fechaEntrega, estado, tipo, 0, 0, "Efectivo", "Pendiente");
  }

  public Orden(int id, String nombreCliente, String producto, double libras, int diaSemana, String fechaEntrega, String estado, String tipo, double total, double anticipo, String tipoPago, String estadoPago) {
  this.id = id;
  this.nombreCliente = nombreCliente;
  this.producto = producto;
  this.libras = libras;
  this.diaSemana = diaSemana;
  this.fechaEntrega = fechaEntrega;
  this.estado = estado;
  this.tipo = tipo;
  this.total = total;
  this.anticipo = anticipo;
  this.tipoPago = tipoPago;
  this.estadoPago = estadoPago;
  }

  public int getId() { return id; }
  public String getNombreCliente() { return nombreCliente; }
  public String getProducto() { return producto; }
  public double getLibras() { return libras; }
  public int getDiaSemana() { return diaSemana; }
  public String getFechaEntrega() { return fechaEntrega; }
  public String getEstado() { return estado; }
  public String getTipo() { return tipo; }
  public double getTotal() { return total; }
  public double getAnticipo() { return anticipo; }
  public double getSaldo() { return total - anticipo; }
  public String getTipoPago() { return tipoPago; }
  public String getEstadoPago() { return estadoPago; }
  }
}
