package com.example.demo.controller;

import com.example.demo.service.Permiso;
import com.example.demo.service.SessionManager;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppShellController {

    private static final Logger LOGGER = Logger.getLogger(AppShellController.class.getName());

    @FXML private Label userLabel, perfilLabel, breadcrumbLabel;
    @FXML private VBox sidebarContainer;
    @FXML private StackPane contentArea;
    @FXML private VBox welcomePane;
    @FXML private ScrollPane sidebarScroll;
    @FXML private Button toggleSidebarBtn, themeToggleBtn, fullscreenBtn;

    private final Map<String, ModuloInfo> modulos = new LinkedHashMap<>();
    private String currentFxml;
    private boolean maximized = false;
    private boolean darkMode = false;
    private boolean fullscreen = false;
    private static final String DARK_THEME = "/com/example/demo/theme-dark.css";

    private record ModuloInfo(String label, String fxml, String parent, Permiso permiso, String icon, String breadcrumb) {}

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        userLabel.setText(session.getUsuarioActual());
        perfilLabel.setText(session.getPerfilActual());

        toggleSidebarBtn.setOnAction(e -> toggleSidebar());
        themeToggleBtn.setOnAction(e -> toggleTheme());
        fullscreenBtn.setOnAction(e -> toggleFullscreen());

        definirModulos();
        construirSidebar(session);
        cargarDashboardInicial(session);
    }

    private void toggleSidebar() {
        maximized = !maximized;
        double targetWidth = maximized ? 0 : 220;
        double targetMinWidth = maximized ? 0 : 180;
        double targetMaxWidth = maximized ? 0 : 220;

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sidebarScroll.prefWidthProperty(), sidebarScroll.getPrefWidth()),
                new KeyValue(sidebarScroll.minWidthProperty(), sidebarScroll.getMinWidth()),
                new KeyValue(sidebarScroll.maxWidthProperty(), sidebarScroll.getMaxWidth())
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(sidebarScroll.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebarScroll.minWidthProperty(), targetMinWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebarScroll.maxWidthProperty(), targetMaxWidth, Interpolator.EASE_BOTH)
            )
        );
        timeline.setOnFinished(e -> {
            sidebarScroll.setVisible(!maximized);
            sidebarScroll.setManaged(!maximized);
        });
        timeline.play();
        toggleSidebarBtn.setText(maximized ? "\u25B6" : "\u2630");
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        Scene scene = breadcrumbLabel.getScene();
        if (scene == null) return;
        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource(DARK_THEME).toExternalForm());
            themeToggleBtn.setText("\uD83C\uDF19");
        } else {
            scene.getStylesheets().remove(getClass().getResource(DARK_THEME).toExternalForm());
            themeToggleBtn.setText("\u2600\uFE0F");
        }
    }

    private void toggleFullscreen() {
        Stage stage = (Stage) fullscreenBtn.getScene().getWindow();
        if (stage == null) return;
        fullscreen = !fullscreen;
        stage.setFullScreen(fullscreen);
        fullscreenBtn.setText(fullscreen ? "\u2716" : "\u2B1E");
    }

    private void definirModulos() {
        modulos.put("dashboard",       new ModuloInfo("Dashboard",         "DashboardAdmin.fxml",      null,    Permiso.DASHBOARD_ADMIN_LEER,   "\uD83D\uDCCA", "Dashboard"));
        modulos.put("dashboard-emp",   new ModuloInfo("Mi Dashboard",      "DashboardEmpleado.fxml",   null,    Permiso.DASHBOARD_EMPLEADO_LEER,"\uD83D\uDCCA", "Dashboard"));
        modulos.put("dashboard-cli",   new ModuloInfo("Mi Dashboard",      "DashboardCliente.fxml",    null,    Permiso.DASHBOARD_CLIENTE_LEER, "\uD83D\uDCCA", "Dashboard"));

        modulos.put("ordenes-produccion", new ModuloInfo("Ordenes Produccion","OrdenesProduccion.fxml", "produccion", Permiso.PRODUCCION_LEER, "\uD83D\uDD27", "Produccion > Ordenes"));

        modulos.put("planificacion",   new ModuloInfo("Planificacion",     "Planificacion.fxml",       "produccion", Permiso.PRODUCCION_LEER, "\uD83D\uDCC5", "Produccion > Planificacion"));
        modulos.put("productos",       new ModuloInfo("Productos",         "Productos.fxml",           "produccion", Permiso.INVENTARIO_LEER, "\uD83C\uDF82", "Produccion > Productos"));

        modulos.put("inventario",      new ModuloInfo("Inventario",        "Inventario.fxml",          "inventario", Permiso.INVENTARIO_LEER, "\uD83D\uDCE6", "Inventario"));
        modulos.put("compras",         new ModuloInfo("Compras",           "HistorialCompras.fxml",    "inventario", Permiso.INVENTARIO_LEER, "\uD83D\uDED2", "Inventario > Compras"));
        modulos.put("proveedores",     new ModuloInfo("Proveedores",       "Proveedores.fxml",         "inventario", Permiso.INVENTARIO_LEER, "\uD83C\uDFED", "Inventario > Proveedores"));

        modulos.put("entregas",        new ModuloInfo("Entregas",          "Entregas.fxml",            "entregas",  Permiso.ENTREGAS_LEER, "\uD83D\uDE9A", "Entregas y Cobros"));

        modulos.put("personal",        new ModuloInfo("Personal",          "Personal.fxml",            "personal",  Permiso.PERSONAL_LEER, "\uD83D\uDC64", "Personal"));

        modulos.put("limpieza",        new ModuloInfo("Limpieza",          "Limpieza.fxml",            "operaciones", Permiso.LIMPIEZA_LEER, "\uD83E\uDDF9", "Operaciones > Limpieza"));
        modulos.put("mantenimiento",   new ModuloInfo("Mantenimiento",     "Mantenimiento.fxml",       "operaciones", Permiso.MANTENIMIENTO_LEER, "\uD83D\uDD27", "Operaciones > Mantenimiento"));

        modulos.put("chefsbox",        new ModuloInfo("Chef's Box",        "ChefsBox.fxml",            "plataforma", Permiso.CHEFS_BOX_LEER, "\uD83C\uDF81", "Plataforma Digital"));

        modulos.put("facturas",        new ModuloInfo("Facturación",       "Factura.fxml",             null,       Permiso.FACTURACION_LEER, "\uD83D\uDCCB", "Administracion > Facturación"));
        modulos.put("reportes",        new ModuloInfo("Reportes",          "Reportes.fxml",            null,       Permiso.REPORTES_LEER, "\uD83D\uDCC8", "Reportes"));
        modulos.put("usuarios",        new ModuloInfo("Usuarios",          "Usuarios.fxml",            null,       Permiso.DASHBOARD_ADMIN_LEER, "\uD83D\uDC65", "Administracion > Usuarios"));
        modulos.put("mis-pedidos",     new ModuloInfo("Mis Pedidos",       "ClientePedidos.fxml",      null,       Permiso.PEDIDOS_LEER, "\uD83D\uDCDD", "Mis Pedidos"));
        modulos.put("mi-perfil",       new ModuloInfo("Mi Perfil",         "MiPerfil.fxml",            null,       Permiso.PERFIL_LEER,   "\uD83D\uDC64", "Mi Perfil"));
    }

    private void construirSidebar(SessionManager session) {
        sidebarContainer.getChildren().clear();

        var itemsPorGrupo = new LinkedHashMap<String, List<Map.Entry<String, ModuloInfo>>>();
        var sinGrupo = new ArrayList<Map.Entry<String, ModuloInfo>>();

        for (var e : modulos.entrySet()) {
            if (!session.tienePermiso(e.getValue().permiso())) continue;
            if (session.isCliente() && "ordenes-produccion".equals(e.getKey())) continue;
            if (!session.isCliente() && "mis-pedidos".equals(e.getKey())) continue;
            if (e.getValue().parent() != null) {
                itemsPorGrupo.computeIfAbsent(e.getValue().parent(), k -> new ArrayList<>()).add(e);
            } else {
                sinGrupo.add(e);
            }
        }

        String dashboardKey = null;
        if (session.tienePermiso(Permiso.DASHBOARD_ADMIN_LEER)) {
            dashboardKey = "dashboard";
        } else if (session.tienePermiso(Permiso.DASHBOARD_EMPLEADO_LEER)) {
            dashboardKey = "dashboard-emp";
        } else if (session.tienePermiso(Permiso.DASHBOARD_CLIENTE_LEER)) {
            dashboardKey = "dashboard-cli";
        }
        final String dKey = dashboardKey;
        var dashboardItems = sinGrupo.stream()
            .filter(e -> e.getKey().startsWith("dashboard-") || e.getKey().equals("dashboard"))
            .filter(e -> dKey == null || e.getKey().equals(dKey))
            .toList();
        var adminItems = sinGrupo.stream().filter(e -> List.of("reportes", "mi-perfil", "facturas", "usuarios").contains(e.getKey())).toList();
        var clientItems = sinGrupo.stream().filter(e -> "mis-pedidos".equals(e.getKey())).toList();

        Map<String, String> etiquetasGrupo = Map.ofEntries(
            Map.entry("produccion", "PRODUCCION"),
            Map.entry("inventario", "INVENTARIO"),
            Map.entry("entregas", "ENTREGAS Y COBROS"),
            Map.entry("personal", "PERSONAL"),
            Map.entry("operaciones", "OPERACIONES"),
            Map.entry("plataforma", "PLATAFORMA DIGITAL")
        );

        List<String> ordenGrupos = List.of("produccion", "inventario", "entregas", "personal", "operaciones", "plataforma");

        if (!dashboardItems.isEmpty()) {
            Label sec = new Label("PRINCIPAL");
            sec.getStyleClass().add("sidebar-section");
            sidebarContainer.getChildren().add(sec);
            for (var e : dashboardItems) {
                sidebarContainer.getChildren().add(crearBoton(e.getKey(), e.getValue()));
            }
        }

        if (!clientItems.isEmpty()) {
            Label sec = new Label("MIS COSAS");
            sec.getStyleClass().add("sidebar-section");
            sidebarContainer.getChildren().add(sec);
            for (var e : clientItems) {
                sidebarContainer.getChildren().add(crearBoton(e.getKey(), e.getValue()));
            }
        }

        for (String grupo : ordenGrupos) {
            var items = itemsPorGrupo.get(grupo);
            if (items == null || items.isEmpty()) continue;
            Label header = new Label(etiquetasGrupo.getOrDefault(grupo, grupo.toUpperCase()));
            header.getStyleClass().add("sidebar-section");
            sidebarContainer.getChildren().add(header);
            for (var e : items) {
                sidebarContainer.getChildren().add(crearBoton(e.getKey(), e.getValue()));
            }
        }

        if (!adminItems.isEmpty()) {
            Label sec = new Label("ADMINISTRACION");
            sec.getStyleClass().add("sidebar-section");
            sidebarContainer.getChildren().add(sec);
            for (var e : adminItems) {
                sidebarContainer.getChildren().add(crearBoton(e.getKey(), e.getValue()));
            }
        }

        sidebarContainer.getChildren().add(new Region());
    }

    private Button crearBoton(String id, ModuloInfo info) {
        Button btn = new Button(info.icon() + "  " + info.label());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("sidebar-btn");
        btn.getProperties().put("moduloId", id);
        btn.setOnAction(e -> cargarModulo(id, info));
        return btn;
    }

    private void cargarModulo(String id, ModuloInfo info) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/" + info.fxml()));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardAdminController) {
                ((DashboardAdminController) controller).setModoEmbedded(true);
            } else if (controller instanceof DashboardEmpleadoController) {
                ((DashboardEmpleadoController) controller).setModoEmbedded(true);
            } else if (controller instanceof DashboardClienteController) {
                ((DashboardClienteController) controller).setModoEmbedded(true);
            } else if (controller instanceof PlanificacionController) {
                ((PlanificacionController) controller).setModoEmbedded(true);
            }

            welcomePane.setVisible(false);
            welcomePane.setManaged(false);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

            breadcrumbLabel.setText(info.breadcrumb());
            currentFxml = info.fxml();

            for (Node node : sidebarContainer.getChildren()) {
                if (node instanceof Button b) {
                    Object raw = b.getProperties().get("moduloId");
                    b.getStyleClass().removeAll("sidebar-btn-active");
                    if (raw != null && raw.equals(id)) {
                        b.getStyleClass().add("sidebar-btn-active");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar modulo {0}: {1}", new Object[]{info.fxml(), e.getMessage()});
        }
    }

    private void cargarDashboardInicial(SessionManager session) {
        if (session.tienePermiso(Permiso.DASHBOARD_ADMIN_LEER)) {
            cargarModulo("dashboard", modulos.get("dashboard"));
        } else if (session.tienePermiso(Permiso.DASHBOARD_EMPLEADO_LEER)) {
            cargarModulo("dashboard-emp", modulos.get("dashboard-emp"));
        } else if (session.tienePermiso(Permiso.DASHBOARD_CLIENTE_LEER)) {
            cargarModulo("dashboard-cli", modulos.get("dashboard-cli"));
        }
    }

    @FXML
    private void cerrarSesion() {
        SessionManager.getInstance().cerrarSesion();
        try {
            Stage stage = (Stage) breadcrumbLabel.getScene().getWindow();
            if (stage.isFullScreen()) stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/Login.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            stage.setScene(scene);
            stage.setTitle("Reposteria Rosato - Iniciar Sesion");
            stage.sizeToScene();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cerrar sesion: {0}", e.getMessage());
        }
    }
}
