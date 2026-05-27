package com.example.demo.service;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripeConfig {

    private static final Logger LOGGER = Logger.getLogger(StripeConfig.class.getName());
    private static final String SECRET;

    public static final String API_URL = "https://api.stripe.com/v1";

    static {
        String key = null;
        // 1. Environment variable
        key = System.getenv("STRIPE_SECRET_KEY");
        if (key != null && !key.isEmpty() && !key.startsWith("sk_test_XXXX")) {
            LOGGER.info("Stripe key cargada desde variable de entorno STRIPE_SECRET_KEY");
        } else {
            key = null;
            // 2. stripe.properties in current directory or user home
            String[] rutas = {
                "stripe.properties",
                System.getProperty("user.home") + File.separator + "stripe.properties",
                "stripe.properties.template"
            };
            for (String ruta : rutas) {
                key = leerDeArchivo(ruta);
                if (key != null) break;
            }
        }
        SECRET = key;
        if (SECRET == null || SECRET.isEmpty() || SECRET.startsWith("sk_test_XXXX")) {
            LOGGER.warning("STRIPE_SECRET_KEY no configurada. Copia stripe.properties.template como stripe.properties en la raiz del proyecto y editala con tu key real de Stripe.");
        }
    }

    private static String leerDeArchivo(String ruta) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(ruta)) {
            props.load(fis);
            String k = props.getProperty("stripe.secret_key");
            if (k != null && !k.isEmpty() && !k.startsWith("sk_test_XXXX")) {
                LOGGER.info("Stripe key cargada desde: " + ruta);
                return k;
            }
        } catch (IOException e) {
            // Archivo no encontrado, ignorar
        }
        return null;
    }

    public static String getSecretKey() {
        return SECRET;
    }

    public static boolean isConfigured() {
        return SECRET != null && !SECRET.isEmpty() && !SECRET.startsWith("sk_test_XXXX");
    }

    private StripeConfig() {}
}
