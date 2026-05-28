package com.example.demo.controller;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.Pago;
import com.example.demo.service.PayPalConfig;
import com.example.demo.service.PayPalService;
import com.example.demo.service.PreciosConfig;
import com.example.demo.service.SessionManager;
import com.example.demo.util.DatabaseConnection;
import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.dao.PagoDAO;
import com.example.demo.dao.PedidoDAO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.SpinnerValueFactory;

public class ClientePedidoFormController {

    private static final Logger LOGGER = Logger.getLogger(ClientePedidoFormController.class.getName());

    private static final String SQL_INSERTAR_PEDIDO =
        "INSERT INTO pedidos (id_cliente, username, fecha_pedido, fecha_entrega, producto, libras, diseno, total, adelanto, observaciones, estado, tipo_pago, estado_pago) VALUES (?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, 'Pendiente', ?, ?)";
    private static final String SQL_OBTENER_ID_CLIENTE =
        "SELECT id_cliente FROM clientes WHERE usuario = ?";
    private static final String SQL_REGISTRAR_ACTIVIDAD =
        "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    private static final String[] HORAS = {"08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00"};
    private static final String[] CATEGORIAS = {"Vainilla","Chocolate","Fresa","Zanahoria","Cheesecake","Ron","Red Velvet","Tres Leches"};
    private static final String[] BASES = {"Bizcocho","Galleta","Brownie","Base Crujiente","Sin Base"};
    private static final String[] MASAS = {"Tradicional","Esponjosa","Hojaldrada","Genovesa","Queque"};
    private static final String[] FORMAS = {"Redonda","Cuadrada","Rectangular","Corazon","Hexagonal","Personalizada"};
    private static final String[] TIPOS_PAGO = {"Efectivo","Tarjeta de Credito","Tarjeta de Debito","Cheque","Transferencia","PayPal"};
    private static final String[] ESTADOS_PAGO = {"Pendiente","PAGADO","PAGADO_PARCIAL","Reembolsado"};

    @FXML private TextField clienteField;
    @FXML private TextField telefonoField;
    @FXML private TextField direccionField;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<String> horaCombo;
    @FXML private ComboBox<String> categoriaCombo;
    @FXML private ComboBox<String> baseCombo;
    @FXML private ComboBox<String> masaCombo;
    @FXML private ComboBox<String> formaCombo;
    @FXML private Spinner<Integer> pisosSpinner;
    @FXML private Spinner<Double> librasSpinner;
    @FXML private TextArea decoracionArea;
    @FXML private TextField lustresField;
    @FXML private TextField camuflajesField;
    @FXML private TextField floresField;
    @FXML private TextField adornosField;
    @FXML private TextField rellenosField;
    @FXML private TextField mensajeField;
    @FXML private TextArea observacionesArea;
    @FXML private ComboBox<String> tipoPagoCombo;
    @FXML private ComboBox<String> estadoPagoCombo;
    @FXML private Button guardarBtn;
    @FXML private Label totalLabel;
    @FXML private Label desgloseLabel;
    @FXML private CheckBox pagoInmediatoCheck;

    private SessionManager session;
    private DatabaseConnection dbConnection;

