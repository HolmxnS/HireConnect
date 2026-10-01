package com.project.HireConnect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representa los diferentes estados por los que puede pasar una postulación.
 */
public enum PostulacionEstado {
    PENDIENTE("Pendiente"),
    EN_REVISION("En revision"),
    ENTREVISTA_PENDIENTE("Entrevista pendiente"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada");

    private final String descripcion;

    PostulacionEstado(String descripcion) {
        this.descripcion = descripcion;
    }

    @JsonValue 
    public String getDescripcion() {
        return descripcion;
    }

    @JsonCreator 
    public static PostulacionEstado fromString(String value) {
        if (value == null) return null;

        for (PostulacionEstado e : values()) {
            
            if (e.name().equalsIgnoreCase(value) || 
                e.descripcion.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de postulación desconocido: " + value);
    }

    
    public static PostulacionEstado fromDescripcion(String descripcion) {
        return fromString(descripcion);
    }
}