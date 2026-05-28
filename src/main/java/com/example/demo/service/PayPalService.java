package com.example.demo.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayPalService {

    private static final Logger LOGGER = Logger.getLogger(PayPalService.class.getName());

    public PayPalCheckoutResult crearCheckoutSession(double monto, String nombreProducto, String clienteEmail, int idPedido) {
        if (!PayPalConfig.isConfigured()) {
            return new PayPalCheckoutResult(false, null, "PayPal no configurado: edita paypal.properties.");
        }
        if (PayPalConfig.isSimulation()) {
            return simularCheckout(idPedido);
        }
        try {
            String token = obtenerAccessToken();
            if (token == null) {
                return new PayPalCheckoutResult(false, null, "No se pudo obtener token de PayPal.");
            }
            String body = "{\n" +
                "  \"intent\": \"CAPTURE\",\n" +
                "  \"purchase_units\": [{\n" +
                "    \"reference_id\": \"" + idPedido + "\",\n" +
                "    \"description\": \"" + escaparJson(nombreProducto) + "\",\n" +
                "    \"amount\": {\n" +
                "      \"currency_code\": \"USD\",\n" +
                "      \"value\": \"" + String.format("%.2f", monto) + "\"\n" +
                "    }\n" +
                "  }],\n" +
                "  \"payment_source\": {\n" +
                "    \"paypal\": {\n" +
                "      \"experience_context\": {\n" +
                "        \"payment_method_preference\": \"IMMEDIATE_PAYMENT_REQUIRED\",\n" +
                "        \"landing_page\": \"LOGIN\",\n" +
                "        \"user_action\": \"PAY_NOW\",\n" +
                "        \"return_url\": \"https://paypal.com/checkout/success\",\n" +
                "        \"cancel_url\": \"https://paypal.com/checkout/cancel\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

            HttpURLConnection conn = (HttpURLConnection) new URL(PayPalConfig.API_URL + "/v2/checkout/orders").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String json = leerRespuesta(conn, code);
            if (code != 200 && code != 201) {
                return new PayPalCheckoutResult(false, null, "Error PayPal (" + code + "): " + extraerError(json));
            }
            String orderId = extraerValor(json, "\"id\":\"", "\"");
            String approvalUrl = null;
            String links = json;
            String linkSearch = "\"href\":\"";
            int idx = links.indexOf("\"rel\":\"approve\"");
            if (idx >= 0) {
                int hrefStart = links.indexOf(linkSearch, idx);
                if (hrefStart >= 0) {
                    hrefStart += linkSearch.length();
                    int hrefEnd = links.indexOf("\"", hrefStart);
                    if (hrefEnd >= 0) approvalUrl = links.substring(hrefStart, hrefEnd);
                }
            }
            if (orderId == null) return new PayPalCheckoutResult(false, null, "No se pudo crear la orden PayPal.");
            if (approvalUrl == null) approvalUrl = "https://www.paypal.com/checkoutnow?token=" + orderId;
            return new PayPalCheckoutResult(true, orderId, approvalUrl);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en PayPalService: {0}", e.getMessage());
            return new PayPalCheckoutResult(false, null, "Error de conexion con PayPal: " + e.getMessage());
        }
    }

    private String obtenerAccessToken() {
        try {
            String creds = PayPalConfig.getClientId() + ":" + PayPalConfig.getSecret();
            String auth = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            String body = "grant_type=client_credentials";
            HttpURLConnection conn = (HttpURLConnection) new URL(PayPalConfig.API_URL + "/v1/oauth2/token").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Basic " + auth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String json = leerRespuesta(conn, code);
            if (code != 200) {
                LOGGER.warning("Error obteniendo token PayPal: " + code);
                return null;
            }
            return extraerValor(json, "\"access_token\":\"", "\"");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error token PayPal: {0}", e.getMessage());
            return null;
        }
    }

    public boolean verificarPago(String orderId) {
        if (PayPalConfig.isSimulation()) {
            return true;
        }
        try {
            String token = obtenerAccessToken();
            if (token == null) return false;
            HttpURLConnection conn = (HttpURLConnection) new URL(PayPalConfig.API_URL + "/v2/checkout/orders/" + orderId).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String json = leerRespuesta(conn, code);
            if (code != 200) return false;
            String status = extraerValor(json, "\"status\":\"", "\"");
            return "COMPLETED".equals(status) || "APPROVED".equals(status);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar pago PayPal: {0}", e.getMessage());
            return false;
        }
    }

    private PayPalCheckoutResult simularCheckout(int idPedido) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        String fakeOrderId = "SIM_ORDER_" + idPedido + "_" + System.currentTimeMillis();
        String fakeUrl = "https://www.paypal.com/checkoutnow?token=" + fakeOrderId;
        LOGGER.info("PayPal SIMULACION: orden " + fakeOrderId + " creada.");
        return new PayPalCheckoutResult(true, fakeOrderId, fakeUrl);
    }

    private String leerRespuesta(HttpURLConnection conn, int code) {
        try {
            java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream == null) stream = conn.getInputStream();
            Scanner sc = new Scanner(stream);
            sc.useDelimiter("\\A");
            return sc.hasNext() ? sc.next() : "";
        } catch (Exception e) { return ""; }
    }

    private String extraerValor(String json, String prefijo, String sufijo) {
        int start = json.indexOf(prefijo);
        if (start < 0) return null;
        start += prefijo.length();
        int end = json.indexOf(sufijo, start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private String extraerError(String json) {
        String msg = extraerValor(json, "\"message\":\"", "\"");
        if (msg != null) return msg;
        msg = extraerValor(json, "\"issue\":\"", "\"");
        return msg != null ? msg : "Error desconocido";
    }

    private String escaparJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static class PayPalCheckoutResult {
        public final boolean ok;
        public final String sessionId;
        public final String url;
        public PayPalCheckoutResult(boolean ok, String sessionId, String url) {
            this.ok = ok; this.sessionId = sessionId; this.url = url;
        }
    }
}
