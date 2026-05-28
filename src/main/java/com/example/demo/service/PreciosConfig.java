package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PreciosConfig {

    private PreciosConfig() {}

    public static class DesgloseItem {
        public final String concepto;
        public final double valor;
        public DesgloseItem(String concepto, double valor) {
            this.concepto = concepto;
            this.valor = valor;
        }
    }

    private static final Map<String, Double> PRECIO_CATEGORIA = new HashMap<>();
    static {
        PRECIO_CATEGORIA.put("Vainilla", 15.0);
        PRECIO_CATEGORIA.put("Chocolate", 18.0);
        PRECIO_CATEGORIA.put("Fresa", 17.0);
        PRECIO_CATEGORIA.put("Zanahoria", 20.0);
        PRECIO_CATEGORIA.put("Cheesecake", 22.0);
        PRECIO_CATEGORIA.put("Ron", 19.0);
        PRECIO_CATEGORIA.put("Red Velvet", 21.0);
        PRECIO_CATEGORIA.put("Tres Leches", 18.0);
    }

    private static final Map<String, Double> PRECIO_BASE = new HashMap<>();
    static {
        PRECIO_BASE.put("Bizcocho", 0.0);
        PRECIO_BASE.put("Galleta", 2.0);
        PRECIO_BASE.put("Brownie", 3.0);
        PRECIO_BASE.put("Base Crujiente", 2.0);
        PRECIO_BASE.put("Sin Base", 0.0);
    }

    private static final Map<String, Double> PRECIO_MASA = new HashMap<>();
    static {
        PRECIO_MASA.put("Tradicional", 0.0);
        PRECIO_MASA.put("Esponjosa", 1.0);
        PRECIO_MASA.put("Hojaldrada", 2.0);
        PRECIO_MASA.put("Genovesa", 2.0);
        PRECIO_MASA.put("Queque", 1.0);
    }

    private static final Map<String, Double> PRECIO_FORMA = new HashMap<>();
    static {
        PRECIO_FORMA.put("Redonda", 0.0);
        PRECIO_FORMA.put("Cuadrada", 2.0);
        PRECIO_FORMA.put("Rectangular", 2.0);
        PRECIO_FORMA.put("Corazon", 3.0);
        PRECIO_FORMA.put("Hexagonal", 3.0);
        PRECIO_FORMA.put("Personalizada", 5.0);
    }

    private static final double PRECIO_PISO_EXTRA = 10.0;
    private static final double PRECIO_LUSTRE = 3.0;
    private static final double PRECIO_CAMUFLAJE = 4.0;
    private static final double PRECIO_FLOR = 5.0;
    private static final double PRECIO_ADORNO = 3.0;
    private static final double PRECIO_RELLENO = 4.0;

    public static double getPrecioCategoria(String categoria) {
        return PRECIO_CATEGORIA.getOrDefault(categoria, 15.0);
    }

    public static double getPrecioBase(String base) {
        return PRECIO_BASE.getOrDefault(base, 0.0);
    }

    public static double getPrecioMasa(String masa) {
        return PRECIO_MASA.getOrDefault(masa, 0.0);
    }

    public static double getPrecioForma(String forma) {
        return PRECIO_FORMA.getOrDefault(forma, 0.0);
    }

    public static double getPrecioPisoExtra() { return PRECIO_PISO_EXTRA; }
    public static double getPrecioLustre() { return PRECIO_LUSTRE; }
    public static double getPrecioCamuflaje() { return PRECIO_CAMUFLAJE; }
    public static double getPrecioFlor() { return PRECIO_FLOR; }
    public static double getPrecioAdorno() { return PRECIO_ADORNO; }
    public static double getPrecioRelleno() { return PRECIO_RELLENO; }

    public static double calcularTotal(String categoria, String base, String masa, String forma,
                                        int pisos, double libras, String lustres, String camuflajes,
                                        String flores, String adornos, String rellenos) {
        double total = getPrecioCategoria(categoria) * libras;
        total += getPrecioBase(base);
        total += getPrecioMasa(masa);
        total += getPrecioForma(forma);
        total += Math.max(0, pisos - 1) * PRECIO_PISO_EXTRA;
        total += contarItems(lustres) * PRECIO_LUSTRE;
        total += contarItems(camuflajes) * PRECIO_CAMUFLAJE;
        total += contarItems(flores) * PRECIO_FLOR;
        total += contarItems(adornos) * PRECIO_ADORNO;
        total += contarItems(rellenos) * PRECIO_RELLENO;
        return Math.round(total * 100.0) / 100.0;
    }

    public static List<DesgloseItem> calcularDesglose(String categoria, String base, String masa,
                                                       String forma, int pisos, double libras,
                                                       String lustres, String camuflajes,
                                                       String flores, String adornos, String rellenos) {
        List<DesgloseItem> items = new ArrayList<>();
        if (categoria != null && libras > 0) {
            items.add(new DesgloseItem("Categoria (" + categoria + ") x " + libras + " lb",
                getPrecioCategoria(categoria) * libras));
        }
        if (base != null) {
            double v = getPrecioBase(base);
            if (v > 0) items.add(new DesgloseItem("Base (" + base + ")", v));
        }
        if (masa != null) {
            double v = getPrecioMasa(masa);
            if (v > 0) items.add(new DesgloseItem("Masa (" + masa + ")", v));
        }
        if (forma != null) {
            double v = getPrecioForma(forma);
            if (v > 0) items.add(new DesgloseItem("Forma (" + forma + ")", v));
        }
        int pisosExtra = Math.max(0, pisos - 1);
        if (pisosExtra > 0) {
            items.add(new DesgloseItem("Pisos extra (" + pisosExtra + ")", pisosExtra * PRECIO_PISO_EXTRA));
        }
        int cLustres = contarItems(lustres);
        if (cLustres > 0) items.add(new DesgloseItem("Lustres (" + cLustres + ")", cLustres * PRECIO_LUSTRE));
        int cCamuflajes = contarItems(camuflajes);
        if (cCamuflajes > 0) items.add(new DesgloseItem("Camuflajes (" + cCamuflajes + ")", cCamuflajes * PRECIO_CAMUFLAJE));
        int cFlores = contarItems(flores);
        if (cFlores > 0) items.add(new DesgloseItem("Flores (" + cFlores + ")", cFlores * PRECIO_FLOR));
        int cAdornos = contarItems(adornos);
        if (cAdornos > 0) items.add(new DesgloseItem("Adornos (" + cAdornos + ")", cAdornos * PRECIO_ADORNO));
        int cRellenos = contarItems(rellenos);
        if (cRellenos > 0) items.add(new DesgloseItem("Rellenos (" + cRellenos + ")", cRellenos * PRECIO_RELLENO));
        return items;
    }

    private static int contarItems(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0;
        String[] partes = valor.split(",");
        int count = 0;
        for (String p : partes) {
            if (!p.trim().isEmpty()) count++;
        }
        return count;
    }
}
