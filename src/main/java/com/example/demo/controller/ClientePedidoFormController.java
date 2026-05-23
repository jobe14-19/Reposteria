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

public class ClientePedidoFormController {

    private static final Logger LOGGER = Logger.getLogger(ClientePedidoFormController.class.getName());

    private static final String SQL_INSERTAR_PEDIDO =
        "INSERT INTO pedidos (id_cliente, username, fecha_pedido, fecha_entrega, producto, libras, diseno, total, adelanto, observaciones, estado) VALUES (?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, 'Pendiente')";
    private static final String SQL_OBTENER_ID_CLIENTE =
        "SELECT id_cliente FROM clientes WHERE username = ?";
    private static final String SQL_INSERTAR_PAGO =
        "INSERT INTO pagos (id_pedido, monto, fecha_pago, estado) VALUES (?, ?, GETDATE(), 'Pagado')";
    private static final String SQL_REGISTRAR_ACTIVIDAD =
        "INSERT INTO actividad (fecha_hora, usuario, accion, detalle) VALUES (GETDATE(), ?, ?, ?)";

    private static final String[] HORAS = {"08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00"};
    private static final String[] CATEGORIAS = {"Vainilla","Chocolate","Fresa","Zanahoria","Cheesecake","Ron","Red Velvet","Tres Leches"};
    private static final String[] BASES = {"Bizcocho","Galleta","Brownie","Base Crujiente","Sin Base"};
    private static final String[] MASAS = {"Tradicional","Esponjosa","Hojaldrada","Genovesa","Queque"};
    private static final String[] FORMAS = {"Redonda","Cuadrada","Rectangular","Corazon","Hexagonal","Personalizada"};

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
    @FXML private TextField precioField;
    @FXML private TextField anticipoField;
    @FXML private Button guardarBtn;

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

        setupValidation();
    }

    private void setupValidation() {
        categoriaCombo.valueProperty().addListener((obs, o, n) -> validar());
        fechaPicker.valueProperty().addListener((obs, o, n) -> validar());
        librasSpinner.valueProperty().addListener((obs, o, n) -> validar());
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
        String diseno = decoracionArea.getText();
        double total = parseDouble(precioField.getText());
        double anticipo = parseDouble(anticipoField.getText());

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idCliente = obtenerIdCliente(conn);
                String username = session.getUsuarioActual();

                int idPedido = insertarPedido(conn, idCliente, username, categoria, libras, diseno, total, anticipo);

                if (idPedido > 0) {
                    if (anticipo > 0) registrarPago(conn, idPedido, anticipo);
                    registrarActividad("CREAR PEDIDO", "Nuevo pedido creado por cliente: #" + idPedido);
                    crearOrdenProduccion();
                    conn.commit();
                    mostrarMensaje("Pedido Creado", "Tu pedido se ha registrado correctamente.");
                    cerrar();
                } else {
                    conn.rollback();
                    mostrarError("Error", "No se pudo crear el pedido.");
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar pedido: {0}", e.getMessage());
            mostrarError("Error de Base de Datos", "No se pudo procesar el pedido: " + e.getMessage());
        }
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
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private void crearOrdenProduccion() {
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
            orden.setPrecioVenta(parseDouble(precioField.getText()));
            orden.setAnticipo(parseDouble(anticipoField.getText()));
            orden.setSaldo(parseDouble(precioField.getText()) - parseDouble(anticipoField.getText()));
            orden.setUsuarioCrea(session.getUsuarioActual());
            orden.setProgreso(0);
            orden.setPausado(false);
            new OrdenProduccionDAO().insertar(orden);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al generar orden de produccion: {0}", e.getMessage());
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

    private void registrarPago(Connection conn, int idPedido, double monto) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_PAGO)) {
            stmt.setInt(1, idPedido);
            stmt.setDouble(2, monto);
            stmt.executeUpdate();
        }
    }

    private void registrarActividad(String accion, String detalle) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_REGISTRAR_ACTIVIDAD)) {
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

    private double parseDouble(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(text.replace("$", "").replace(",", "").trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private void mostrarError(String t, String m) { alerta(Alert.AlertType.ERROR, t, m); }
    private void mostrarMensaje(String t, String m) { alerta(Alert.AlertType.INFORMATION, t, m); }
    private void alerta(Alert.AlertType tipo, String t, String m) {
        Alert a = new Alert(tipo); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
