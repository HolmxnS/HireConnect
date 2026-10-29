package com.project.HireConnect.service;

import java.net.URI;
import java.util.*;

public final class PublicacionesSociales {
    private PublicacionesSociales() { }

    public static Map<String, String> interpretar(String enlace) {
        try {
            URI uri = URI.create(enlace.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1 || enlace.length() > 2048) throw new IllegalArgumentException();
            String red, base;
            if ((host.equals("instagram.com") || host.equals("www.instagram.com")) && path.matches("/(p|reel)/[A-Za-z0-9_-]+/?")) { red = "Instagram"; base = "https://www.instagram.com"; }
            else if ((host.equals("threads.com") || host.equals("www.threads.com") || host.equals("threads.net") || host.equals("www.threads.net")) && path.matches("/(@[A-Za-z0-9_.]+/post|t)/[A-Za-z0-9_-]+/?")) { red = "Threads"; base = "https://www.threads.com"; }
            else if ((host.equals("x.com") || host.equals("www.x.com") || host.equals("twitter.com") || host.equals("www.twitter.com")) && path.matches("/[A-Za-z0-9_]+/status/[0-9]+/?")) { red = "X"; base = "https://twitter.com"; }
            else if ((host.equals("facebook.com") || host.equals("www.facebook.com")) && path.matches("/[A-Za-z0-9.]+/posts/[A-Za-z0-9]+/?")) { red = "Facebook"; base = "https://www.facebook.com"; }
            else throw new IllegalArgumentException();
            return Map.of("red", red, "url", base + path);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Usa el enlace https:// de una publicación de Instagram, Threads, Facebook o X; no el enlace del perfil.");
        }
    }

    public static List<String> normalizar(List<String> enlaces) {
        if (enlaces == null) return List.of();
        if (enlaces.size() > 6) throw new IllegalArgumentException("Puedes destacar hasta 6 publicaciones.");
        Set<String> resultado = new LinkedHashSet<>();
        for (String enlace : enlaces) if (enlace != null && !enlace.isBlank()) resultado.add(interpretar(enlace).get("url"));
        return new ArrayList<>(resultado);
    }

    public static List<Map<String, String>> mostrar(List<String> enlaces) {
        List<Map<String, String>> resultado = new ArrayList<>();
        if (enlaces != null) for (String enlace : enlaces.stream().limit(6).toList()) {
            if (enlace == null || enlace.isBlank()) continue;
            try { resultado.add(interpretar(enlace)); } catch (IllegalArgumentException ignored) { }
        }
        return resultado;
    }
}
