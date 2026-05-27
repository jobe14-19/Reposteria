package com.example.demo.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripeService {

    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());

    public StripeCheckoutResult crearCheckoutSession(double monto, String nombreProducto, String clienteEmail, int idPedido) {
        if (!StripeConfig.isConfigured()) {
            return new StripeCheckoutResult(false, null, "Stripe no configurado: edita stripe.properties en la raiz del proyecto con tu Stripe Secret Key (sk_test_...).");
        }
        try {
            String body = "mode=payment"
                + "&success_url=" + URLEncoder.encode("https://checkout.stripe.com/success", StandardCharsets.UTF_8)
                + "&cancel_url=" + URLEncoder.encode("https://checkout.stripe.com/cancel", StandardCharsets.UTF_8)
                + "&client_reference_id=" + idPedido
                + "&line_items[0][price_data][currency]=usd"
                + "&line_items[0][price_data][product_data][name]=" + URLEncoder.encode(nombreProducto, StandardCharsets.UTF_8)
                + "&line_items[0][price_data][unit_amount]=" + Math.round(monto * 100)
                + "&line_items[0][quantity]=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(StripeConfig.API_URL + "/checkout/sessions").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + StripeConfig.getSecretKey());
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String json = leerRespuesta(conn, code);
            if (code != 200) {
                return new StripeCheckoutResult(false, null, "Error Stripe (" + code + "): " + extraerError(json));
            }
            String url = extraerValor(json, "\"url\":\"", "\"");
            String sessionId = extraerValor(json, "\"id\":\"", "\"");
            if (url == null) return new StripeCheckoutResult(false, null, "No se pudo crear la sesión");
            return new StripeCheckoutResult(true, sessionId, url);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en StripeService: {0}", e.getMessage());
            return new StripeCheckoutResult(false, null, "Error de conexión con Stripe: " + e.getMessage());
        }
    }

    public boolean verificarPago(String sessionId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(StripeConfig.API_URL + "/checkout/sessions/" + sessionId).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + StripeConfig.getSecretKey());
            int code = conn.getResponseCode();
            String json = leerRespuesta(conn, code);
            if (code != 200) return false;
            String paymentStatus = extraerValor(json, "\"payment_status\":\"", "\"");
            return "paid".equals(paymentStatus);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar pago: {0}", e.getMessage());
            return false;
        }
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
        return msg != null ? msg : "Error desconocido";
    }

    public static class StripeCheckoutResult {
        public final boolean ok;
        public final String sessionId;
        public final String url;
        public StripeCheckoutResult(boolean ok, String sessionId, String url) {
            this.ok = ok; this.sessionId = sessionId; this.url = url;
        }
    }
}
