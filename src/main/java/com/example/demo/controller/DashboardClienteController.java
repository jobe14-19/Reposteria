package com.example.demo.controller;
import com.example.demo.service.PayPalConfig;
import com.example.demo.service.PayPalService;
import com.example.demo.service.PayPalService.PayPalCheckoutResult;
import com.example.demo.service.SessionManager;
import com.example.demo.dao.PedidoDAO;
import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.dao.PagoDAO;
import com.example.demo.model.Pago;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardClienteController {

 private static final Logger LOGGER = Logger.getLogger(DashboardClienteController.class.getName());

 private static final String SQL_KPI_CLIENTE =
 "SELECT COUNT(*) as total_pedidos, COALESCE(SUM(total), 0) as total_gastado FROM pedidos WHERE username = ?";
  private static final String SQL_PROXIMO_PEDIDO =
  "SELECT TOP 1 fecha_entrega FROM pedidos WHERE username = ? AND estado = 'PROGRAMADO' ORDER BY fecha_entrega ASC";
  private static final String SQL_EN_PROCESO =
  "SELECT COUNT(*) as total FROM pedidos WHERE username = ? AND estado IN ('Confirmado','En Proceso','Programado')";
  private static final String SQL_PEDIDOS_RECIENTES =
  "SELECT TOP 5 id_pedido, producto, libras, fecha_entrega, total, estado, ISNULL(estado_pago, 'Pendiente') as estado_pago FROM pedidos WHERE username = ? ORDER BY fecha_entrega DESC";
 private static final String SQL_PRODUCTO_FAVORITO =
 "SELECT TOP 1 producto, COUNT(*) as frecuencia FROM pedidos WHERE username = ? GROUP BY producto ORDER BY frecuencia DESC";
  private static final String SQL_TOTAL_PEDIDO =
  "SELECT id_pedido, total, producto, estado_pago FROM pedidos WHERE id_pedido = ?";
  private static final String SQL_ACTUALIZAR_ESTADO_PAGO =
  "UPDATE pedidos SET adelanto = total, estado_pago = 'Pagado', tipo_pago = 'PayPal' WHERE id_pedido = ?";
  private static final String SQL_ACTUALIZAR_ORDEN_PAGO =
  "UPDATE ordenes_produccion SET anticipo = precio_venta, saldo = 0, estado_pago = 'Pagado', tipo_pago = 'PayPal' WHERE id_pedido = ?";

 private static final int REFRESH_INTERVAL_MS = 30000;
 private static final int PUNTOS_POR_GASTO = 10;
 private static final String PRODUCTO_POR_DEFECTO = "Pastel de Chocolate";
 private static final String MENSAJE_PROMO1 = "C\u00f3digo: WELCOME10";
 private static final String MENSAJE_PROMO3 = "V\u00e1lido este mes";

  @FXML private Label userLabel;
  @FXML private Label lastUpdateLabel;
  @FXML private Label welcomeLabel;
  @FXML private Label totalPedidosLabel;
  @FXML private Label proxPedidoLabel;
  @FXML private Label enProcesoLabel;
  @FXML private Label puntosLabel;
  @FXML private Label totalGastadoLabel;
 @FXML private TableView<Pedido> pedidosTable;
 @FXML private TableColumn<Pedido, Integer> idColumn;
 @FXML private TableColumn<Pedido, String> productoColumn;
 @FXML private TableColumn<Pedido, String> librasColumn;
 @FXML private TableColumn<Pedido, String> fechaColumn;
 @FXML private TableColumn<Pedido, String> totalColumn;
 @FXML private TableColumn<Pedido, String> estadoColumn;
  @FXML private TableColumn<Pedido, String> estadoPagoColumn;
  @FXML private TableColumn<Pedido, String> accionColumn;
 @FXML private Label sugerenciaProductoLabel;
 @FXML private Label sugerenciaDescLabel;
 @FXML private Label promo1Label;
 @FXML private Label promo3Label;

  @FXML private VBox topBar;
  private SessionManager sessionManager;
  private DatabaseConnection dbConnection;
  private Timer refreshTimer;
  private boolean modoEmbedded;
 private DateTimeFormatter timeFormatter;

  public void setModoEmbedded(boolean b) {
  this.modoEmbedded = b;
  if (topBar != null) { topBar.setVisible(!b); topBar.setManaged(!b); }
  }

  @FXML
  public void initialize() {
  sessionManager = SessionManager.getInstance();
  dbConnection = DatabaseConnection.getInstance();
 timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

 configurarTablaPedidos();
 cargarDatosCliente();
 iniciarAutoRefresh();
 actualizarInfoUsuario();
 actualizarTimestamp();
 }

 private void configurarTablaPedidos() {
 idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 productoColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
 librasColumn.setCellValueFactory(new PropertyValueFactory<>("libras"));
 fechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
 totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
  estadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));
  estadoPagoColumn.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));

   estadoPagoColumn.setCellFactory(col -> new TableCell<>() {
    @Override protected void updateItem(String est, boolean empty) {
     super.updateItem(est, empty);
     if (empty || est == null) { setText(null); setStyle(""); return; }
     setText(est);
     String bg;
     switch (est != null ? est : "") {
      case "Pagado": bg = "#28A745"; break;
      case "En Proceso": bg = "#FF9800"; break;
      case "Pendiente": bg = "#DC3545"; break;
      default: bg = "#6C757D";
     }
     setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
     setAlignment(Pos.CENTER);
    }
   });

   accionColumn.setCellFactory(col -> new TableCell<>() {
    private final Button pagarBtn = new Button("Pagar");
    { pagarBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"); }
    @Override protected void updateItem(String item, boolean empty) {
     super.updateItem(item, empty);
     if (empty) { setGraphic(null); return; }
     Pedido p = getTableView().getItems().get(getIndex());
     boolean mostrar = p != null && "Pendiente".equals(p.getEstadoPago()) && PayPalConfig.isConfigured();
     if (mostrar) {
      pagarBtn.setOnAction(e -> pagarPedido(p));
      setGraphic(pagarBtn);
     } else {
      setGraphic(null);
     }
    }
   });
  }

 private void cargarDatosCliente() {
 String username = sessionManager.getUsuarioActual();

 try (Connection conn = dbConnection.getConnection()) {
 cargarKPIs(conn, username);
 cargarPedidosRecientes(conn, username);
 cargarSugerencias(conn, username);
 cargarPromociones();
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar datos del cliente: {0}", e.getMessage());
 mostrarError("Error de conexi\u00f3n", "No se pudieron cargar tus datos. Intenta nuevamente.");
 cargarDatosOffline(username);
 }
 }
 
 private void cargarDatosOffline(String username) {
 totalPedidosLabel.setText("5");
 totalGastadoLabel.setText("RD$125.50");
 puntosLabel.setText("12");
 proxPedidoLabel.setText("2026-05-15");
 
 ObservableList<Pedido> pedidos = FXCollections.observableArrayList();
pedidos.add(new Pedido(1, "Pastel de Chocolate", "2.5", "2026-05-15", "RD$45.00", "Confirmado"));
  pedidos.add(new Pedido(2, "Tres Leches", "1.8", "2026-05-12", "RD$32.40", "Entregado"));
  pedidos.add(new Pedido(3, "Cheesecake", "1.2", "2026-05-10", "RD$28.00", "Entregado"));
 pedidosTable.setItems(pedidos);

 sugerenciaProductoLabel.setText("Pastel de Chocolate");
 sugerenciaDescLabel.setText("Basado en tu historial, te recomendamos: Pastel de Chocolate");

 cargarPromociones();
 }

 private void cargarKPIs(Connection conn, String username) throws SQLException {
 try (PreparedStatement stmt = conn.prepareStatement(SQL_KPI_CLIENTE)) {
 stmt.setString(1, username);
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 int totalPedidos = rs.getInt("total_pedidos");
 double totalGastado = rs.getDouble("total_gastado");
 totalPedidosLabel.setText(String.valueOf(totalPedidos));
 totalGastadoLabel.setText(String.format("RD$%.2f", totalGastado));
 int puntos = (int) (totalGastado / PUNTOS_POR_GASTO);
 puntosLabel.setText(String.valueOf(puntos));
 }
 }
 }

  try (PreparedStatement stmt = conn.prepareStatement(SQL_PROXIMO_PEDIDO)) {
  stmt.setString(1, username);
  try (ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) {
  proxPedidoLabel.setText(rs.getString("fecha_entrega"));
  } else {
  proxPedidoLabel.setText("No programado");
  }
  }
  }

  try (PreparedStatement stmt = conn.prepareStatement(SQL_EN_PROCESO)) {
  stmt.setString(1, username);
  try (ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) enProcesoLabel.setText(String.valueOf(rs.getInt("total")));
  }
  }
  }

 private void cargarPedidosRecientes(Connection conn, String username) throws SQLException {
 ObservableList<Pedido> pedidos = FXCollections.observableArrayList();
 try (PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_RECIENTES)) {
 stmt.setString(1, username);
 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 pedidos.add(new Pedido(
 rs.getInt("id_pedido"),
 rs.getString("producto"),
 rs.getString("libras"),
 rs.getString("fecha_entrega"),
 String.format("RD$%.2f", rs.getDouble("total")),
   rs.getString("estado"),
   rs.getString("estado_pago")
   ));
  }
 }
 }
 pedidosTable.setItems(pedidos);
 }

 private void cargarSugerencias(Connection conn, String username) throws SQLException {
 try (PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTO_FAVORITO)) {
 stmt.setString(1, username);
 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 String productoFavorito = rs.getString("producto");
 sugerenciaProductoLabel.setText(productoFavorito);
 sugerenciaDescLabel.setText("Basado en tu historial, te recomendamos: " + productoFavorito);
 } else {
 setSugerenciasPorDefecto();
 }
 }
 }
 }

 private void setSugerenciasPorDefecto() {
 sugerenciaProductoLabel.setText(PRODUCTO_POR_DEFECTO);
 sugerenciaDescLabel.setText("Nuestro producto m\u00e1s popular");
 }

 private void cargarPromociones() {
 promo1Label.setText(MENSAJE_PROMO1);
 promo3Label.setText(MENSAJE_PROMO3);
 }

 private void iniciarAutoRefresh() {
 refreshTimer = new Timer(true);
 refreshTimer.scheduleAtFixedRate(new TimerTask() {
 @Override
 public void run() {
 javafx.application.Platform.runLater(new Runnable() {
 @Override
 public void run() {
 cargarDatosCliente();
 actualizarTimestamp();
 }
 });
 }
 }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
 }

 private void actualizarInfoUsuario() {
 if (sessionManager.isLoggedIn()) {
 String nombre = sessionManager.getUsuarioActual();
 userLabel.setText(" " + nombre + " (CLIENTE)");
 welcomeLabel.setText("\u00a1Bienvenido de vuelta, " + nombre + "!");
 } else {
 userLabel.setText(" Invitado");
 welcomeLabel.setText("\u00a1Bienvenido!");
 }
 }

 private void actualizarTimestamp() {
 String now = LocalDateTime.now().format(timeFormatter);
 lastUpdateLabel.setText("\u00daltima actualizaci\u00f3n: " + now);
 }

 @FXML
 private void cerrarSesion(ActionEvent event) {
 if (refreshTimer != null) {
 refreshTimer.cancel();
 }

 sessionManager.cerrarSesion();

 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
 Parent root = loader.load();

 Stage stage = (Stage) userLabel.getScene().getWindow();
 Scene scene = new Scene(root, 1280, 720);
 stage.setScene(scene);
 stage.setTitle(" Reposter\u00eda Rosato - Sistema de Gesti\u00f3n");
 stage.show();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al regresar al login: {0}", e.getMessage());
 }
 }

 @FXML
 private void mostrarDashboard(ActionEvent event) {
 cargarDatosCliente();
 actualizarTimestamp();
 }

    @FXML
 private void verMiPerfil(ActionEvent event) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/MiPerfil.fxml"));
 Parent root = loader.load();

 Stage stage = new Stage();
 Scene scene = new Scene(root, 800, 600);
 stage.setScene(scene);
 stage.setTitle(" Reposter\u00eda Rosato - Mi Perfil");
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.show();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al abrir Mi Perfil: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir la ventana de perfil");
 }
 }