    @FXML
    public void initialize() {
        session = SessionManager.getInstance();
        dbConnection = DatabaseConnection.getInstance();

        clienteField.setText(session.getUsuarioActual());

        horaCombo.getItems().addAll(HORAS);
        horaCombo.getSelectionModel().select("12:00");

        categoriaCombo.getItems().addAll(CATEGORIAS);
        baseCombo.getItems().addAll(BASES);
        masaCombo.getItems().addAll(MASAS);
        formaCombo.getItems().addAll(FORMAS);

        pisosSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1));
        librasSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 50.0, 1.0, 0.5));
        librasSpinner.setEditable(true);

        fechaPicker.setValue(LocalDate.now().plusDays(1));

        tipoPagoCombo.getItems().addAll(TIPOS_PAGO);
        tipoPagoCombo.getSelectionModel().select("Efectivo");
        estadoPagoCombo.getItems().addAll(ESTADOS_PAGO);
        estadoPagoCombo.getSelectionModel().select("Pendiente");

        pagoInmediatoCheck.setVisible(PayPalConfig.isConfigured());
        pagoInmediatoCheck.setManaged(PayPalConfig.isConfigured());
        pagoInmediatoCheck.setSelected(false);

        setupValidation();
        setupPriceListeners();
        actualizarTotalLabel();
    }

    private void setupValidation() {
        categoriaCombo.valueProperty().addListener((obs, o, n) -> validar());
        fechaPicker.valueProperty().addListener((obs, o, n) -> validar());
        librasSpinner.valueProperty().addListener((obs, o, n) -> validar());
    }

    private void setupPriceListeners() {
        categoriaCombo.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        baseCombo.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        masaCombo.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        formaCombo.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        pisosSpinner.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        librasSpinner.valueProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        lustresField.textProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        camuflajesField.textProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        floresField.textProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        adornosField.textProperty().addListener((obs, o, n) -> actualizarTotalLabel());
        rellenosField.textProperty().addListener((obs, o, n) -> actualizarTotalLabel());
    }

    private void actualizarTotalLabel() {
        String cat = categoriaCombo.getValue();
        String base = baseCombo.getValue();
        String masa = masaCombo.getValue();
        String forma = formaCombo.getValue();
        int pisos = pisosSpinner.getValue() != null ? pisosSpinner.getValue() : 1;
        double libras = librasSpinner.getValue() != null ? librasSpinner.getValue() : 0;

        double total = PreciosConfig.calcularTotal(cat, base, masa, forma, pisos, libras,
            lustresField.getText(), camuflajesField.getText(), floresField.getText(),
            adornosField.getText(), rellenosField.getText());

        if (total <= 0) {
            totalLabel.setText("RD$0.00");
            desgloseLabel.setText("Selecciona una categoria y libras para ver el desglose.");
            return;
        }

        totalLabel.setText(String.format("RD$%.2f", total));

        StringBuilder sb = new StringBuilder();
        for (PreciosConfig.DesgloseItem item : PreciosConfig.calcularDesglose(cat, base, masa, forma, pisos, libras,
            lustresField.getText(), camuflajesField.getText(), floresField.getText(),
            adornosField.getText(), rellenosField.getText())) {
            sb.append(String.format("  %s ................... $%.2f\n", item.concepto, item.valor));
        }
        desgloseLabel.setText(sb.toString());
    }

    private void validar() {
        boolean ok = categoriaCombo.getValue() != null
            && fechaPicker.getValue() != null
            && librasSpinner.getValue() != null
            && librasSpinner.getValue() > 0;
        guardarBtn.setDisable(!ok);
    }

    @FXML
    private void guardar(ActionEvent event) {
        if (!camposValidos()) return;

        String categoria = categoriaCombo.getValue();
        double libras = librasSpinner.getValue();
        double total = recalcularPrecio();
        double anticipo = 0.0;
        boolean pagoInmediato = pagoInmediatoCheck.isSelected();

        if (pagoInmediato) {
            totalLabel.setText(String.format("RD$%.2f", total));
            tipoPagoCombo.setValue("PayPal");
        }

        String username = session.getUsuarioActual();

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idCliente = obtenerIdCliente(conn);
                int idPedido = insertarPedido(conn, idCliente, username, categoria, libras, null, total, anticipo);

                if (idPedido <= 0) {
                    conn.rollback();
                    mostrarError("Error", "No se pudo crear el pedido.");
                    return;
                }

                crearOrdenProduccion(conn, idPedido, username, total);
                registrarActividad(conn, "CREAR PEDIDO", "Nuevo pedido creado por cliente: #" + idPedido);
                conn.commit();

                if (pagoInmediato) {
                    procesarPagoPayPal(idPedido, total, username);
                } else {
                    mostrarMensaje("Pedido Creado", "Tu pedido se ha registrado correctamente.");
                    cerrar();
                }
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error al guardar pedido", e);
                mostrarError("Error", "No se pudo crear el pedido: " + e.getMessage());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error de BD al guardar pedido: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo procesar el pedido: " + e.getMessage());
        }
    }

    private double recalcularPrecio() {
        String cat = categoriaCombo.getValue();
        String base = baseCombo.getValue();
        String masa = masaCombo.getValue();
        String forma = formaCombo.getValue();
        int pisos = pisosSpinner.getValue() != null ? pisosSpinner.getValue() : 1;
        double libras = librasSpinner.getValue() != null ? librasSpinner.getValue() : 0;
        if (cat == null || libras <= 0) return 0.0;
        return PreciosConfig.calcularTotal(cat, base, masa, forma, pisos, libras,
            lustresField.getText(), camuflajesField.getText(), floresField.getText(),
            adornosField.getText(), rellenosField.getText());
    }

    private void procesarPagoPayPal(int idPedido, double total, String username) {
        guardarBtn.setDisable(true);
        new Thread(() -> {
            try {
                PayPalService paypal = new PayPalService();
                PayPalService.PayPalCheckoutResult result = paypal.crearCheckoutSession(total,
                    "Pedido #" + idPedido + " - Pastel Personalizado", null, idPedido);

                if (!result.ok) {
                    Platform.runLater(() -> {
                        mostrarMensaje("Pedido Creado, Pago no disponible",
                            "El pedido #" + idPedido + " se creo correctamente, pero no se pudo iniciar el pago con PayPal:\n"
                            + result.url + "\n\nPuedes pagar mas tarde desde tu perfil.");
                        cerrar();
                    });
                    return;
                }

                Platform.runLater(() -> {
                    mostrarMensaje("Redirigiendo a PayPal",
                        "Se abrira el navegador para completar el pago.\nEspera mientras confirmamos el pago...");
                });

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(result.url));
                } else {
                    Platform.runLater(() -> {
                        mostrarMensaje("Abrir navegador",
                            "Abre este enlace en tu navegador:\n" + result.url);
                    });
                }

                boolean confirmado = false;
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(3000);
                    if (paypal.verificarPago(result.sessionId)) {
                        new PedidoDAO().actualizarAdelantoYEstadoPago(idPedido, total, "PAGADO");
                        new OrdenProduccionDAO().actualizarPagoPorIdPedido(idPedido, total, "PAGADO");
                        Pago pago = new Pago(0, idPedido, total, null, "PayPal", "PayPal Checkout", "Pagado");
                        new PagoDAO().insertar(pago);
                        confirmado = true;
                        Platform.runLater(() -> {
                            mostrarMensaje("Pago Exitoso",
                                "Pago de $" + String.format("%.2f", total) + " confirmado.\nPedido #" + idPedido + " creado correctamente.");
                            cerrar();
                        });
                        break;
                    }
                }

                if (!confirmado) {
                    Platform.runLater(() -> {
                        mostrarMensaje("Pago Pendiente",
                            "Tu pedido #" + idPedido + " se creo pero el pago no se confirmo en el tiempo esperado.\nPuedes pagar desde tu perfil mas tarde.");
                        cerrar();
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en pago PayPal: {0}", e.getMessage());
                Platform.runLater(() -> {
                    mostrarError("Pedido Creado, Error de Pago",
                        "El pedido #" + idPedido + " se creo correctamente, pero ocurrio un error al procesar el pago:\n" + e.getMessage()
                        + "\n\nPuedes pagar mas tarde desde tu perfil.");
                    cerrar();
                });
            }
        }).start();
    }

    private int insertarPedido(Connection conn, int idCliente, String username, String categoria,
                               double libras, String diseno, double total, double anticipo) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PEDIDO, PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (idCliente > 0) stmt.setInt(1, idCliente);
            else stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setString(2, username);
            stmt.setDate(3, java.sql.Date.valueOf(fechaPicker.getValue()));
            stmt.setString(4, categoria);
            stmt.setDouble(5, libras);
            stmt.setString(6, diseno != null && !diseno.isEmpty() ? diseno : null);
            stmt.setDouble(7, total);
            stmt.setDouble(8, anticipo);
            stmt.setString(9, observacionesArea.getText());
            stmt.setString(10, tipoPagoCombo.getValue());
            stmt.setString(11, estadoPagoCombo.getValue());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private void crearOrdenProduccion(Connection conn, int idPedido, String username, double total) {
        try {
            OrdenProduccion orden = new OrdenProduccion();
            orden.setCliente(clienteField.getText());
            orden.setTelefono(telefonoField.getText());
            orden.setDireccion(direccionField.getText());
            orden.setCategoria(categoriaCombo.getValue());
            orden.setLibras(librasSpinner.getValue());
            orden.setFechaEntrega(fechaPicker.getValue().toString());
            orden.setHoraEntrega(horaCombo.getValue());
            orden.setBaseTipo(baseCombo.getValue());
            orden.setMaso(masaCombo.getValue());
            orden.setForma(formaCombo.getValue());
            orden.setPisos(pisosSpinner.getValue());
            orden.setDecoracion(decoracionArea.getText());
            orden.setLustres(lustresField.getText());
            orden.setCamuflajes(camuflajesField.getText());
            orden.setFlores(floresField.getText());
            orden.setAdornos(adornosField.getText());
            orden.setRellenos(rellenosField.getText());
            orden.setMensaje(mensajeField.getText());
            orden.setObservaciones(observacionesArea.getText());
            orden.setPrecioVenta(total);
            orden.setAnticipo(0.0);
            orden.setSaldo(total);
            orden.setUsuarioCrea(username);
            orden.setProgreso(0);
            orden.setPausado(false);
            orden.setTipoPago(tipoPagoCombo.getValue());
            orden.setEstadoPago(estadoPagoCombo.getValue());
            orden.setIdPedido(idPedido);
            int idOrden = new OrdenProduccionDAO().insertarEnTransaccion(conn, orden);
            if (idOrden <= 0) {
                throw new SQLException("insertarEnTransaccion devolvio " + idOrden);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar orden de produccion", e);
            throw new RuntimeException("Error al crear orden de produccion", e);
        }
    }

    private int obtenerIdCliente(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_ID_CLIENTE)) {
            stmt.setString(1, session.getUsuarioActual());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id_cliente") : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al obtener id_cliente: {0}", e.getMessage());
            return 0;
        }
    }

    private void registrarActividad(Connection conn, String accion, String detalle) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {
            stmt.setString(1, session.getUsuarioActual());
            stmt.setString(2, accion);
            stmt.setString(3, detalle);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al registrar actividad: {0}", e.getMessage());
        }
    }

    private boolean camposValidos() {
        if (categoriaCombo.getValue() == null) { mostrarError("Campo Requerido", "Selecciona una categoria."); return false; }
        if (fechaPicker.getValue() == null) { mostrarError("Campo Requerido", "Selecciona una fecha de entrega."); return false; }
        if (librasSpinner.getValue() == null || librasSpinner.getValue() <= 0) { mostrarError("Campo Requerido", "Indica las libras."); return false; }
        return true;
    }

    @FXML
    private void cancelar(ActionEvent event) { cerrar(); }

    private void cerrar() {
        ((Stage) guardarBtn.getScene().getWindow()).close();
    }

    private void mostrarError(String t, String m) { alerta(Alert.AlertType.ERROR, t, m); }
    private void mostrarMensaje(String t, String m) { alerta(Alert.AlertType.INFORMATION, t, m); }
    private void alerta(Alert.AlertType tipo, String t, String m) {
        Alert a = new Alert(tipo); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
