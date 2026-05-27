package com.example.demo.controller;

import com.example.demo.dao.OrdenProduccionDAO;
import com.example.demo.dao.RecetaDAO;
import com.example.demo.model.OrdenProduccion;
import com.example.demo.model.OrdenProduccion.OrdenFase;
import com.example.demo.model.OrdenProduccion.OrdenHistorial;
import com.example.demo.model.OrdenProduccion.OrdenIngrediente;
import com.example.demo.model.Receta;
import com.example.demo.service.ReportService;
import com.example.demo.service.SessionManager;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrdenProduccionDetalleController {

    private static final Logger LOGGER = Logger.getLogger(OrdenProduccionDetalleController.class.getName());
    private static final String[] ESTADOS = {"ACTIVA", "EN PRODUCCION", "COMPLETADA", "ENTREGADA", "CANCELADA"};

    @FXML private Label numOrdenLabel, estadoLabel, clienteLabel, categoriaLabel, librasLabel;
    @FXML private Label fechaLabel, horaLabel, vendedorLabel, recetaLabel, direccionLabel, telefonoLabel;
    @FXML private Label costoEstLabel, costoRealLabel, precioLabel, anticipoLabel, saldoLabel;
    @FXML private Label baseLabel, masoLabel, formaLabel, pisosLabel;
    @FXML private Label decoracionLabel, lustresLabel, camuflajesLabel, floresLabel;
    @FXML private Label mensajeLabel, adornosLabel, rellenosLabel, observacionesLabel;
    @FXML private Label progresoLabel, pausadoLabel;
    @FXML private Label tipoPagoLabel, estadoPagoLabel, fechaInicioLabel, fechaCompletadoLabel;

    @FXML private ProgressBar progresoBar;
    @FXML private ComboBox<String> estadoCombo;
    @FXML private Button cambiarEstadoBtn, pausarBtn, descontarStockBtn, verRecetaBtn;
    @FXML private Button editarBtn, cerrarBtn, imprimirBtn;

    @FXML private VBox fasesContainer;
    @FXML private TableView<OrdenIngrediente> ingredientesTable;
    @FXML private TableColumn<OrdenIngrediente, String> ingNombreCol, ingUnidadCol, ingEstadoCol;
    @FXML private TableColumn<OrdenIngrediente, Double> ingReqCol, ingDescCol;

    @FXML private TableView<OrdenHistorial> historialTable;
    @FXML private TableColumn<OrdenHistorial, String> histAccionCol, histDetalleCol, histUsuarioCol, histFechaCol;

    private OrdenProduccionDAO ordenDAO;
    private OrdenProduccion orden;
    private int ordenId;

    public void setOrdenDAO(OrdenProduccionDAO dao) { this.ordenDAO = dao; }
    public void setOrdenId(int id) {
        this.ordenId = id;
        if (id > 0) cargarOrden();
    }

    @FXML
    public void initialize() {
        if (ordenDAO == null) ordenDAO = new OrdenProduccionDAO();
        configurarCombos();
        configurarTablas();
        if (ordenId > 0) cargarOrden();
    }

    private void configurarCombos() {
        estadoCombo.getItems().addAll(ESTADOS);
        cambiarEstadoBtn.setOnAction(e -> cambiarEstado());
        pausarBtn.setOnAction(e -> pausarReanudar());
        descontarStockBtn.setOnAction(e -> descontarStock());
        verRecetaBtn.setOnAction(e -> verReceta());
        editarBtn.setOnAction(e -> editarOrden());
        imprimirBtn.setOnAction(e -> imprimirOrden());
    }

    private void configurarTablas() {
        ingNombreCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreIngrediente()));
        ingReqCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCantidadRequerida()));
        ingDescCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCantidadDescontada()));
        ingUnidadCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUnidad()));
        ingEstadoCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().isDescontado() ? "DESCONTADO" : "PENDIENTE"));
        ingEstadoCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("DESCONTADO".equals(s) ? "-fx-text-fill: #28A745; -fx-font-weight: bold;" : "-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            }
        });

        histAccionCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getAccion()));
        histDetalleCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDetalle()));
        histUsuarioCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsuario()));
        histFechaCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFechaHoraStr()));
    }

    private void cargarOrden() {
        orden = ordenDAO.obtenerPorId(ordenId);
        if (orden == null) return;

        numOrdenLabel.setText(orden.getNumeroOrden());
        estadoLabel.setText(orden.getEstado());
        estadoLabel.setStyle("-fx-background-color: " + getEstadoColor(orden.getEstado()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4;");
        clienteLabel.setText(orden.getCliente());
        categoriaLabel.setText(orden.getCategoria());
        librasLabel.setText(orden.getLibras() + " lbs");
        fechaLabel.setText(orden.getFechaEntrega());
        horaLabel.setText(orden.getHoraEntrega());
        vendedorLabel.setText(orden.getVendedor());
        boolean tieneReceta = orden.getIdReceta() > 0;
        if (tieneReceta) {
            recetaLabel.setText("\u2713 " + orden.getNombreReceta());
            recetaLabel.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
        } else {
            recetaLabel.setText("\u25CB Sin receta");
            recetaLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-style: italic;");
        }
        descontarStockBtn.setDisable(!tieneReceta);
        verRecetaBtn.setText(tieneReceta ? "Ver Receta" : "Asignar Receta");
        direccionLabel.setText(orden.getDireccion());
        telefonoLabel.setText(orden.getTelefono());
        costoEstLabel.setText("RD$" + String.format("%.2f", orden.getCostoEstimado()));
        costoRealLabel.setText("RD$" + String.format("%.2f", orden.getCostoReal()));
        precioLabel.setText("RD$" + String.format("%.2f", orden.getPrecioVenta()));
        anticipoLabel.setText("RD$" + String.format("%.2f", orden.getAnticipo()));
        saldoLabel.setText("RD$" + String.format("%.2f", orden.getSaldo()));

        tipoPagoLabel.setText(orden.getTipoPago() != null ? orden.getTipoPago() : "Efectivo");
        String ep = orden.getEstadoPago() != null ? orden.getEstadoPago() : "Pendiente";
        estadoPagoLabel.setText(ep);
        String epBg;
        switch (ep) {
            case "Pagado": epBg = "#28A745"; break;
            case "En Proceso": epBg = "#FF9800"; break;
            case "Pendiente": epBg = "#DC3545"; break;
            default: epBg = "#6C757D";
        }
        estadoPagoLabel.setStyle("-fx-background-color: " + epBg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4;");

        fechaInicioLabel.setText(orden.getFechaInicioStr());
        fechaCompletadoLabel.setText(orden.getFechaCompletadoStr());
        baseLabel.setText(orden.getBaseTipo());
        masoLabel.setText(orden.getMaso());
        formaLabel.setText(orden.getForma());
        pisosLabel.setText(String.valueOf(orden.getPisos()));
        decoracionLabel.setText(orden.getDecoracion());
        lustresLabel.setText(orden.getLustres());
        camuflajesLabel.setText(orden.getCamuflajes());
        floresLabel.setText(orden.getFlores());
        mensajeLabel.setText(orden.getMensaje());
        adornosLabel.setText(orden.getAdornos());
        rellenosLabel.setText(orden.getRellenos());
        observacionesLabel.setText(orden.getObservaciones());

        progresoBar.setProgress(orden.getProgreso() / 100.0);
        progresoLabel.setText(orden.getProgreso() + "%");

        if (orden.isPausado()) {
            pausadoLabel.setText("PAUSADO");
            pausadoLabel.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");
        } else {
            pausadoLabel.setText("EN CURSO");
            pausadoLabel.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");
        }

        estadoCombo.setValue(orden.getEstado());

        cargarFases();
        ingredientesTable.setItems(FXCollections.observableArrayList(orden.getIngredientes()));
        historialTable.setItems(FXCollections.observableArrayList(orden.getHistorial()));
    }

    private String getEstadoColor(String est) {
        if (est == null) return "#6C757D";
        switch (est) {
            case "ACTIVA": return "#007BFF";
            case "EN PRODUCCION": return "#FF9800";
            case "COMPLETADA": return "#28A745";
            case "ENTREGADA": return "#6C757D";
            case "CANCELADA": return "#DC3545";
            default: return "#6C757D";
        }
    }

    private void cargarFases() {
        fasesContainer.getChildren().clear();
        List<OrdenFase> fases = orden.getFases();
        if (fases == null || fases.isEmpty()) return;

        for (OrdenFase fase : fases) {
            HBox faseRow = new HBox(10);
            faseRow.setAlignment(Pos.CENTER_LEFT);
            faseRow.setPadding(new Insets(8));
            faseRow.setMaxWidth(Double.MAX_VALUE);

            String faseBg;
            switch (fase.getEstado()) {
                case "EN CURSO": faseBg = "#FFF3CD"; break;
                case "COMPLETADA": faseBg = "#D4EDDA"; break;
                default: faseBg = "#F8F9FA";
            }
            faseRow.setStyle("-fx-background-color: " + faseBg + "; -fx-border-color: #dee2e6; -fx-border-radius: 6; -fx-background-radius: 6;");

            Label numLbl = new Label(String.valueOf(fase.getFaseOrden()));
            numLbl.setStyle("-fx-background-color: " + getFaseColor(fase.getEstado()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-radius: 14;");
            numLbl.setAlignment(Pos.CENTER);
            numLbl.setMinWidth(28);
            numLbl.setMinHeight(28);

            Label nombreLbl = new Label(fase.getFaseNombre());
            nombreLbl.setStyle("-fx-font-weight: bold; -fx-min-width: 160;");

            Label estadoFaseLbl = new Label(fase.getEstado());
            estadoFaseLbl.setStyle("-fx-background-color: " + getFaseColor(fase.getEstado()) + "33; -fx-text-fill: " + getFaseColor(fase.getEstado()) + "; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4; -fx-min-width: 100;");

            Label userLbl = new Label();
            if (fase.getUsuarioCompleta() != null) userLbl.setText("por: " + fase.getUsuarioCompleta());
            else if (fase.getUsuarioInicia() != null) userLbl.setText("inicio: " + fase.getUsuarioInicia());
            userLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button accionBtn = new Button();
            if ("PENDIENTE".equals(fase.getEstado())) {
                accionBtn.setText("Iniciar");
                accionBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                accionBtn.setOnAction(e -> iniciarFase(fase));
            } else if ("EN CURSO".equals(fase.getEstado())) {
                accionBtn.setText("Completar");
                accionBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                accionBtn.setOnAction(e -> completarFase(fase));
            }

            faseRow.getChildren().addAll(numLbl, nombreLbl, estadoFaseLbl, userLbl, spacer);
            if (accionBtn.getText() != null) faseRow.getChildren().add(accionBtn);
            fasesContainer.getChildren().add(faseRow);
        }
    }

    private String getFaseColor(String est) {
        if (est == null) return "#6C757D";
        switch (est) {
            case "EN CURSO": return "#FF9800";
            case "COMPLETADA": return "#28A745";
            default: return "#6C757D";
        }
    }

    private void iniciarFase(OrdenFase fase) {
        String usuario = SessionManager.getInstance().getUsuarioActual();
        if (ordenDAO.iniciarFase(fase.getIdFase(), usuario)) {
            if (!"EN PRODUCCION".equals(orden.getEstado())) {
                ordenDAO.cambiarEstado(orden.getId(), "EN PRODUCCION", usuario);
            }
            cargarOrden();
        }
    }

    private void completarFase(OrdenFase fase) {
        String observaciones = mostrarDialogoObservaciones();
        String usuario = SessionManager.getInstance().getUsuarioActual();
        if (ordenDAO.completarFase(fase.getIdFase(), usuario, observaciones)) {
            cargarOrden();
        }
    }

    private String mostrarDialogoObservaciones() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Completar Fase");
        dialog.setHeaderText("Agregue observaciones (opcional)");
        dialog.setContentText("Observaciones:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    private void cambiarEstado() {
        String nuevoEstado = estadoCombo.getValue();
        if (nuevoEstado == null || nuevoEstado.equals(orden.getEstado())) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cambiar Estado");
        confirm.setHeaderText("Cambiar estado a: " + nuevoEstado);
        confirm.setContentText("Esta seguro de cambiar el estado de la orden " + orden.getNumeroOrden() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String usuario = SessionManager.getInstance().getUsuarioActual();
            if (ordenDAO.cambiarEstado(orden.getId(), nuevoEstado, usuario)) {
                cargarOrden();
            }
        }
    }

    private void pausarReanudar() {
        boolean pausar = !orden.isPausado();
        String usuario = SessionManager.getInstance().getUsuarioActual();
        if (ordenDAO.pausarReanudar(orden.getId(), pausar, usuario)) {
            cargarOrden();
        }
    }

    private void descontarStock() {
        if (!"EN PRODUCCION".equals(orden.getEstado()) && !"ACTIVA".equals(orden.getEstado())) {
            mostrarError("Estado invalido", "Solo se puede descontar stock en ordenes ACTIVAS o EN PRODUCCION.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Descontar Stock");
        confirm.setHeaderText("Descontar ingredientes del inventario");
        confirm.setContentText("Se descontaran los ingredientes necesarios del stock actual. Continuar?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String usuario = SessionManager.getInstance().getUsuarioActual();
            if (ordenDAO.descontarStock(orden.getId(), usuario)) {
                mostrarMensaje("Stock descontado", "Ingredientes descontados correctamente del inventario.");
                cargarOrden();
            } else {
                mostrarError("Stock insuficiente", "No hay suficiente stock de algunos ingredientes.");
            }
        }
    }

    private void verReceta() {
        if (orden.getIdReceta() > 0) {
            abrirVisorReceta(orden.getIdReceta());
        } else {
            mostrarDialogoAsignarOcrear();
        }
    }

    private void abrirVisorReceta(int idReceta) {
        try {
            RecetaDAO recetaDAO = new RecetaDAO();
            Receta receta = recetaDAO.obtenerPorId(idReceta);
            if (receta == null) { mostrarError("Error", "No se encontro la receta."); return; }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaViewer.fxml"));
            Parent root = loader.load();
            RecetaViewerController viewer = loader.getController();
            viewer.setReceta(receta);
            viewer.setModoCapacitacion(false);
            viewer.initialize();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 750, 650));
            stage.setTitle("Receta: " + receta.getNombreReceta());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error ver receta: {0}", e.getMessage());
        }
    }

    private void mostrarDialogoAsignarOcrear() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Asignar Receta");
        dialog.setHeaderText("La orden " + orden.getNumeroOrden() + " no tiene receta asociada.");

        Button asignarBtn = new Button("Asignar receta existente");
        asignarBtn.setMaxWidth(Double.MAX_VALUE);
        asignarBtn.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12; -fx-font-size: 14px; -fx-cursor: hand;");
        asignarBtn.setOnAction(e -> { dialog.setResult("ASIGNAR"); dialog.close(); });

        Button crearBtn = new Button("Crear nueva receta");
        crearBtn.setMaxWidth(Double.MAX_VALUE);
        crearBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12; -fx-font-size: 14px; -fx-cursor: hand;");
        crearBtn.setOnAction(e -> { dialog.setResult("CREAR"); dialog.close(); });

        VBox content = new VBox(15, new Label("Selecciona una opcion:"), asignarBtn, crearBtn);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.showAndWait().ifPresent(r -> {
            if ("ASIGNAR".equals(r)) mostrarDialogoAsignarReceta();
            else if ("CREAR".equals(r)) abrirCrearReceta();
        });
    }

    private void mostrarDialogoAsignarReceta() {
        RecetaDAO recetaDAO = new RecetaDAO();
        List<Receta> todas = recetaDAO.listarTodas();
        if (todas.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Sin recetas");
            alert.setHeaderText("No hay recetas disponibles.");
            alert.setContentText("Deseas crear una nueva receta ahora?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                abrirCrearReceta();
            }
            return;
        }

        Dialog<Receta> dialog = new Dialog<>();
        dialog.setTitle("Asignar receta existente");
        dialog.setHeaderText("Selecciona una receta para la orden " + orden.getNumeroOrden());

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar receta...");

        ListView<Receta> listView = new ListView<>();
        ObservableList<Receta> items = FXCollections.observableArrayList(todas);
        listView.setItems(items);
        listView.getSelectionModel().selectFirst();
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Receta r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setText(null); return; }
                setText(r.getNombreReceta() + "  (" + (r.getCategoria() != null ? r.getCategoria() : "Sin categoria") + ")");
            }
        });

        searchField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().isEmpty()) {
                listView.setItems(items);
            } else {
                String q = n.toLowerCase().trim();
                listView.setItems(FXCollections.observableArrayList(
                    todas.stream().filter(r -> r.getNombreReceta().toLowerCase().contains(q)
                        || (r.getCategoria() != null && r.getCategoria().toLowerCase().contains(q))
                        || (r.getNombreProducto() != null && r.getNombreProducto().toLowerCase().contains(q)))
                        .collect(Collectors.toList())
                ));
            }
        });

        VBox content = new VBox(10, searchField, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        content.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(450, 350);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(receta -> {
            if (receta == null) return;
            if (ordenDAO.asignarReceta(orden.getId(), receta.getId())) {
                orden.setIdReceta(receta.getId());
                orden.setNombreReceta(receta.getNombreReceta());
                orden.setIngredientes(new java.util.ArrayList<>());
                cargarOrden();
                mostrarMensaje("Receta asignada", "Receta \"" + receta.getNombreReceta() + "\" asignada correctamente.");
            } else {
                mostrarError("Error", "No se pudo asignar la receta.");
            }
        });
    }

    private void abrirCrearReceta() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/RecetaModal.fxml"));
            Parent root = loader.load();
            RecetaModalController modalController = loader.getController();
            modalController.setRecetaDAO(new RecetaDAO());

            Stage stage = new Stage();
            stage.setTitle("Nueva Receta");
            stage.setScene(new Scene(root, 600, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            RecetaDAO recetaDAO = new RecetaDAO();
            List<Receta> recetas = recetaDAO.listarTodas();
            if (!recetas.isEmpty()) {
                Receta ultima = recetas.get(0);
                if (ordenDAO.asignarReceta(orden.getId(), ultima.getId())) {
                    orden = ordenDAO.obtenerPorId(ordenId);
                    cargarOrden();
                    mostrarMensaje("Receta creada y asignada",
                        "La nueva receta \"" + ultima.getNombreReceta() + "\" se ha creado y asignado a la orden.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear receta", e);
            mostrarError("Error", "No se pudo abrir el formulario de creacion de receta.");
        }
    }

    private void editarOrden() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/OrdenProduccionForm.fxml"));
            Parent root = loader.load();
            OrdenProduccionFormController controller = loader.getController();
            controller.setOrdenDAO(ordenDAO);
            controller.setOrden(orden);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 750));
            stage.setTitle("Editar Orden: " + orden.getNumeroOrden());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> cargarOrden());
            stage.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error editar orden: {0}", e.getMessage());
        }
    }

    @FXML private void cerrar() { ((Stage) cerrarBtn.getScene().getWindow()).close(); }

    private void imprimirOrden() {
        try {
            ReportService rs = new ReportService();
            JasperReport report = rs.compileReport("/reportes/Reporte_OrdenProduccion.jrxml");
            Map<String, Object> params = new HashMap<>();
            params.put("ORDEN", orden.getNumeroOrden());
            params.put("ESTADO", orden.getEstado());
            params.put("CATEGORIA", orden.getCategoria());
            params.put("REVESTIMIENTO", orden.getRevestimiento());
            params.put("SUCURSAL", orden.getSucursal());
            params.put("FECHA_ENTREGA", orden.getFechaEntrega());
            params.put("HORA_ENTREGA", orden.getHoraEntrega());
            params.put("CLIENTE", orden.getCliente());
            params.put("DIRECCION", orden.getDireccion());
            params.put("VENDEDOR", orden.getVendedor());
            params.put("TELEFONO", orden.getTelefono());
            params.put("LIBRAS", String.valueOf(orden.getLibras()));
            params.put("BASE", orden.getBaseTipo());
            params.put("MASA", orden.getMaso());
            params.put("FORMA", orden.getForma());
            params.put("PISOS", String.valueOf(orden.getPisos()));
            params.put("ADORNOS", orden.getAdornos());
            params.put("RELLENOS", orden.getRellenos());
            params.put("LUSTRES", orden.getLustres());
            params.put("DECORACION", orden.getDecoracion());
            params.put("CAMUFLAJES", orden.getCamuflajes());
            params.put("FLOR", orden.getFlores());
            params.put("MENSAJE", orden.getMensaje());
            params.put("OBSERVACION", orden.getObservaciones());
            params.put("ADORNO_COSTO", BigDecimal.ZERO);
            params.put("ALQUILER", BigDecimal.ZERO);
            params.put("SUBTOTAL", BigDecimal.valueOf(orden.getPrecioVenta()));
            params.put("DESCUENTO", BigDecimal.ZERO);
            params.put("IMPUESTOS", BigDecimal.ZERO);
            params.put("GARANTIA", BigDecimal.ZERO);
            params.put("ENVIO", BigDecimal.ZERO);
            params.put("TOTAL", BigDecimal.valueOf(orden.getPrecioVenta()));

            JasperPrint print = rs.fillReport(report, params);
            rs.showReport(print);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al imprimir orden: {0}", e.getMessage());
            mostrarError("Error", "No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private void mostrarMensaje(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}
