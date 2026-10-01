package com.project.HireConnect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VacanteExperienciaRequerida {
    SIN_EXPERIENCIA("Sin experiencia"),
    DE_1_A_2_ANIOS("1-2 años"),
    DE_2_A_3_ANIOS("2-3 años"),
    MAS_DE_3_ANIOS("+3 años");

    private final String descripcion;

    private VacanteExperienciaRequerida(String descripcion) {
        this.descripcion = descripcion;
    }

    @JsonValue
    public String getDescripcion() {
        return descripcion;
    }
    
    @JsonCreator
    public static VacanteExperienciaRequerida fromString(String value) {
        if (value == null) return null;

        for (VacanteExperienciaRequerida v : values()) {
            
            if (v.name().equalsIgnoreCase(value) || 
                v.descripcion.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Valor de experiencia desconocido: " + value);
    }

    
    public static VacanteExperienciaRequerida fromDescripcion(String descripcion) {
        return fromString(descripcion);
    }
}