@FXML
private void verChefsBox(ActionEvent event) {
  try {
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ChefsBox.fxml"));
  Parent root = loader.load();
  Stage stage = new Stage();
  stage.setScene(new Scene(root, 900, 550));
  stage.setTitle("Reposteria Rosato - Chef's Box");
  stage.initModality(Modality.APPLICATION_MODAL);
  stage.show();
  } catch (Exception e) {
  LOGGER.log(Level.SEVERE, "Error al abrir Chef's Box", e);
  mostrarError("Error", "No se pudo abrir Chef's Box");
  }
  }

 @FXML
 private void pedirSugerencia(ActionEvent event) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ClientePedidoForm.fxml"));
 Parent root = loader.load();
 Stage stage = new Stage();
 stage.setScene(new Scene(root, 900, 700));
 stage.setTitle("Reposter\u00eda Rosato - Nuevo Pedido");
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();
 cargarDatosCliente();
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al abrir formulario de pedido: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir el formulario de pedido");
 }
 }

  private void pagarPedido(Pedido pedidoView) {
   try (Connection conn = dbConnection.getConnection()) {
    try (PreparedStatement stmt = conn.prepareStatement(SQL_TOTAL_PEDIDO)) {
     stmt.setInt(1, pedidoView.getId());
     try (ResultSet rs = stmt.executeQuery()) {
      if (!rs.next()) { mostrarError("Error", "Pedido no encontrado"); return; }
      double total = rs.getDouble("total");
      String producto = rs.getString("producto");
      if (total <= 0) { mostrarError("Sin Precio", "El administrador aun no ha asignado un precio a este pedido."); return; }
      if ("Pagado".equals(rs.getString("estado_pago"))) { mostrarMensaje("Ya Pagado", "Este pedido ya fue pagado."); return; }
      PayPalService paypal = new PayPalService();
      PayPalCheckoutResult res = paypal.crearCheckoutSession(total, producto, sessionManager.getUsuarioActual(), pedidoView.getId());
      if (!res.ok) { mostrarError("Error de Pago", res.url); return; }
      try {
       java.awt.Desktop.getDesktop().browse(java.net.URI.create(res.url));
      } catch (Exception ignored) {
       mostrarMensaje("Pago", "Abre este enlace en tu navegador para pagar:\n" + res.url);
       return;
      }
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.setTitle("Pago en Proceso");
      confirm.setHeaderText("Completaste el pago en PayPal?");
      confirm.setContentText("Si ya pagaste, presiona OK para verificar. Si no, presiona Cancelar.");
      confirm.showAndWait().ifPresent(r -> {
       if (r == javafx.scene.control.ButtonType.OK) {
        if (res.sessionId != null && paypal.verificarPago(res.sessionId)) {
          try (PreparedStatement u1 = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO_PAGO);
               PreparedStatement u2 = conn.prepareStatement(SQL_ACTUALIZAR_ORDEN_PAGO)) {
           conn.setAutoCommit(false);
           u1.setInt(1, pedidoView.getId()); u1.executeUpdate();
           u2.setInt(1, pedidoView.getId()); u2.executeUpdate();
           conn.commit();
           new PagoDAO().insertar(new Pago(0, pedidoView.getId(), total, null, "PayPal", "PayPal Checkout", "Pagado"));
           mostrarMensaje("Pago Exitoso", "Pedido pagado y enviado a produccion. Gracias!");
           cargarDatosCliente();
         } catch (SQLException ex) {
          LOGGER.log(Level.SEVERE, "Error al actualizar pago: {0}", ex.getMessage());
          mostrarError("Error", "El pago se realizo pero hubo un error al actualizar el sistema. Contacta al administrador.");
         }
        } else {
         mostrarError("Pago No Verificado", "No pudimos confirmar el pago. Si pagaste, contacta al administrador.");
        }
       }
      });
     }
    }
   } catch (SQLException e) {
    LOGGER.log(Level.SEVERE, "Error en pagarPedido: {0}", e.getMessage());
    mostrarError("Error", "Error al procesar el pago.");
   }
  }

  private void mostrarError(String titulo, String mensaje) {
 mostrarAlerta(Alert.AlertType.ERROR, titulo, mensaje);
 }

 private void mostrarMensaje(String titulo, String mensaje) {
 mostrarAlerta(Alert.AlertType.INFORMATION, titulo, mensaje);
 }

 private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
 Alert alert = new Alert(tipo);
 alert.setTitle(titulo);
 alert.setHeaderText(null);
 alert.setContentText(mensaje);
 alert.showAndWait();
 }

 public static class Pedido {
 private int id;
 private String producto;
 private String libras;
 private String fechaEntrega;
 private String total;
 private String estado;
 private String estadoPago;

 public Pedido(int id, String producto, String libras, String fechaEntrega, String total, String estado) {
 this(id, producto, libras, fechaEntrega, total, estado, "Pendiente");
 }

 public Pedido(int id, String producto, String libras, String fechaEntrega, String total, String estado, String estadoPago) {
 this.id = id;
 this.producto = producto;
 this.libras = libras;
 this.fechaEntrega = fechaEntrega;
 this.total = total;
 this.estado = estado;
 this.estadoPago = estadoPago;
 }

 public int getId() { return id; }
 public String getProducto() { return producto; }
 public String getLibras() { return libras; }
 public String getFechaEntrega() { return fechaEntrega; }
 public String getTotal() { return total; }
 public String getEstado() { return estado; }
 public String getEstadoPago() { return estadoPago; }

 public boolean isEntregado() { return "ENTREGADO".equalsIgnoreCase(estado); }
 public boolean isProgramado() { return "PROGRAMADO".equalsIgnoreCase(estado); }
 public boolean isCancelado() { return "CANCELADO".equalsIgnoreCase(estado); }
 }
}
