package com.project.HireConnect.dto;

import com.project.HireConnect.model.Postulante;
import java.util.List;
import java.util.function.Predicate;

/** Completitud de la información guardada; cada apartado aporta el mismo peso. */
public record CompletitudPerfil(List<Apartado> apartados) {
    public record Apartado(String clave, String nombre, boolean completo) {}

    public int getCompletados() {
        return (int) apartados.stream().filter(Apartado::completo).count();
    }

    public int getTotal() { return apartados.size(); }

    public int getPorcentaje() { return Math.round(100f * getCompletados() / getTotal()); }

    public static CompletitudPerfil calcular(Postulante p) {
        return new CompletitudPerfil(List.of(
            new Apartado("nombre", "Nombre completo", texto(p.getNombre()) && texto(p.getApellido())),
            new Apartado("correo", "Correo electrónico", texto(p.getEmail())),
            new Apartado("telefono", "Teléfono", texto(p.getTelefono())),
            new Apartado("titulacion", "Titulación", texto(p.getTitulacion())),
            new Apartado("nacionalidad", "Nacionalidad", texto(p.getNacionalidad())),
            new Apartado("foto", "Foto de perfil", texto(p.getFoto())),
            new Apartado("cv", "Hoja de vida", texto(p.getCv())),
            new Apartado("habilidades", "Habilidades", contiene(p.getHabilidades(), CompletitudPerfil::texto)),
            new Apartado("educacion", "Educación", contiene(p.getEducacion(), e ->
                texto(e.getInstitucion()) && texto(e.getTitulo()) && e.getFechaInicio() != null)),
            new Apartado("experiencia", "Experiencia", contiene(p.getExperiencia(), e ->
                texto(e.getEmpresa()) && texto(e.getCargo()) && e.getFechaInicio() != null))
        ));
    }

    private static boolean texto(String valor) { return valor != null && !valor.isBlank(); }

    private static <T> boolean contiene(java.util.Collection<T> valores, Predicate<T> valido) {
        return valores != null && valores.stream().anyMatch(v -> v != null && valido.test(v));
    }
}
