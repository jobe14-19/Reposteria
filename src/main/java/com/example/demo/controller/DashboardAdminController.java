package com.example.demo.controller;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
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
import java.io.IOException;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardAdminController {

 private static final Logger LOGGER = Logger.getLogger(DashboardAdminController.class.getName());

 // KPIs
 private static final String SQL_PEDIDOS_PENDIENTES =
  "SELECT COUNT(*) as total FROM pedidos WHERE estado IN ('Pendiente','Confirmado','Programado')";
 private static final String SQL_PROD_ACTIVA =
  "SELECT COUNT(*) as total FROM ordenes_produccion WHERE estado IN ('ACTIVA','EN_PRODUCCION')";
 private static final String SQL_PEDIDOS_HOY =
  "SELECT COUNT(*) as total FROM pedidos WHERE CAST(fecha_entrega AS DATE) = CAST(GETDATE() AS DATE)";
 private static final String SQL_STOCK_BAJO =
  "SELECT COUNT(*) as total FROM inventario WHERE stock_actual < stock_minimo";
 private static final String SQL_ENTREGAS_HOY =
  "SELECT COUNT(*) as total FROM entregas WHERE CAST(fecha_entrega AS DATE) = CAST(GETDATE() AS DATE)";
  private static final String SQL_SALDO_PENDIENTE =
  "SELECT COALESCE(SUM(total - ISNULL(adelanto, 0)), 0) as total FROM pedidos WHERE total > ISNULL(adelanto, 0)";
 private static final String SQL_PROD_ATRASADA =
  "SELECT COUNT(*) as total FROM ordenes_produccion WHERE fecha_entrega < GETDATE() AND estado NOT IN ('COMPLETADA','ENTREGADA','CANCELADA')";
 private static final String SQL_CLIENTES_NUEVOS =
  "SELECT COUNT(*) as total FROM clientes WHERE MONTH(fecha_registro) = MONTH(GETDATE()) AND YEAR(fecha_registro) = YEAR(GETDATE())";

 // Charts
  private static final String SQL_VENTAS_7_DIAS =
  "SELECT CONVERT(VARCHAR, CAST(fecha_pedido AS DATE), 103) as dia, SUM(total) as ventas FROM pedidos WHERE fecha_pedido >= DATEADD(day, -6, GETDATE()) GROUP BY CAST(fecha_pedido AS DATE) ORDER BY CAST(fecha_pedido AS DATE)";
  private static final String SQL_PRODUCTOS_TOP =
  "SELECT TOP 10 p.nombre as producto, COUNT(*) as unidades FROM detalles_pedido dp INNER JOIN productos p ON dp.id_producto = p.id_producto INNER JOIN pedidos pe ON dp.id_pedido = pe.id_pedido WHERE pe.fecha_pedido >= DATEADD(month, -1, GETDATE()) GROUP BY p.nombre ORDER BY COUNT(*) DESC";

 // Tables
  private static final String SQL_ENTREGAS_PROXIMAS =
  "SELECT TOP 10 FORMAT(hora_entrega, 'HH:mm') as hora, cliente, direccion, estado FROM entregas WHERE CAST(fecha_entrega AS DATE) = CAST(GETDATE() AS DATE) ORDER BY hora_entrega ASC";
 private static final String SQL_ALERTAS =
  "SELECT TOP 15 'Stock Bajo' as tipo, i.ingrediente + ' - Disp: ' + CAST(CAST(i.stock_actual AS INT) AS VARCHAR) + ' (Min: ' + CAST(CAST(i.stock_minimo AS INT) AS VARCHAR) + ')' as descripcion, FORMAT(GETDATE(), 'yyyy-MM-dd HH:mm') as fecha, CASE WHEN i.stock_actual < i.stock_minimo THEN 'CRITICO' ELSE 'BAJO' END as estado FROM inventario i WHERE i.stock_actual < i.stock_minimo * 1.2 UNION ALL SELECT TOP 5 'Atrasado' as tipo, 'Pedido #' + CAST(p.id_pedido AS VARCHAR) + ' - ' + c.nombre as descripcion, FORMAT(p.fecha_entrega, 'yyyy-MM-dd HH:mm') as fecha, 'ATRASADO' as estado FROM pedidos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente WHERE p.estado = 'Atrasado' ORDER BY fecha DESC";
 private static final String SQL_CLIENTES_RECIENTES =
  "SELECT TOP 10 c.nombre, c.telefono, FORMAT(c.fecha_registro, 'yyyy-MM-dd') as fecha_registro, COALESCE(SUM(p.total), 0) as total_gastado FROM clientes c LEFT JOIN pedidos p ON c.id_cliente = p.id_cliente GROUP BY c.id_cliente, c.nombre, c.telefono, c.fecha_registro ORDER BY c.fecha_registro DESC";
 private static final String SQL_ACTIVIDADES =
  "SELECT TOP 20 FORMAT(a.fecha_hora, 'yyyy-MM-dd HH:mm') as fecha, a.usuario, a.accion, a.detalle FROM actividad a ORDER BY a.fecha_hora DESC";

 private static final int REFRESH_INTERVAL_MS = 30000;

 @FXML private Label userLabel, lastUpdateLabel;
 @FXML private Label pedidosPendientesLabel, prodActivaLabel, pedidosHoyLabel, stockBajoLabel;
 @FXML private Label entregasHoyLabel, saldoPendienteLabel, atrasadaLabel, clientesNuevosLabel;

 @FXML private LineChart<String, Number> salesChart;
 @FXML private CategoryAxis xAxis;
 @FXML private NumberAxis yAxis;
 @FXML private BarChart<String, Number> productsChart;
 @FXML private CategoryAxis productsXAxis;
 @FXML private NumberAxis productsYAxis;

 @FXML private TableView<ProximaEntrega> entregasProxTable;
 @FXML private TableColumn<ProximaEntrega, String> entProxHoraColumn;
 @FXML private TableColumn<ProximaEntrega, String> entProxClienteColumn;
 @FXML private TableColumn<ProximaEntrega, String> entProxDireccionColumn;
 @FXML private TableColumn<ProximaEntrega, String> entProxEstadoColumn;

 @FXML private TableView<Alerta> alertasTable;
 @FXML private TableColumn<Alerta, String> alertTipoColumn;
 @FXML private TableColumn<Alerta, String> alertDescripcionColumn;
 @FXML private TableColumn<Alerta, String> alertFechaColumn;
 @FXML private TableColumn<Alerta, String> alertEstadoColumn;

 @FXML private TableView<ClienteReciente> clientesRecTable;
 @FXML private TableColumn<ClienteReciente, String> cliRecNombreColumn;
 @FXML private TableColumn<ClienteReciente, String> cliRecTelefonoColumn;
 @FXML private TableColumn<ClienteReciente, String> cliRecFechaColumn;
 @FXML private TableColumn<ClienteReciente, String> cliRecTotalColumn;

 @FXML private TableView<Actividad> actividadesTable;
 @FXML private TableColumn<Actividad, String> actFechaColumn;
 @FXML private TableColumn<Actividad, String> actUsuarioColumn;
 @FXML private TableColumn<Actividad, String> actAccionColumn;
 @FXML private TableColumn<Actividad, String> actDetalleColumn;

  @FXML private VBox topBar;
  private SessionManager sessionManager;
  private DatabaseConnection dbConnection;
  private Timer refreshTimer;
  private DateTimeFormatter timeFormatter;
  private boolean modoEmbedded;

  public void setModoEmbedded(boolean b) {
  this.modoEmbedded = b;
  if (topBar != null) {
  topBar.setVisible(!b);
  topBar.setManaged(!b);
  }
  }

  @FXML
  public void initialize() {
  sessionManager = SessionManager.getInstance();
  dbConnection = DatabaseConnection.getInstance();
  timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

  configurarTablas();
  cargarDatosAdministrador();
  iniciarAutoRefresh();
  actualizarInfoUsuario();
  actualizarTimestamp();
  }

 private void configurarTablas() {
  entProxHoraColumn.setCellValueFactory(new PropertyValueFactory<>("hora"));
  entProxClienteColumn.setCellValueFactory(new PropertyValueFactory<>("cliente"));
  entProxDireccionColumn.setCellValueFactory(new PropertyValueFactory<>("direccion"));
  entProxEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));

  alertTipoColumn.setCellValueFactory(new PropertyValueFactory<>("tipo"));
  alertDescripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
  alertFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
  alertEstadoColumn.setCellValueFactory(new PropertyValueFactory<>("estado"));

  cliRecNombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
  cliRecTelefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
  cliRecFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaRegistro"));
  cliRecTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalGastado"));

  actFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
  actUsuarioColumn.setCellValueFactory(new PropertyValueFactory<>("usuario"));
  actAccionColumn.setCellValueFactory(new PropertyValueFactory<>("accion"));
  actDetalleColumn.setCellValueFactory(new PropertyValueFactory<>("detalle"));
 }

 private void cargarDatosAdministrador() {
  if (!sessionManager.isLoggedIn()) return;
  try (Connection conn = dbConnection.getConnection()) {
  cargarKPIs(conn);
  cargarGraficoVentas(conn);
  cargarGraficoProductos(conn);
  cargarEntregasProximas(conn);
  cargarAlertas(conn);
  cargarClientesRecientes(conn);
  cargarActividades(conn);
  } catch (SQLException e) {
  LOGGER.log(Level.INFO, "Modo offline: {0}", e.getMessage());
  String na = "--";
  pedidosPendientesLabel.setText(na); prodActivaLabel.setText(na);
  pedidosHoyLabel.setText(na); stockBajoLabel.setText(na);
  entregasHoyLabel.setText(na); saldoPendienteLabel.setText(na);
  atrasadaLabel.setText(na); clientesNuevosLabel.setText(na);
  salesChart.setData(FXCollections.observableArrayList());
  productsChart.setData(FXCollections.observableArrayList());
  }
 }

 private void cargarKPIs(Connection conn) {
  ejecutarKPI(conn, SQL_PEDIDOS_PENDIENTES, pedidosPendientesLabel);
  ejecutarKPI(conn, SQL_PROD_ACTIVA, prodActivaLabel);
  ejecutarKPI(conn, SQL_PEDIDOS_HOY, pedidosHoyLabel);
  ejecutarKPI(conn, SQL_STOCK_BAJO, stockBajoLabel);
  ejecutarKPI(conn, SQL_ENTREGAS_HOY, entregasHoyLabel);
  ejecutarKPI(conn, SQL_PROD_ATRASADA, atrasadaLabel);
  ejecutarKPI(conn, SQL_CLIENTES_NUEVOS, clientesNuevosLabel);
  try (PreparedStatement stmt = conn.prepareStatement(SQL_SALDO_PENDIENTE);
    ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) saldoPendienteLabel.setText(String.format("$%.2f", rs.getDouble("total")));
  } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error saldo: {0}", e.getMessage()); }
 }

 private void ejecutarKPI(Connection conn, String sql, Label label) {
  try (PreparedStatement stmt = conn.prepareStatement(sql);
    ResultSet rs = stmt.executeQuery()) {
  if (rs.next()) label.setText(String.valueOf(rs.getInt("total")));
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "Error KPI: {0}", e.getMessage());
  }
 }

 private void cargarGraficoVentas(Connection conn) {
  ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_VENTAS_7_DIAS);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) data.add(new XYChart.Data<>(rs.getString("dia"), rs.getDouble("ventas")));
  XYChart.Series<String, Number> series = new XYChart.Series<>();
  series.setData(data);
  salesChart.setData(FXCollections.observableArrayList(series));
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "Error ventas: {0}", e.getMessage());
  salesChart.setData(FXCollections.observableArrayList());
  }
 }

 private void cargarGraficoProductos(Connection conn) {
  ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_PRODUCTOS_TOP);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
   int uds = rs.getInt("unidades");
   if (uds > 0) data.add(new XYChart.Data<>(rs.getString("producto"), uds));
  }
  XYChart.Series<String, Number> series = new XYChart.Series<>();
  series.setData(data);
  productsChart.setData(FXCollections.observableArrayList(series));
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "Error productos: {0}", e.getMessage());
  productsChart.setData(FXCollections.observableArrayList());
  }
 }

 private void cargarEntregasProximas(Connection conn) {
  ObservableList<ProximaEntrega> list = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_ENTREGAS_PROXIMAS);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) list.add(new ProximaEntrega(
   rs.getString("hora"), rs.getString("cliente"),
   rs.getString("direccion"), rs.getString("estado")));
  } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error entregas: {0}", e.getMessage()); }
  entregasProxTable.setItems(list);
 }

 private void cargarAlertas(Connection conn) {
  ObservableList<Alerta> list = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_ALERTAS);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) list.add(new Alerta(
   rs.getString("tipo"), rs.getString("descripcion"),
   rs.getString("fecha"), rs.getString("estado")));
  } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error alertas: {0}", e.getMessage()); }
  alertasTable.setItems(list);
 }

 private void cargarClientesRecientes(Connection conn) {
  ObservableList<ClienteReciente> list = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_CLIENTES_RECIENTES);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) list.add(new ClienteReciente(
   rs.getString("nombre"), rs.getString("telefono"),
   rs.getString("fecha_registro"), String.format("$%.2f", rs.getDouble("total_gastado"))));
  } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error clientes: {0}", e.getMessage()); }
  clientesRecTable.setItems(list);
 }

 private void cargarActividades(Connection conn) {
  ObservableList<Actividad> list = FXCollections.observableArrayList();
  try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTIVIDADES);
    ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) list.add(new Actividad(
   rs.getString("fecha"), rs.getString("usuario"),
   rs.getString("accion"), rs.getString("detalle")));
  } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error actividad: {0}", e.getMessage()); }
  actividadesTable.setItems(list);
 }

 private void iniciarAutoRefresh() {
  refreshTimer = new Timer(true);
  refreshTimer.scheduleAtFixedRate(new TimerTask() {
  @Override
  public void run() {
  javafx.application.Platform.runLater(() -> {
   cargarDatosAdministrador();
   actualizarTimestamp();
  });
  }
  }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
 }

 private void actualizarInfoUsuario() {
  if (sessionManager.isLoggedIn()) {
  userLabel.setText(" " + sessionManager.getUsuarioActual() + " (ADMINISTRADOR)");
  }
 }

 private void actualizarTimestamp() {
  lastUpdateLabel.setText("Última actualización: " + LocalDateTime.now().format(timeFormatter));
 }

 @FXML private void cerrarSesion(ActionEvent event) {
  if (refreshTimer != null) refreshTimer.cancel();
  sessionManager.cerrarSesion();
  try {
  Stage stage = (Stage) userLabel.getScene().getWindow();
  stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/example/demo/Login.fxml")), 1280, 720));
  stage.setTitle(" Repostería Rosato - Sistema de Gestión");
  } catch (Exception e) { LOGGER.log(Level.SEVERE, "Error: {0}", e.getMessage()); }
 }

 @FXML private void mostrarDashboard(ActionEvent event) { cargarDatosAdministrador(); actualizarTimestamp(); }
 @FXML private void mostrarClientes(ActionEvent event) { navegarAVista("Clientes.fxml", "Gestión de Clientes"); }
 @FXML private void mostrarPedidos(ActionEvent event) { navegarAVista("Pedidos.fxml", "Gestión de Pedidos"); }
 @FXML private void mostrarProduccion(ActionEvent event) { navegarAVista("Planificacion.fxml", "Gestión de Producción"); }
 @FXML private void mostrarInventario(ActionEvent event) { navegarAVista("Inventario.fxml", "Gestión de Inventario"); }
 @FXML private void mostrarEntregas(ActionEvent event) { navegarAVista("Entregas.fxml", "Gestión de Entregas"); }
 @FXML private void mostrarPersonal(ActionEvent event) { navegarAVista("Personal.fxml", "Gestión de Personal"); }

 @FXML private void mostrarReportes(ActionEvent event) {
  try {
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Reportes.fxml"));
  Parent root = loader.load();
  Stage stage = new Stage();
  stage.setScene(new Scene(root, 700, 500));
  stage.setTitle("Repostería Rosato - Reportes");
  stage.initModality(Modality.APPLICATION_MODAL);
  stage.show();
  } catch (Exception e) {
  LOGGER.log(Level.SEVERE, "Error reportes: {0}", e.getMessage());
  mostrarError("Error", "No se pudo abrir reportes");
  }
 }

 @FXML private void mostrarAlertas(ActionEvent event) {
  mostrarMensaje("Alertas del Sistema", "Las alertas se muestran en el panel del dashboard.");
 }

 @FXML private void mostrarLogs(ActionEvent event) {
  mostrarMensaje("Logs del Sistema", "Los logs se muestran en el panel del dashboard.");
 }

 private void navegarAVista(String fxmlName, String titulo) {
  try {
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/" + fxmlName));
  Parent viewRoot = loader.load();
  BorderPane wrapper = new BorderPane();
  HBox navBar = new HBox(15);
  navBar.setPadding(new Insets(15, 30, 15, 30));
  navBar.setStyle("-fx-background-color: #8B5E3C; -fx-border-color: #7A4D2B; -fx-border-width: 0 0 2 0;");
  navBar.setAlignment(Pos.CENTER_LEFT);
  Button btnVolver = new Button("← Cerrar");
  btnVolver.setStyle("-fx-background-color: white; -fx-text-fill: #8B5E3C; -fx-font-weight: bold; -fx-background-radius: 10;");
  btnVolver.setOnAction(e -> ((Stage) btnVolver.getScene().getWindow()).close());
  Label title = new Label(titulo);
  title.setFont(Font.font("System", FontWeight.BOLD, 18));
  title.setStyle("-fx-text-fill: white;");
  navBar.getChildren().addAll(btnVolver, title);
  wrapper.setTop(navBar);
  wrapper.setCenter(viewRoot);
  Stage newStage = new Stage();
  newStage.setScene(new Scene(wrapper, 1280, 720));
  newStage.setTitle("Repostería Rosato - " + titulo);
  newStage.show();
  } catch (Exception e) {
  LOGGER.log(Level.SEVERE, "Error navegar: {0}", e.getMessage());
  mostrarError("Error", "No se pudo abrir " + titulo);
  }
 }

 private void mostrarError(String t, String m) { mostrarAlerta(Alert.AlertType.ERROR, t, m); }
 private void mostrarMensaje(String t, String m) { mostrarAlerta(Alert.AlertType.INFORMATION, t, m); }
 private void mostrarAlerta(Alert.AlertType tipo, String t, String m) {
  Alert a = new Alert(tipo); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
 }

 // --- Data classes ---

 public static class ProximaEntrega {
  private String hora, cliente, direccion, estado;
  public ProximaEntrega(String h, String c, String d, String e) {
  this.hora = h; this.cliente = c; this.direccion = d; this.estado = e;
  }
  public String getHora() { return hora; }
  public String getCliente() { return cliente; }
  public String getDireccion() { return direccion; }
  public String getEstado() { return estado; }
 }

 public static class Alerta {
  private String tipo, descripcion, fechaHora, estado;
  public Alerta(String t, String d, String f, String e) {
  this.tipo = t; this.descripcion = d; this.fechaHora = f; this.estado = e;
  }
  public String getTipo() { return tipo; }
  public String getDescripcion() { return descripcion; }
  public String getFechaHora() { return fechaHora; }
  public String getEstado() { return estado; }
 }

 public static class ClienteReciente {
  private String nombre, telefono, fechaRegistro, totalGastado;
  public ClienteReciente(String n, String t, String f, String tg) {
  this.nombre = n; this.telefono = t; this.fechaRegistro = f; this.totalGastado = tg;
  }
  public String getNombre() { return nombre; }
  public String getTelefono() { return telefono; }
  public String getFechaRegistro() { return fechaRegistro; }
  public String getTotalGastado() { return totalGastado; }
 }

 public static class Actividad {
  private String fechaHora, usuario, accion, detalle;
  public Actividad(String f, String u, String a, String d) {
  this.fechaHora = f; this.usuario = u; this.accion = a; this.detalle = d;
  }
  public String getFechaHora() { return fechaHora; }
  public String getUsuario() { return usuario; }
  public String getAccion() { return accion; }
  public String getDetalle() { return detalle; }
 }
}
