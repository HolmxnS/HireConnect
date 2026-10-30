package com.project.HireConnect.model;

import java.time.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "entrevistas")
public class Entrevista {
    @Id private String id;
    @Version private Long version;
    @Indexed private String empresaId;
    @Indexed private String postulanteId;
    private String empresaNombre;
    private String postulanteNombre;
    private String vacanteTitulo;
    private Instant fecha;
    private int duracion;
    private boolean virtual;
    private String lugar;
    private String indicaciones;
    private String respuesta;
    private Estado estado = Estado.POR_CONFIRMAR;
    public enum Estado {
        POR_CONFIRMAR("Por confirmar"), CONFIRMADA("Confirmada"),
        REPROGRAMACION_SOLICITADA("Reprogramación solicitada"), NO_ASISTIRA("No asistirá"), CANCELADA("Cancelada");
        private final String descripcion;
        Estado(String descripcion) { this.descripcion = descripcion; }
        public String getDescripcion() { return descripcion; }
    }
    public LocalDateTime getFechaLocal() { return fecha.atZone(ZoneId.of("America/Bogota")).toLocalDateTime(); }
    public boolean isFutura() { return fecha != null && fecha.isAfter(Instant.now()); }
}
