package com.example.demo.service;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class PayPalConfig {

    private static final Logger LOGGER = Logger.getLogger(PayPalConfig.class.getName());
    private static final String CLIENT_ID;
    private static final String SECRET;
    private static final boolean SIMULATION;

    public static final String API_URL = "https://api-m.sandbox.paypal.com";

    static {
        String cid = null, sec = null;
        cid = System.getenv("PAYPAL_CLIENT_ID");
        sec = System.getenv("PAYPAL_SECRET");
        if (cid != null && sec != null && !cid.isEmpty() && !sec.isEmpty()) {
            if ("SIMULATE".equals(cid) && "SIMULATE".equals(sec)) {
                SIMULATION = true;
                LOGGER.info("PayPal en modo SIMULACION.");
            } else {
                SIMULATION = false;
                LOGGER.info("PayPal credenciales cargadas desde variables de entorno.");
            }
        } else {
            cid = null; sec = null;
            String[] rutas = {
                "paypal.properties",
                System.getProperty("user.home") + File.separator + "paypal.properties",
                "paypal.properties.template"
            };
            for (String ruta : rutas) {
                String[] creds = leerDeArchivo(ruta);
                if (creds != null) { cid = creds[0]; sec = creds[1]; break; }
            }
            if ("SIMULATE".equals(cid) && "SIMULATE".equals(sec)) {
                SIMULATION = true;
            } else {
                SIMULATION = false;
            }
        }
        CLIENT_ID = cid;
        SECRET = sec;
        if (!isConfigured()) {
            LOGGER.warning("PayPal no configurado. Copia paypal.properties.template como paypal.properties en la raiz del proyecto y editala con tus credenciales, o usa SIMULATE/SIMULATE para modo simulacion.");
        }
    }

    private static String[] leerDeArchivo(String ruta) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(ruta)) {
            props.load(fis);
            String c = props.getProperty("paypal.client_id");
            String s = props.getProperty("paypal.secret");
            if (c != null && s != null && !c.isEmpty() && !s.isEmpty()) {
                LOGGER.info("PayPal credenciales cargadas desde: " + ruta);
                return new String[]{c, s};
            }
        } catch (IOException e) {}
        return null;
    }

    public static String getClientId() { return CLIENT_ID; }
    public static String getSecret() { return SECRET; }
    public static boolean isSimulation() { return SIMULATION; }

    public static boolean isConfigured() {
        return (CLIENT_ID != null && SECRET != null && !CLIENT_ID.isEmpty() && !SECRET.isEmpty());
    }

    private PayPalConfig() {}
}
