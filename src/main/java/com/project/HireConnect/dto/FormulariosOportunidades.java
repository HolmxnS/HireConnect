package com.project.HireConnect.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public class FormulariosOportunidades {
    public record Cita(@NotBlank String postulacionId, @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fecha,
            @Min(15) @Max(240) int duracion, boolean virtual, @NotBlank @Size(max=500) String lugar,
            @Size(max=1500) String indicaciones, @Min(-1) long version) {}
    public record Proyecto(@NotBlank @Size(max=100) String titulo, @NotBlank @Size(max=3000) String descripcion,
            @NotBlank @Size(max=60) String categoria, @Min(1000) @Max(1000000000) long presupuesto,
            @Min(1) @Max(365) int dias, @NotBlank String modalidad) {}
    public record Propuesta(@NotBlank @Size(max=1500) String mensaje, @Min(1000) @Max(1000000000) long precio,
            @Min(1) @Max(365) int dias, @Size(max=500) String portafolio) {}
}
