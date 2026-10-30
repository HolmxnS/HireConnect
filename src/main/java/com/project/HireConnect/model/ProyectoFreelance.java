package com.project.HireConnect.model;

import java.time.*;
import java.util.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "proyectos_freelance")
public class ProyectoFreelance {
    @Id private String id;
    @Version private Long version;
    @Indexed private String empresaId;
    private String empresaNombre;
    private String titulo;
    private String descripcion;
    private String categoria;
    private long presupuesto;
    private int dias;
    private String modalidad;
    private Instant fecha = Instant.now();
    private Estado estado = Estado.ABIERTO;
    // Las propuestas y la selección se guardan juntas para evitar adjudicar un proyecto dos veces.
    private List<PropuestaFreelance> propuestas = new ArrayList<>();
    public LocalDate getFechaLocal() {
        return fecha == null ? null : fecha.atZone(ZoneId.of("America/Bogota")).toLocalDate();
    }
    public enum Estado {
        ABIERTO("Abierto"), EN_CURSO("En curso"), FINALIZADO("Finalizado"), CANCELADO("Cancelado");
        private final String descripcion;
        Estado(String descripcion) { this.descripcion = descripcion; }
        public String getDescripcion() { return descripcion; }
    }
}
