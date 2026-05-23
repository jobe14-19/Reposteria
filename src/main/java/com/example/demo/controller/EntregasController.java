package com.example.demo.controller;
import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import com.example.demo.service.ReportService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntregasController {

 private static final Logger LOGGER = Logger.getLogger(EntregasController.class.getName());

 // Constantes SQL - CORREGIDO para SQLite
 private static final String SQL_PEDIDOS_PENDIENTES = """
 SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente,
 c.direccion, p.total, p.adelanto,
 p.total - p.adelanto as saldo,
 CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo
 FROM pedidos p
 INNER JOIN clientes c ON p.id_cliente = c.id_cliente
 WHERE p.estado = 'Listo para entregar'
 AND (p.total - p.adelanto) > 0
 ORDER BY p.fecha_entrega
 """;

 private static final String SQL_HISTORIAL_ENTREGAS = """
 SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente,
 CAST(p.fecha_entrega AS DATE) as fecha_entrega,
 CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo,
 p.total,
 COALESCE((SELECT SUM(monto) FROM pagos WHERE id_pedido = p.id_pedido), 0) as pagado,
 (SELECT TOP 1 metodo_pago FROM pagos WHERE id_pedido = p.id_pedido ORDER BY fecha_pago DESC) as metodo_pago
 FROM pedidos p
 INNER JOIN clientes c ON p.id_cliente = c.id_cliente
 WHERE p.estado = 'Entregado'
 ORDER BY p.fecha_entrega DESC
 """;

 // Constantes
 private static final String TIPO_LOCAL = "Local";
 private static final String TIPO_DELIVERY = "Delivery";
 private static final String TIPO_TODOS = "Todos";

 // Estilos CSS
 private static final String BADGE_STYLE_BASE = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
 private static final String BADGE_LOCAL_STYLE = "-fx-background-color: #28A745;";
 private static final String BADGE_DELIVERY_STYLE = "-fx-background-color: #007BFF;";
 private static final String BADGE_DEFAULT_STYLE = "-fx-background-color: #666666;";
 private static final String BADGE_EFECTIVO_STYLE = "-fx-background-color: #6F42C1;";
 private static final String BADGE_TARJETA_STYLE = "-fx-background-color: #007BFF;";
 private static final String BADGE_TRANSFERENCIA_STYLE = "-fx-background-color: #17A2B8;";

 private static final String BUTTON_COBRAR_STYLE = "-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
 private static final String BUTTON_FACTURA_STYLE = "-fx-background-color: #6F42C1; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";
 private static final String BUTTON_WHATSAPP_STYLE = "-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;";

 // UI Components
 @FXML private Button actualizarButton;
 @FXML private TabPane tabPane;
 @FXML private TableView<PedidoPendiente> pedidosPendientesTable;
 @FXML private TableColumn<PedidoPendiente, Integer> pendienteIdColumn;
 @FXML private TableColumn<PedidoPendiente, String> pendienteClienteColumn;
 @FXML private TableColumn<PedidoPendiente, String> pendienteDireccionColumn;
 @FXML private TableColumn<PedidoPendiente, Double> pendienteTotalColumn;
 @FXML private TableColumn<PedidoPendiente, Double> pendienteAdelantoColumn;
 @FXML private TableColumn<PedidoPendiente, Double> pendienteSaldoColumn;
 @FXML private TableColumn<PedidoPendiente, String> pendienteTipoColumn;
 @FXML private TableColumn<PedidoPendiente, Void> pendienteAccionesColumn;
 @FXML private Label totalPendientesLabel;
 @FXML private DatePicker fechaDesdePicker;
 @FXML private DatePicker fechaHastaPicker;
 @FXML private ComboBox<String> tipoEntregaFilter;
 @FXML private TableView<EntregaHistorial> historialTable;
 @FXML private TableColumn<EntregaHistorial, Integer> historialIdColumn;
 @FXML private TableColumn<EntregaHistorial, String> historialClienteColumn;
 @FXML private TableColumn<EntregaHistorial, String> historialFechaColumn;
 @FXML private TableColumn<EntregaHistorial, String> historialTipoColumn;
 @FXML private TableColumn<EntregaHistorial, Double> historialTotalColumn;
 @FXML private TableColumn<EntregaHistorial, Double> historialPagadoColumn;
 @FXML private TableColumn<EntregaHistorial, String> historialMetodoColumn;
 @FXML private TableColumn<EntregaHistorial, Void> historialAccionesColumn;
 @FXML private Label totalHistorialLabel;

 // Services and Managers
 private SessionManager sessionManager;
 private DatabaseConnection dbConnection;
 private ObservableList<PedidoPendiente> pedidosPendientesList;
 private ObservableList<EntregaHistorial> historialList;

 @FXML
 public void initialize() {
 sessionManager = SessionManager.getInstance();
 dbConnection = DatabaseConnection.getInstance();

 if (!sessionManager.tienePermiso(Permiso.ENTREGAS_LEER)) {
 mostrarError("Acceso Denegado", "No tienes permiso para acceder a la gestión de entregas.");
 return;
 }

 initializeFilters();
 configurarTablaPendientes();
 configurarTablaHistorial();
 cargarPedidosPendientes();
 cargarHistorial();
 setupEventHandlers();
 }

 private void initializeFilters() {
 tipoEntregaFilter.getItems().addAll(TIPO_TODOS, TIPO_LOCAL, TIPO_DELIVERY);
 tipoEntregaFilter.getSelectionModel().selectFirst();
 }

 private void configurarTablaPendientes() {
 pendienteIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 pendienteClienteColumn.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
 pendienteDireccionColumn.setCellValueFactory(new PropertyValueFactory<>("direccion"));
 pendienteTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
 pendienteAdelantoColumn.setCellValueFactory(new PropertyValueFactory<>("adelanto"));
 pendienteSaldoColumn.setCellValueFactory(new PropertyValueFactory<>("saldo"));

 pendienteTipoColumn.setCellFactory(param -> new TableCell<PedidoPendiente, String>() {
 private final HBox hbox = new HBox(5);
 private final Label badge = new Label();

 {
 badge.setStyle(BADGE_STYLE_BASE);
 hbox.getChildren().setAll(badge);
 }

 @Override
 protected void updateItem(String tipo, boolean empty) {
 super.updateItem(tipo, empty);
 if (empty || tipo == null) {
 setGraphic(null);
 } else {
 String estilo = obtenerEstiloTipo(tipo);
 badge.setStyle(BADGE_STYLE_BASE + estilo);
 badge.setText(tipo.toUpperCase());
 setGraphic(hbox);
 }
 }
 });

 pendienteAccionesColumn.setCellFactory(param -> new TableCell<PedidoPendiente, Void>() {
 private final Button cobrarButton = new Button(" Cobrar Saldo");
 private final HBox hbox = new HBox(5);

 {
 cobrarButton.setStyle(BUTTON_COBRAR_STYLE);
 hbox.getChildren().setAll(cobrarButton);
 }

 @Override
 protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) {
 setGraphic(null);
 } else {
 PedidoPendiente pedido = getTableView().getItems().get(getIndex());
 cobrarButton.setOnAction(e -> cobrarSaldo(pedido));
 setGraphic(hbox);
 }
 }
 });
 }

 private void configurarTablaHistorial() {
 historialIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
 historialClienteColumn.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
 historialFechaColumn.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));

 historialTipoColumn.setCellFactory(param -> new TableCell<EntregaHistorial, String>() {
 private final HBox hbox = new HBox(5);
 private final Label badge = new Label();

 {
 badge.setStyle(BADGE_STYLE_BASE);
 hbox.getChildren().setAll(badge);
 }

 @Override
 protected void updateItem(String tipo, boolean empty) {
 super.updateItem(tipo, empty);
 if (empty || tipo == null) {
 setGraphic(null);
 } else {
 String estilo = obtenerEstiloTipo(tipo);
 badge.setStyle(BADGE_STYLE_BASE + estilo);
 badge.setText(tipo.toUpperCase());
 setGraphic(hbox);
 }
 }
 });

 historialTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
 historialPagadoColumn.setCellValueFactory(new PropertyValueFactory<>("pagado"));

 historialMetodoColumn.setCellFactory(param -> new TableCell<EntregaHistorial, String>() {
 private final HBox hbox = new HBox(5);
 private final Label badge = new Label();

 {
 badge.setStyle(BADGE_STYLE_BASE);
 hbox.getChildren().setAll(badge);
 }

 @Override
 protected void updateItem(String metodo, boolean empty) {
 super.updateItem(metodo, empty);
 if (empty || metodo == null) {
 setGraphic(null);
 } else {
 String estilo = obtenerEstiloMetodo(metodo);
 badge.setStyle(BADGE_STYLE_BASE + estilo);
 badge.setText(metodo.toUpperCase());
 setGraphic(hbox);
 }
 }
 });

 historialAccionesColumn.setCellFactory(param -> new TableCell<EntregaHistorial, Void>() {
 private final Button facturaButton = new Button(" Factura");
 private final Button whatsappButton = new Button(" WhatsApp");
 private final HBox hbox = new HBox(5);

 {
 facturaButton.setStyle(BUTTON_FACTURA_STYLE);
 whatsappButton.setStyle(BUTTON_WHATSAPP_STYLE);
 hbox.getChildren().setAll(facturaButton, whatsappButton);
 }

 @Override
 protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) {
 setGraphic(null);
 } else {
 EntregaHistorial entrega = getTableView().getItems().get(getIndex());
 facturaButton.setOnAction(e -> generarFactura(entrega));
 whatsappButton.setOnAction(e -> enviarWhatsApp(entrega));
 setGraphic(hbox);
 }
 }
 });
 }

 private String obtenerEstiloTipo(String tipo) {
 if (tipo == null) return BADGE_DEFAULT_STYLE;

 if (TIPO_LOCAL.equals(tipo)) {
 return BADGE_LOCAL_STYLE;
 } else if (TIPO_DELIVERY.equals(tipo)) {
 return BADGE_DELIVERY_STYLE;
 } else {
 return BADGE_DEFAULT_STYLE;
 }
 }

 private String obtenerEstiloMetodo(String metodo) {
 if (metodo == null) return BADGE_DEFAULT_STYLE;

 switch (metodo.toLowerCase()) {
 case "efectivo": return BADGE_EFECTIVO_STYLE;
 case "tarjeta": return BADGE_TARJETA_STYLE;
 case "transferencia": return BADGE_TRANSFERENCIA_STYLE;
 default: return BADGE_DEFAULT_STYLE;
 }
 }

 private void setupEventHandlers() {
 actualizarButton.setOnAction(this::actualizarDatos);

 fechaDesdePicker.setOnAction(event -> aplicarFiltrosHistorial());
 fechaHastaPicker.setOnAction(event -> aplicarFiltrosHistorial());
 tipoEntregaFilter.setOnAction(event -> aplicarFiltrosHistorial());
 }

 private void cargarPedidosPendientes() {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_PEDIDOS_PENDIENTES);
 ResultSet rs = stmt.executeQuery()) {

 pedidosPendientesList = FXCollections.observableArrayList();

 while (rs.next()) {
 pedidosPendientesList.add(new PedidoPendiente(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("direccion"),
 rs.getDouble("total"),
 rs.getDouble("adelanto"),
 rs.getDouble("saldo"),
 rs.getString("tipo")
 ));
 }

 pedidosPendientesTable.setItems(pedidosPendientesList);
 actualizarTotalPendientes();

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar pedidos pendientes: {0}", e.getMessage());
 mostrarError("Error de Base de Datos", "No se pudieron cargar los pedidos pendientes: " + e.getMessage());
 }
 }

 private void cargarHistorial() {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_HISTORIAL_ENTREGAS);
 ResultSet rs = stmt.executeQuery()) {

 historialList = FXCollections.observableArrayList();

 while (rs.next()) {
 historialList.add(new EntregaHistorial(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("fecha_entrega"),
 rs.getString("tipo"),
 rs.getDouble("total"),
 rs.getDouble("pagado"),
 rs.getString("metodo_pago") != null ? rs.getString("metodo_pago") : "Efectivo"
 ));
 }

 historialTable.setItems(historialList);
 actualizarTotalHistorial();

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar historial: {0}", e.getMessage());
 mostrarError("Error de Base de Datos", "No se pudo cargar el historial: " + e.getMessage());
 }
 }

 private void aplicarFiltrosHistorial() {
 StringBuilder sqlBuilder = new StringBuilder("""
 SELECT p.id_pedido, c.nombre + ' ' + c.apellido as nombre_cliente,
 CAST(p.fecha_entrega AS DATE) as fecha_entrega,
 CASE WHEN p.tipo_entrega = 'L' THEN 'Local' ELSE 'Delivery' END as tipo,
 p.total,
 COALESCE((SELECT SUM(monto) FROM pagos WHERE id_pedido = p.id_pedido), 0) as pagado,
 (SELECT TOP 1 metodo_pago FROM pagos WHERE id_pedido = p.id_pedido ORDER BY fecha_pago DESC) as metodo_pago
 FROM pedidos p
 INNER JOIN clientes c ON p.id_cliente = c.id_cliente
 WHERE p.estado = 'Entregado'
 """);

 java.util.ArrayList<String> condiciones = new java.util.ArrayList<>();
 java.util.ArrayList<Object> parametros = new java.util.ArrayList<>();

 if (fechaDesdePicker.getValue() != null) {
 condiciones.add("p.fecha_entrega >= ?");
 parametros.add(java.sql.Date.valueOf(fechaDesdePicker.getValue()));
 }
 if (fechaHastaPicker.getValue() != null) {
 condiciones.add("p.fecha_entrega <= ?");
 parametros.add(java.sql.Date.valueOf(fechaHastaPicker.getValue()));
 }

 String tipoSeleccionado = tipoEntregaFilter.getValue();
 if (!TIPO_TODOS.equals(tipoSeleccionado)) {
 condiciones.add("p.tipo_entrega = ?");
 parametros.add(TIPO_LOCAL.equals(tipoSeleccionado) ? "L" : "D");
 }

 if (!condiciones.isEmpty()) {
 sqlBuilder.append(" AND ").append(String.join(" AND ", condiciones));
 }

 sqlBuilder.append(" ORDER BY p.fecha_entrega DESC");

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

 for (int i = 0; i < parametros.size(); i++) {
 stmt.setObject(i + 1, parametros.get(i));
 }

 ObservableList<EntregaHistorial> resultados = FXCollections.observableArrayList();

 try (ResultSet rs = stmt.executeQuery()) {
 while (rs.next()) {
 resultados.add(new EntregaHistorial(
 rs.getInt("id_pedido"),
 rs.getString("nombre_cliente"),
 rs.getString("fecha_entrega"),
 rs.getString("tipo"),
 rs.getDouble("total"),
 rs.getDouble("pagado"),
 rs.getString("metodo_pago") != null ? rs.getString("metodo_pago") : "Efectivo"
 ));
 }
 }

 historialTable.setItems(resultados);
 actualizarTotalHistorial();

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al aplicar filtros: {0}", e.getMessage());
 mostrarError("Error de Filtros", "No se pudieron aplicar los filtros: " + e.getMessage());
 }
 }

 private void cobrarSaldo(PedidoPendiente pedido) {
 abrirModalEntrega(pedido);
 }

 private void abrirModalEntrega(PedidoPendiente pedido) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/EntregaModal.fxml"));
 Parent root = loader.load();

 EntregaModalController controller = loader.getController();
 controller.setPedido(pedido);

 Stage stage = new Stage();
 Scene scene = new Scene(root, 600, 500);
 stage.setScene(scene);
 stage.setTitle(" Registrar Entrega y Cobro");
 stage.initModality(Modality.APPLICATION_MODAL);
 stage.showAndWait();

 cargarPedidosPendientes();
 cargarHistorial();

 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al abrir modal de entrega: {0}", e.getMessage());
 mostrarError("Error", "No se pudo abrir la ventana de entrega: " + e.getMessage());
 }
 }

 private void generarFactura(EntregaHistorial entrega) {
 try {
 ReportService rs = new ReportService();
 java.util.Map<String, Object> params = new java.util.HashMap<>();
 params.put("ID_FACTURA", entrega.getId());
 net.sf.jasperreports.engine.JasperReport report = rs.compileReport("/reportes/Reporte_Factura.jrxml");
 net.sf.jasperreports.engine.JasperPrint print = rs.fillReport(report, params);
 rs.showReport(print);
 } catch (Exception e) {
 LOGGER.log(Level.SEVERE, "Error al generar factura: {0}", e.getMessage());
 Alert alert = new Alert(Alert.AlertType.ERROR);
 alert.setTitle("Error al generar factura");
 alert.setHeaderText("No se pudo generar la factura");
 alert.setContentText(e.getMessage() + "\n\nVerifique que la factura exista en la base de datos.");
 alert.showAndWait();
 }
 }

 private void enviarWhatsApp(EntregaHistorial entrega) {
 String mensaje = "Comprobante enviado por WhatsApp para pedido #" + entrega.getId() +
 "\n\nMensaje simulado: Su pedido ha sido entregado. Total: $" + 
 String.format("%.2f", entrega.getTotal());
 mostrarMensaje("Enviar WhatsApp", mensaje);
 }

 @FXML
 private void actualizarDatos(ActionEvent event) {
 cargarPedidosPendientes();
 cargarHistorial();
 mostrarMensaje("Datos Actualizados", "Los datos de entregas han sido actualizados correctamente.");
 }

 private void actualizarTotalPendientes() {
 totalPendientesLabel.setText("Total: " + pedidosPendientesList.size() + " pedidos");
 }

 private void actualizarTotalHistorial() {
 totalHistorialLabel.setText("Total: " + historialList.size() + " entregas");
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

 /**
 * CLASE TRADICIONAL para PedidoPendiente
 */
 public static class PedidoPendiente {
 private int id;
 private String nombreCliente;
 private String direccion;
 private double total;
 private double adelanto;
 private double saldo;
 private String tipo;

 public PedidoPendiente(int id, String nombreCliente, String direccion,
 double total, double adelanto, double saldo, String tipo) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.direccion = direccion;
 this.total = total;
 this.adelanto = adelanto;
 this.saldo = saldo;
 this.tipo = tipo;
 }

 public int getId() { return id; }
 public String getNombreCliente() { return nombreCliente; }
 public String getDireccion() { return direccion; }
 public double getTotal() { return total; }
 public double getAdelanto() { return adelanto; }
 public double getSaldo() { return saldo; }
 public String getTipo() { return tipo; }

 public boolean esLocal() { return TIPO_LOCAL.equals(tipo); }
 public boolean esDelivery() { return TIPO_DELIVERY.equals(tipo); }
 public boolean tieneSaldo() { return saldo > 0; }
 }

 /**
 * CLASE TRADICIONAL para EntregaHistorial
 */
 public static class EntregaHistorial {
 private int id;
 private String nombreCliente;
 private String fechaEntrega;
 private String tipo;
 private double total;
 private double pagado;
 private String metodoPago;

 public EntregaHistorial(int id, String nombreCliente, String fechaEntrega,
 String tipo, double total, double pagado, String metodoPago) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.fechaEntrega = fechaEntrega;
 this.tipo = tipo;
 this.total = total;
 this.pagado = pagado;
 this.metodoPago = metodoPago;
 }

 public int getId() { return id; }
 public String getNombreCliente() { return nombreCliente; }
 public String getFechaEntrega() { return fechaEntrega; }
 public String getTipo() { return tipo; }
 public double getTotal() { return total; }
 public double getPagado() { return pagado; }
 public String getMetodoPago() { return metodoPago; }

 public boolean estaPagadoCompleto() { return pagado >= total; }
 public double getSaldoPendiente() { return total - pagado; }
 public boolean esLocal() { return TIPO_LOCAL.equals(tipo); }
 public boolean esDelivery() { return TIPO_DELIVERY.equals(tipo); }
 }
}
