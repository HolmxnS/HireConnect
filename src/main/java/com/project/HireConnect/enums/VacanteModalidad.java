package com.project.HireConnect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VacanteModalidad {
    Presencial("Presencial"),
    Remoto("Remoto"),
    Hibrido("Hibrido");

    private final String descripcion;

    VacanteModalidad(String descripcion) {
        this.descripcion = descripcion;
    }

    @JsonValue 
    public String getDescripcion() {
        return descripcion;
    }

    @JsonCreator 
    public static VacanteModalidad fromString(String value) {
        if (value == null) return null;
        
        for (VacanteModalidad m : values()) {
            if (m.name().equalsIgnoreCase(value) || 
                m.descripcion.equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Modalidad desconocida: " + value);
    }
    
    public static VacanteModalidad fromDescripcion(String descripcion) {
        return fromString(descripcion);
    }
}