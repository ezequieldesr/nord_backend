package com.nord.nord.config;

import java.util.Map;
import java.util.Set;

/** Imagens próprias de demonstração incluídas no JAR, sem dependência de CDN. */
public final class CatalogImages {
    private CatalogImages() {}
    public static final String URL_PREFIX = "/api/catalogo/imagens/";
    public static final Map<String, String> FILES_BY_PRODUCT = Map.of(
            "Camiseta Dry Trail", "camiseta-dry-trail.png",
            "Camiseta Algodão Básica", "camiseta-algodao-basica.png",
            "Camisa Polo Nord", "camisa-polo-nord.png",
            "Tênis Corrida Fjord", "tenis-corrida-fjord.png",
            "Tênis Urbano Branco", "tenis-urbano-branco.png",
            "Boné Aba Curva", "bone-aba-curva.png",
            "Boné Trucker", "bone-trucker.png",
            "Calça Jogger", "calca-jogger.png",
            "Calça Corta-vento", "calca-corta-vento.png");
    public static final Set<String> ALLOWED_FILES = Set.copyOf(FILES_BY_PRODUCT.values());
    public static String urlFor(String productName) {
        String file = FILES_BY_PRODUCT.get(productName);
        return file == null ? null : URL_PREFIX + file;
    }
}