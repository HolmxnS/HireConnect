package com.project.HireConnect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VacanteTipocontrato {

    TIEMPO_COMPLETO("Tiempo completo"),
    TIEMPO_PARCIAL("Tiempo parcial"),
    INDEFINIDO("Indefinido");

    private final String descripcion;

    VacanteTipocontrato(String descripcion) {
        this.descripcion = descripcion;
    }

    @JsonValue 
    public String getDescripcion() {
        return descripcion;
    }

    @JsonCreator 
    public static VacanteTipocontrato fromString(String value) {
        if (value == null) return null;

        for (VacanteTipocontrato t : values()) {
            if (t.name().equalsIgnoreCase(value) || 
                t.descripcion.equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de contrato desconocido: " + value);
    }

    // Mantenemos este método para no romper lógica existente en otros servicios
    public static VacanteTipocontrato fromDescripcion(String descripcion) {
        return fromString(descripcion);
    }
}