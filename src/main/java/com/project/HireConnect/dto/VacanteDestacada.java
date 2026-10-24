package com.project.HireConnect.dto;

import com.project.HireConnect.model.Vacante;

public class VacanteDestacada {

    private final Vacante vacante;
    private final String empresaNombre;
    private final String empresaFoto;
    private final String empresaSector;

    public VacanteDestacada(Vacante vacante, String empresaNombre, String empresaFoto, String empresaSector) {
        this.vacante = vacante;
        this.empresaNombre = empresaNombre;
        this.empresaFoto = empresaFoto;
        this.empresaSector = empresaSector;
    }

    public Vacante getVacante() {
        return vacante;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public String getEmpresaFoto() {
        return empresaFoto;
    }

    public String getEmpresaSector() {
        return empresaSector;
    }
}