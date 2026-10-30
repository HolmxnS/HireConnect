package com.project.HireConnect.dto;

import java.util.List;

public record RespuestaChat(String respuesta, List<VacanteChat> vacantes, List<Accion> acciones,
        List<String> sugerencias, String modo) {
    public record VacanteChat(String titulo, String empresa, String modalidad, String salario, String motivo, String url) {}
    public record Accion(String texto, String url) {}
}
