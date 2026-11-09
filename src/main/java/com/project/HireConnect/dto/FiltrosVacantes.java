package com.project.HireConnect.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltrosVacantes {
    private String filtro;
    private String query;
    private String oferta;
    private String modalidad;
    private String sector;
    private String fechaFiltro;
    private String tipoContrato;
    private Double salarioMin;
    private Double salarioMax;
    private boolean pocosCandidatos;
    private int pagina;

    public String texto() {
        String texto = filtro != null ? filtro : query;
        return texto == null ? "" : texto.trim().substring(0, Math.min(texto.trim().length(), 100));
    }
}
