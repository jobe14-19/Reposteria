package com.example.demo.controller;
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
import java.util.logging.Level;
import java.util.logging.Logger;

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
 @FXML private Label totalPedidosLabel;
 @FXML private Label enProduccionLabel;
 @FXML private Label listosEntregarLabel;
 @FXML private ListView<String> alertasListView;
 @FXML private TextField buscarPedidoField;
 @FXML private Button buscarPedidoButton;
 @FXML private Button verDetallesButton;
 @FXML private Label pedidoIdLabel;
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
 private Pedido pedidoSeleccionado;
 private ObservableList<Pedido> pedidosList;
 private ObservableList<Receta> recetasList;

 @FXML
 public void initialize() {
 sessionManager = SessionManager.getInstance();
 dbConnection = DatabaseConnection.getInstance();
 recetaDAO = new RecetaDAO();

 if (!sessionManager.tienePermiso(Permiso.PRODUCCION_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso para acceder a la planificación de producción.");
 return;
 }

 planificacionView.setVisible(true);
 seguimientoView.setVisible(false);
 recetasView.setVisible(false);

 configurarTablaRecetas();
 cargarDatosPlanificacion();
 setupEventHandlers();
 actualizarInfoUsuario();
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

 buscarPedidoField.textProperty().addListener((obs, oldVal, newVal) -> {
 if (newVal == null || newVal.isEmpty()) {
 limpiarSeguimiento();
 } else {
 buscarPedido(newVal);
 }
 });

 buscarPedidoButton.setOnAction(event -> buscarPedido(buscarPedidoField.getText()));
 verDetallesButton.setOnAction(event -> verDetallesPedido());
 actualizarButton.setOnAction(event -> actualizarEstadoPedido());
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
 cargarPedidosConfirmados();
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
 String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
 "pr.nombre as producto, p.libras, " +
 "DATEPART(WEEKDAY, p.fecha_entrega) as dia_semana, " +
 "p.fecha_entrega, p.estado " +
 "FROM pedidos p " +
 "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
 "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
 "WHERE p.estado IN ('Confirmado', 'En produccion', 'Listo') " +
 "AND p.fecha_entrega >= DATEADD(DAY, -DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
 "AND p.fecha_entrega <= DATEADD(DAY, 6 - DATEPART(WEEKDAY, GETDATE()), GETDATE()) " +
 "ORDER BY p.fecha_entrega";

 pedidosList = FXCollections.observableArrayList();

 try (PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 Pedido pedido = new Pedido(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("producto"),
 rs.getDouble("libras"),
 rs.getInt("dia_semana"),
 rs.getString("fecha_entrega"),
 rs.getString("estado")
 );
 pedidosList.add(pedido);
 }
 }

 construirSemanalGrid();
 cargarEstadisticas();
 cargarAlertas();

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: usando datos de demostraciUn en planificaciUn");
 pedidosList = FXCollections.observableArrayList();
 construirSemanalGrid();
 totalPedidosLabel.setText("--");
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

 for (Pedido pedido : pedidosList) {
 int diaSemana = pedido.getDiaSemana();
 if (diaSemana == i + 2) {
 Label clienteLbl = new Label(pedido.getNombreCliente());
 clienteLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
 Label productoLbl = new Label(pedido.getProducto());
 productoLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
 Label librasLbl = new Label(pedido.getLibras() + " lbs");
 librasLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
 VBox card = new VBox(2, clienteLbl, productoLbl, librasLbl);
 card.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 6; -fx-border-radius: 4; -fx-background-radius: 4;");
 cell.getChildren().add(card);
 }
 }

 semanaGridPane.add(cell, i + 1, 1);
 }
 }

 private void cargarEstadisticas() {
 try (Connection conn = dbConnection.getConnection()) {
 String sql = "SELECT " +
 "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Confirmado') as total_pedidos, " +
 "(SELECT COUNT(*) FROM pedidos WHERE estado = 'En produccion') as en_produccion, " +
 "(SELECT COUNT(*) FROM pedidos WHERE estado = 'Listo') as listos_entregar";

 try (PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 totalPedidosLabel.setText(String.valueOf(rs.getInt("total_pedidos")));
 enProduccionLabel.setText(String.valueOf(rs.getInt("en_produccion")));
 listosEntregarLabel.setText(String.valueOf(rs.getInt("listos_entregar")));
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: estadAsticas no disponibles");
 }
 }

 private void cargarAlertas() {
 try (Connection conn = dbConnection.getConnection()) {
 String sql = "SELECT TOP 10 " +
 "'Pedido #' + CAST(p.id_pedido AS VARCHAR) + ' - ' + c.nombre + ' - ' + pr.nombre + ' - ' + p.estado as alerta " +
 "FROM pedidos p " +
 "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
 "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
 "WHERE p.estado IN ('En produccion', 'Listo') " +
 "ORDER BY p.fecha_entrega";

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

 private void cargarPedidosConfirmados() {
 try (Connection conn = dbConnection.getConnection()) {
 String sql = "SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
 "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
 "FROM pedidos p " +
 "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
 "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
 "WHERE p.estado IN ('Confirmado', 'En produccion', 'Listo') " +
 "ORDER BY p.fecha_entrega";

 pedidosList = FXCollections.observableArrayList();

 try (PreparedStatement stmt = conn.prepareStatement(sql);
 ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 Pedido pedido = new Pedido(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("producto"),
 rs.getDouble("libras"),
 0,
 rs.getString("fecha_entrega"),
 rs.getString("estado")
 );
 pedidosList.add(pedido);
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: pedidos confirmados no disponibles");
 pedidosList = FXCollections.observableArrayList();
 }
 }

 private void buscarPedido(String textoBusqueda) {
 try (Connection conn = dbConnection.getConnection()) {
 String sql = "SELECT TOP 1 p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente, " +
 "p.estado, p.fecha_entrega, pr.nombre as producto, p.libras " +
 "FROM pedidos p " +
 "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
 "INNER JOIN productos pr ON p.id_producto = pr.id_producto " +
 "WHERE (CAST(p.id_pedido AS VARCHAR) LIKE ? OR " +
 "c.nombre LIKE ? OR c.apellido LIKE ?) " +
 "AND p.estado IN ('Confirmado', 'En produccion', 'Listo')";

 try (PreparedStatement stmt = conn.prepareStatement(sql)) {
 String busqueda = "%" + textoBusqueda + "%";
 stmt.setString(1, busqueda);
 stmt.setString(2, busqueda);
 stmt.setString(3, busqueda);

 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 pedidoSeleccionado = new Pedido(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("producto"),
 rs.getDouble("libras"),
 0,
 rs.getString("fecha_entrega"),
 rs.getString("estado")
 );
 mostrarDetallesPedido(pedidoSeleccionado);
 } else {
 limpiarSeguimiento();
 mostrarMensaje("No Encontrado", "No se encontrA ningAn pedido con ese criterio.");
 }
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: bAsqueda de pedidos no disponible");
 mostrarMensaje("BAAsqueda no disponible", "La bAsqueda de pedidos no estA disponible en modo offline.");
 }
 }

 private void verDetallesPedido() {
 if (pedidoSeleccionado != null) {
 mostrarDetallesPedido(pedidoSeleccionado);
 } else {
 mostrarMensaje("Sin SelecciA n", "Por favor seleccione un pedido para ver sus detalles.");
 }
 }

 private void mostrarDetallesPedido(Pedido pedido) {
 pedidoIdLabel.setText(String.valueOf(pedido.getId()));
 clienteLabel.setText(pedido.getNombreCliente());
 estadoActualLabel.setText(pedido.getEstado());
 construirTimeline(pedido);
 }

 private void construirTimeline(Pedido pedido) {
 timelineListView.getItems().clear();
 String[] pasos = {
 "1. PreparaciA n de masas",
 "2. Horneado",
 "3. Enfriado controlado",
 "4. PreparaciA n de rellenos",
 "5. DecoraciA n",
 "6. Empaque y control de calidad"
 };
 for (String paso : pasos) {
 timelineListView.getItems().add(paso);
 }
 }

 @FXML
 private void actualizarEstadoPedido() {
 if (pedidoSeleccionado != null) {
 mostrarMensaje("Actualizar Estado", "FunciA n de actualizaciA n de estado en desarrollo.");
 } else {
 mostrarMensaje("Sin SelecciA n", "Por favor seleccione un pedido para actualizar su estado.");
 }
 }

 @FXML
 private void marcarComoListo() {
 if (!sessionManager.tienePermiso(Permiso.PRODUCCION_ACTUALIZAR)) {
 mostrarError("Acceso Denegado", "No tienes permiso para actualizar el estado de producciA n.");
 return;
 }
 if (pedidoSeleccionado != null) {
 try (Connection conn = dbConnection.getConnection()) {
 String sql = "UPDATE pedidos SET estado = 'Listo' " +
 "WHERE id_pedido = ?";

 try (PreparedStatement stmt = conn.prepareStatement(sql)) {
 stmt.setInt(1, pedidoSeleccionado.getId());
 int filasAfectadas = stmt.executeUpdate();

 if (filasAfectadas > 0) {
 mostrarMensaje("Pedido Actualizado", "El pedido ha sido marcado como listo para entregar.");
 cargarPedidosConfirmados();
 } else {
 mostrarError("Error al Actualizar", "No se pudo actualizar el estado del pedido.");
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: no se puede actualizar el estado del pedido");
 mostrarMensaje("Modo offline", "No se puede actualizar el estado del pedido en modo offline.");
 }
 } else {
 mostrarMensaje("Sin SelecciA n", "Por favor seleccione un pedido para marcar como listo.");
 }
 }

 private void limpiarSeguimiento() {
 pedidoSeleccionado = null;
 pedidoIdLabel.setText("-");
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

 public static class Pedido {
 private int id;
 private String nombreCliente;
 private String producto;
 private double libras;
 private int diaSemana;
 private String fechaEntrega;
 private String estado;

 public Pedido(int id, String nombreCliente, String producto, double libras, int diaSemana, String fechaEntrega, String estado) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.producto = producto;
 this.libras = libras;
 this.diaSemana = diaSemana;
 this.fechaEntrega = fechaEntrega;
 this.estado = estado;
 }

 public int getId() { return id; }
 public String getNombreCliente() { return nombreCliente; }
 public String getProducto() { return producto; }
 public double getLibras() { return libras; }
 public int getDiaSemana() { return diaSemana; }
 public String getFechaEntrega() { return fechaEntrega; }
 public String getEstado() { return estado; }
 }
}
