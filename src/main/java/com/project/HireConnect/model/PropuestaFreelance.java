package com.project.HireConnect.model;

import java.time.Instant;
import lombok.Data;

@Data
public class PropuestaFreelance {
    private String id;
    private String postulanteId;
    private String nombre;
    private String mensaje;
    private long precio;
    private int dias;
    private String portafolio;
    private Instant fecha = Instant.now();
    private Estado estado = Estado.PENDIENTE;
    public enum Estado {
        PENDIENTE("Pendiente"), SELECCIONADA("Seleccionada"), NO_SELECCIONADA("No seleccionada"), CANCELADA("Cancelada");
        private final String descripcion;
        Estado(String descripcion) { this.descripcion = descripcion; }
        public String getDescripcion() { return descripcion; }
    }
}
