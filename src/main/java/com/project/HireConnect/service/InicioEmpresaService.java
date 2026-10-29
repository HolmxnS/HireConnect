package com.project.HireConnect.service;

import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Vacante;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InicioEmpresaService {
    private final VacanteService vacanteService;
    private final PostulacionService postulacionService;

    public InicioEmpresaService(VacanteService vacanteService, PostulacionService postulacionService) {
        this.vacanteService = vacanteService;
        this.postulacionService = postulacionService;
    }

    public Inicio cargar(Empresa empresa, LocalDate hoy) {
        List<Vacante> vacantes = vacanteService.buscarPorEmpresa(empresa).stream().filter(Objects::nonNull).toList();
        Set<String> ids = vacantes.stream().map(Vacante::getId).collect(Collectors.toSet());
        List<Postulacion> postulaciones = postulacionService.buscarPorVacantes(vacantes).stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getVacante() != null && ids.contains(p.getVacante().getId()))
                .toList();
        Map<PostulacionEstado, Long> porEstado = postulaciones.stream()
                .collect(Collectors.groupingBy(p -> p.getEstado() == null ? PostulacionEstado.PENDIENTE : p.getEstado(), Collectors.counting()));
        Map<String, Long> porVacante = postulaciones.stream()
                .collect(Collectors.groupingBy(p -> p.getVacante().getId(), Collectors.counting()));
        vacantes.forEach(v -> v.setTotalPostulaciones(porVacante.getOrDefault(v.getId(), 0L)));
        Comparator<Postulacion> antiguasPrimero = Comparator.comparing(Postulacion::getFechaPostulacion,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Postulacion::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        List<Postulacion> pendientes = postulaciones.stream()
                .filter(p -> p.getEstado() == null || p.getEstado() == PostulacionEstado.PENDIENTE)
                .filter(p -> p.getPostulante() != null).sorted(antiguasPrimero).limit(5).toList();
        // Usa las fechas además del estado: una vacante vencida no debe figurar como activa.
        List<Vacante> activas = vacantes.stream().filter(v -> v.getEstado() == VacanteEstado.ACTIVA)
                .filter(v -> v.getFechaInicio() == null || !v.getFechaInicio().isAfter(hoy))
                .filter(v -> v.getFechaFin() == null || !v.getFechaFin().isBefore(hoy)).toList();
        List<Vacante> cierres = activas.stream().filter(v -> v.getFechaFin() != null && !v.getFechaFin().isAfter(hoy.plusDays(7)))
                .sorted(Comparator.comparing(Vacante::getFechaFin)).toList();
        long maximo = porEstado.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Estado> estados = java.util.Arrays.stream(PostulacionEstado.values())
                .map(e -> new Estado(e.name(), etiqueta(e), porEstado.getOrDefault(e, 0L),
                        maximo == 0 ? 0 : (int) Math.round(100.0 * porEstado.getOrDefault(e, 0L) / maximo))).toList();
        Map<LocalDate, Long> porDia = postulaciones.stream().filter(p -> p.getFechaPostulacion() != null)
                .collect(Collectors.groupingBy(Postulacion::getFechaPostulacion, Collectors.counting()));
        List<Dia> actividad = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> hoy.minusDays(29 - i))
                .map(fecha -> new Dia(fecha, porDia.getOrDefault(fecha, 0L))).toList();
        return new Inicio(vacantes.size(), activas.size(), postulaciones.size(),
                porEstado.getOrDefault(PostulacionEstado.PENDIENTE, 0L),
                porEstado.getOrDefault(PostulacionEstado.ENTREVISTA_PENDIENTE, 0L),
                porEstado.getOrDefault(PostulacionEstado.ACEPTADA, 0L), pendientes, cierres, estados, actividad);
    }

    private String etiqueta(PostulacionEstado estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendientes";
            case EN_REVISION -> "En revisión";
            case ENTREVISTA_PENDIENTE -> "Entrevista pendiente";
            case ACEPTADA -> "Aceptadas";
            case RECHAZADA -> "Rechazadas";
        };
    }

    public record Estado(String codigo, String nombre, long cantidad, int ancho) {}
    public record Dia(LocalDate fecha, long cantidad) {}
    public record Inicio(int vacantes, int activas, int total, long pendientes, long entrevistas, long aceptadas,
                        List<Postulacion> porRevisar, List<Vacante> proximosCierres, List<Estado> estados,
                        List<Dia> actividad) {}
}
