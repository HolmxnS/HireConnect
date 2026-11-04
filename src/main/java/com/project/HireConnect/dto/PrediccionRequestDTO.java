package com.project.HireConnect.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PrediccionRequestDTO {
    private int schemaVersion = 2;
    private String fechaReferencia;
    private List<String> habilidades;
    private String habilidadesRequeridas;
    private List<Map<String, String>> experiencia;
    private boolean educacionRegistrada;
    private String experienciaRequerida;
    private String modalidad;
    private String tipoContrato;
}
