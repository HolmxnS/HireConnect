package com.project.HireConnect.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrediccionRequestDTO {
    private int edad;
    private int experienciaAnos;
    private int coincidenciaHabilidades;
    private String nivelEducacion;
    private String modalidad;
    private String tipoContrato;
}