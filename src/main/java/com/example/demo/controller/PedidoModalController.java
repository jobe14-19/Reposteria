package com.example.demo.controller;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import com.example.demo.dao.OrdenProduccionDAO;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.SpinnerValueFactory;

public class PedidoModalController {

 private static final Logger LOGGER = Logger.getLogger(PedidoModalController.class.getName());

 // Constantes SQL (sin Text Blocks, con GETDATE() para SQL Server)
 private static final String SQL_CARGAR_CLIENTES =
 "SELECT id_cliente, nombre + ' ' + apellido as nombre_completo FROM clientes ORDER BY nombre, apellido";

 private static final String SQL_OBTENER_ID_CLIENTE =
 "SELECT id_cliente FROM clientes WHERE nombre + ' ' + apellido = ?";

  private static final String SQL_CARGAR_PRODUCTOS =
  "SELECT nombre FROM productos WHERE estado = 'Activo' ORDER BY nombre";

  private static final String SQL_OBTENER_ID_PRODUCTO =
  "SELECT id_producto FROM productos WHERE nombre = ?";

 private static final String SQL_OBTENER_PRECIOS_PRODUCTO =
 "SELECT precio_base, costo_disenio FROM productos WHERE nombre = ?";

 private static final String SQL_ACTUALIZAR_PEDIDO =
 "UPDATE pedidos SET id_cliente = ?, fecha_entrega = ?, id_producto = ?, libras = ?, diseno = ?, total = ?, adelanto = ?, observaciones = ? WHERE id_pedido = ?";

 private static final String SQL_INSERTAR_PEDIDO =
 "INSERT INTO pedidos (id_cliente, fecha_pedido, fecha_entrega, id_producto, libras, diseno, total, adelanto, observaciones, estado) VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, 'Pendiente')";

 private static final String SQL_INSERTAR_PAGO =
 "INSERT INTO pagos (id_pedido, monto, fecha_pago, estado) VALUES (?, ?, GETDATE(), 'Pagado')";

 private static final String SQL_REGISTRAR_ACTIVIDAD =
 "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

 // Constantes
 private static final int HORAS_MINIMAS_ENTREGA = 3;
 private static final LocalTime HORA_ENTREGA_DEFAULT = LocalTime.of(12, 0);
 private static final String PRODUCTO_FRESA = "Fresa";
 private static final String TITULO_NUEVO = " Nuevo Pedido";
 private static final String TITULO_EDITAR = " Editar Pedido";
 private static final String FORMATO_MONEDA = "$%.2f";

 private static final String DISABLED_STYLE = "-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-width: 1;";
 private static final String ENABLED_STYLE = "-fx-background-color: #F0F0F0; -fx-border-color: #E0E0E0; -fx-border-width: 1;";
 private static final String DEFAULT_STYLE = "-fx-border-color: #E0E0E0; -fx-border-width: 1;";

 // Tipos de bizcocho predefinidos
 private static final String[] TIPOS_BIZCOCHO = {"Vainilla", "Chocolate", "Fresa", "Zanahoria", "Cheesecake", "Ron"};

 // UI Components
 @FXML private Label tituloLabel;
 @FXML private ComboBox<String> clienteComboBox;
 @FXML private DatePicker fechaEntregaPicker;
 @FXML private ComboBox<String> tipoBizcochoComboBox;
 @FXML private Spinner<Double> librasSpinner;
 @FXML private TextArea disenoTextArea;
 @FXML private CheckBox disenoComplejoCheckBox;
 @FXML private TextField totalField;
 @FXML private TextField adelantoField;
 @FXML private TextArea observacionesTextArea;
 @FXML private TextField precioBaseField;
 @FXML private TextField costoDiseñoField;
 @FXML private TextField totalCalculadoField;
 @FXML private Button guardarResultado;
 @FXML private Button cancelarButton;

 // Services and Managers
 private DatabaseConnection dbConnection;
 private SessionManager sessionManager;
 private Pedido pedidoActual;
 private boolean esEdicion = false;
 private int idPedidoEdicion = -1;

 @FXML
 public void initialize() {
 dbConnection = DatabaseConnection.getInstance();
 sessionManager = SessionManager.getInstance();

 initializeCombos();
 setupFieldValidation();
 setupDesignCheckboxListener();
 setupSpinnerFactory();
 }

  private void initializeCombos() {
  cargarClientes();
  cargarProductos();
  }

  private void cargarProductos() {
  tipoBizcochoComboBox.getItems().clear();
  try (Connection conn = dbConnection.getConnection();
  PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_PRODUCTOS);
  ResultSet rs = stmt.executeQuery()) {
  while (rs.next()) {
  tipoBizcochoComboBox.getItems().add(rs.getString("nombre"));
  }
  } catch (SQLException e) {
  LOGGER.log(Level.WARNING, "Error al cargar productos, usando lista por defecto: {0}", e.getMessage());
  }
  if (tipoBizcochoComboBox.getItems().isEmpty()) {
  for (String tipo : TIPOS_BIZCOCHO) {
  tipoBizcochoComboBox.getItems().add(tipo);
  }
  }
  tipoBizcochoComboBox.getSelectionModel().selectFirst();
  }

  private void cargarClientes() {
 clienteComboBox.getItems().clear();

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_CARGAR_CLIENTES);
 ResultSet rs = stmt.executeQuery()) {

 while (rs.next()) {
 clienteComboBox.getItems().add(rs.getString("nombre_completo"));
 }

 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al cargar clientes: {0}", e.getMessage());
 }
 }

 private void setupSpinnerFactory() {
 librasSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 50.0, 1.0, 0.5));
 librasSpinner.setEditable(true);
 }

 private void setupFieldValidation() {
 clienteComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 fechaEntregaPicker.valueProperty().addListener((obs, oldVal, newVal) -> validarCampos());
 tipoBizcochoComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
 calcularTotal();
 validarCampos();
 });
 librasSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
 calcularTotal();
 validarCampos();
 });
 disenoComplejoCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
 calcularTotal();
 validarCampos();
 });
 }

 private void setupDesignCheckboxListener() {
 disenoComplejoCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
 if (newVal) {
 disenoTextArea.setDisable(false);
 disenoTextArea.setStyle(ENABLED_STYLE);
 } else {
 disenoTextArea.setDisable(true);
 disenoTextArea.clear();
 disenoTextArea.setStyle(DISABLED_STYLE);
 }
 });
 }

 public void setPedido(Pedido pedido) {
 this.pedidoActual = pedido;

 if (pedido != null) {
 esEdicion = true;
 tituloLabel.setText(TITULO_EDITAR);
 cargarDatosPedido(pedido);
 guardarResultado.setText("Actualizar");
 } else {
 esEdicion = false;
 tituloLabel.setText(TITULO_NUEVO);
 limpiarCampos();
 guardarResultado.setText("Guardar");
 }
 }

 private void cargarDatosPedido(Pedido pedido) {
 clienteComboBox.getSelectionModel().select(pedido.getNombreCliente());

 if (pedido.getFechaEntrega() != null) {
 fechaEntregaPicker.setValue(LocalDate.parse(pedido.getFechaEntrega()));
 }

 tipoBizcochoComboBox.getSelectionModel().select(pedido.getProducto());
 librasSpinner.getValueFactory().setValue(pedido.getLibras());

 boolean tieneDiseno = pedido.getDiseno() != null && !pedido.getDiseno().trim().isEmpty();
 disenoTextArea.setText(pedido.getDiseno());
 disenoComplejoCheckBox.setSelected(tieneDiseno);

 totalField.setText(String.format(FORMATO_MONEDA, pedido.getTotal()));
 adelantoField.setText(String.format(FORMATO_MONEDA, pedido.getAdelanto()));
 observacionesTextArea.setText(pedido.getObservaciones());
 idPedidoEdicion = pedido.getId();
 }

 @FXML
 private void guardarResultado(ActionEvent event) {
 if (!sonCamposValidos()) {
 mostrarError("Campos Requeridos", "Por favor complete todos los campos obligatorios (*).");
 return;
 }

 if (!esFechaEntregaValida()) {
 mostrarError("Fecha Inválida",
 String.format("La fecha de entrega debe ser al menos %d horas en el futuro.", HORAS_MINIMAS_ENTREGA));
 return;
 }

 if (!esProductoFresaValido()) {
 mostrarError("Producto con Fresas", "Los productos con fresas requieren diseño obligatorio.");
 return;
 }

 try (Connection conn = dbConnection.getConnection()) {
 if (esEdicion) {
 actualizarPedido(conn);
 } else {
 insertarPedido(conn);
 }
 } catch (SQLException e) {
 LOGGER.log(Level.SEVERE, "Error al procesar pedido: {0}", e.getMessage());
 mostrarError("Error de Base de Datos", "No se pudo procesar el pedido: " + e.getMessage());
 }
 }

 private void actualizarPedido(Connection conn) throws SQLException {
 int idCliente = obtenerIdCliente(clienteComboBox.getSelectionModel().getSelectedItem());
 int idProducto = obtenerIdProducto(tipoBizcochoComboBox.getSelectionModel().getSelectedItem());

 try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_PEDIDO)) {
 stmt.setInt(1, idCliente);
 stmt.setDate(2, java.sql.Date.valueOf(fechaEntregaPicker.getValue()));
 stmt.setInt(3, idProducto);
 stmt.setDouble(4, librasSpinner.getValue());
 stmt.setString(5, obtenerDiseno());
 stmt.setDouble(6, obtenerTotal());
 stmt.setDouble(7, obtenerAdelanto());
 stmt.setString(8, observacionesTextArea.getText());
 stmt.setInt(9, pedidoActual.getId());

 if (stmt.executeUpdate() > 0) {
 registrarActividad("ACTUALIZAR PEDIDO", "Pedido actualizado: #" + pedidoActual.getId());
 registrarPagoSiCorresponde(conn, pedidoActual.getId());
 mostrarMensaje("Pedido Actualizado", "El pedido ha sido actualizado correctamente.");
 cerrarModal();
 } else {
 mostrarError("Error al Actualizar", "No se pudo actualizar el pedido.");
 }
 }
 }

 private void insertarPedido(Connection conn) throws SQLException {
 int idCliente = obtenerIdCliente(clienteComboBox.getSelectionModel().getSelectedItem());
 int idProducto = obtenerIdProducto(tipoBizcochoComboBox.getSelectionModel().getSelectedItem());

 try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PEDIDO, PreparedStatement.RETURN_GENERATED_KEYS)) {
 stmt.setInt(1, idCliente);
 stmt.setDate(2, java.sql.Date.valueOf(fechaEntregaPicker.getValue()));
 stmt.setInt(3, idProducto);
 stmt.setDouble(4, librasSpinner.getValue());
 stmt.setString(5, obtenerDiseno());
 stmt.setDouble(6, obtenerTotal());
 stmt.setDouble(7, obtenerAdelanto());
 stmt.setString(8, observacionesTextArea.getText());

 if (stmt.executeUpdate() > 0) {
 try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
 if (generatedKeys.next()) {
  int idPedido = generatedKeys.getInt(1);
  registrarActividad("CREAR PEDIDO", "Nuevo pedido creado: #" + idPedido);
  registrarPagoSiCorresponde(conn, idPedido);
  crearOrdenProduccionDesdePedido();
  mostrarMensaje("Pedido Creado", "El pedido ha sido creado correctamente y se ha generado una orden de producción.");
  cerrarModal();
 }
 }
 } else {
 mostrarError("Error al Crear", "No se pudo crear el pedido.");
 }
 }
 }

 @FXML
 private void cancelarResultado(ActionEvent event) {
 cerrarModal();
 }

 @FXML
 private void eliminar(ActionEvent event) {
 if (!esEdicion || idPedidoEdicion <= 0) return;

 Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
 confirm.setTitle("Confirmar Eliminación");
 confirm.setHeaderText("¿Está seguro de eliminar este pedido?");
 confirm.setContentText("Esta acción no se puede deshacer.");

 if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
 if (dbConnection != null) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement("UPDATE pedidos SET estado = 'Cancelado' WHERE id_pedido = ?")) {
 stmt.setInt(1, idPedidoEdicion);
 stmt.executeUpdate();
 } catch (SQLException e) {
 LOGGER.log(Level.INFO, "Modo offline: simulando eliminación de pedido");
 }
 }
 mostrarMensaje("Pedido Eliminado", "El pedido ha sido eliminado correctamente.");
 cerrarModal();
 }
 }

 private boolean sonCamposValidos() {
 return clienteComboBox.getSelectionModel().getSelectedItem() != null &&
 fechaEntregaPicker.getValue() != null &&
 tipoBizcochoComboBox.getSelectionModel().getSelectedItem() != null &&
 librasSpinner.getValue() != null &&
 librasSpinner.getValue() > 0;
 }

 private boolean esFechaEntregaValida() {
 if (fechaEntregaPicker.getValue() == null) {
 return false;
 }

 LocalDate fechaEntrega = fechaEntregaPicker.getValue();
 LocalDateTime ahora = LocalDateTime.now();
 LocalDateTime fechaEntregaDateTime = fechaEntrega.atTime(HORA_ENTREGA_DEFAULT);

 return fechaEntregaDateTime.isAfter(ahora.plusHours(HORAS_MINIMAS_ENTREGA));
 }

 private boolean esProductoFresaValido() {
 String producto = tipoBizcochoComboBox.getSelectionModel().getSelectedItem();
 return !PRODUCTO_FRESA.equals(producto) || disenoComplejoCheckBox.isSelected();
 }

 private int obtenerIdCliente(String nombreCompleto) {
 if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
 return 0;
 }

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_CLIENTE)) {

 stmt.setString(1, nombreCompleto);

 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() ? rs.getInt("id_cliente") : 0;
 }

 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al obtener ID de cliente: {0}", e.getMessage());
 return 0;
 }
 }

 private int obtenerIdProducto(String producto) {
 if (producto == null || producto.trim().isEmpty()) {
 return 0;
 }

 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_PRODUCTO)) {

 stmt.setString(1, producto);

 try (ResultSet rs = stmt.executeQuery()) {
 return rs.next() ? rs.getInt("id_producto") : 0;
 }

 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al obtener ID de producto: {0}", e.getMessage());
 return 0;
 }
 }

 private void calcularTotal() {
 String producto = tipoBizcochoComboBox.getSelectionModel().getSelectedItem();
 Double libras = librasSpinner.getValue();

 if (producto != null && libras != null && libras > 0) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PRECIOS_PRODUCTO)) {

 stmt.setString(1, producto);

 try (ResultSet rs = stmt.executeQuery()) {
 if (rs.next()) {
 double precioBase = rs.getDouble("precio_base");
 double costoDiseño = rs.getDouble("costo_disenio");
 double total = (precioBase * libras) + (disenoComplejoCheckBox.isSelected() ? costoDiseño : 0);
 totalField.setText(String.format(FORMATO_MONEDA, total));
 precioBaseField.setText(String.format("%.2f", precioBase));
 costoDiseñoField.setText(String.format("%.2f", costoDiseño));
 totalCalculadoField.setText(String.format(FORMATO_MONEDA, total));
 adelantoField.setText(String.format(FORMATO_MONEDA, total * 0.5));
 }
 }

 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al calcular total: {0}", e.getMessage());
 }
 }
 }

 private double obtenerTotal() {
 try {
 String totalText = totalField.getText();
 return (totalText != null && !totalText.trim().isEmpty())
 ? Double.parseDouble(totalText.replace("$", "").trim())
 : 0.0;
 } catch (NumberFormatException e) {
 return 0.0;
 }
 }

 private double obtenerAdelanto() {
 try {
 String adelantoText = adelantoField.getText();
 return (adelantoText != null && !adelantoText.trim().isEmpty())
 ? Double.parseDouble(adelantoText.replace("$", "").trim())
 : 0.0;
 } catch (NumberFormatException e) {
 return 0.0;
 }
 }

 private String obtenerDiseno() {
 return disenoComplejoCheckBox.isSelected() ? disenoTextArea.getText() : null;
 }

  private void crearOrdenProduccionDesdePedido() {
   try {
    OrdenProduccion orden = new OrdenProduccion();
    orden.setCliente(clienteComboBox.getSelectionModel().getSelectedItem());
    orden.setCategoria(tipoBizcochoComboBox.getSelectionModel().getSelectedItem());
    orden.setLibras(librasSpinner.getValue());
    orden.setFechaEntrega(fechaEntregaPicker.getValue() != null ? fechaEntregaPicker.getValue().toString() : null);
    orden.setDecoracion(obtenerDiseno());
    orden.setPrecioVenta(obtenerTotal());
    orden.setAnticipo(obtenerAdelanto());
    orden.setSaldo(obtenerTotal() - obtenerAdelanto());
    orden.setObservaciones(observacionesTextArea.getText());
    orden.setUsuarioCrea(sessionManager.getUsuarioActual());
    new OrdenProduccionDAO().insertar(orden);
   } catch (Exception e) {
    LOGGER.log(Level.WARNING, "No se pudo generar orden de producción automática: {0}", e.getMessage());
   }
  }

  private void registrarPagoSiCorresponde(Connection conn, int idPedido) throws SQLException {
 double adelanto = obtenerAdelanto();
 if (adelanto > 0) {
 try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PAGO)) {
 stmt.setInt(1, idPedido);
 stmt.setDouble(2, adelanto);
 stmt.executeUpdate();
 }
 }
 }

 private void registrarActividad(String accion, String detalle) {
 try (Connection conn = dbConnection.getConnection();
 PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {

 stmt.setString(1, sessionManager.getUsuarioActual());
 stmt.setString(2, accion);
 stmt.setString(3, detalle);
 stmt.executeUpdate();

 } catch (SQLException e) {
 LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
 }
 }

 @FXML
 private void limpiarCampos() {
 clienteComboBox.getSelectionModel().clearSelection();
 fechaEntregaPicker.setValue(null);
 tipoBizcochoComboBox.getSelectionModel().selectFirst();
 librasSpinner.getValueFactory().setValue(1.0);
 disenoTextArea.clear();
 disenoComplejoCheckBox.setSelected(false);
 totalField.clear();
 adelantoField.clear();
 observacionesTextArea.clear();

 clienteComboBox.setStyle(DEFAULT_STYLE);
 fechaEntregaPicker.setStyle(DEFAULT_STYLE);
 tipoBizcochoComboBox.setStyle(DEFAULT_STYLE);
 librasSpinner.setStyle(DEFAULT_STYLE);
 disenoTextArea.setStyle(DISABLED_STYLE);
 totalField.setStyle(DEFAULT_STYLE);
 adelantoField.setStyle(DEFAULT_STYLE);
 observacionesTextArea.setStyle(DEFAULT_STYLE);
 }

 private void validarCampos() {
 boolean camposValidos = sonCamposValidos() && esFechaEntregaValida() && esProductoFresaValido();
 guardarResultado.setDisable(!camposValidos);
 }

 private void cerrarModal() {
 Stage stage = (Stage) guardarResultado.getScene().getWindow();
 stage.close();
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
 * CLASE TRADICIONAL para Pedido (Java 8 compatible)
 */
 public static class Pedido {
 private int id;
 private String nombreCliente;
 private String fechaEntrega;
 private String producto;
 private double libras;
 private String diseno;
 private double total;
 private double adelanto;
 private String observaciones;

 public Pedido(int id, String nombreCliente, String fechaEntrega, String producto,
 double libras, String diseno, double total, double adelanto, String observaciones) {
 this.id = id;
 this.nombreCliente = nombreCliente;
 this.fechaEntrega = fechaEntrega;
 this.producto = producto;
 this.libras = libras;
 this.diseno = diseno;
 this.total = total;
 this.adelanto = adelanto;
 this.observaciones = observaciones;
 }

 public int getId() { return id; }
 public String getNombreCliente() { return nombreCliente; }
 public String getFechaEntrega() { return fechaEntrega; }
 public String getProducto() { return producto; }
 public double getLibras() { return libras; }
 public String getDiseno() { return diseno; }
 public double getTotal() { return total; }
 public double getAdelanto() { return adelanto; }
 public String getObservaciones() { return observaciones; }

 public boolean tieneDiseno() { return diseno != null && !diseno.trim().isEmpty(); }
 public double getSaldoPendiente() { return total - adelanto; }
 public boolean tieneSaldoPendiente() { return getSaldoPendiente() > 0; }
 }
}
