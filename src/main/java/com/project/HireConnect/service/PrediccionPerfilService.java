package com.project.HireConnect.service;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class PrediccionPerfilService {
    public PrediccionRequestDTO preparar(Postulante persona, Vacante vacante) {
        var dto = new PrediccionRequestDTO();
        dto.setFechaReferencia(LocalDate.now(ZoneId.of("America/Bogota")).toString());
        dto.setHabilidades(persona.getHabilidades() == null ? List.of() : List.copyOf(persona.getHabilidades()));
        dto.setHabilidadesRequeridas(vacante.getHabilidadesRequeridas());
        dto.setEducacionRegistrada(persona.getEducacion() != null && !persona.getEducacion().isEmpty());
        dto.setExperiencia(persona.getExperiencia() == null ? List.of() : persona.getExperiencia().stream()
                .filter(java.util.Objects::nonNull).map(exp -> {
                    Map<String, String> fechas = new HashMap<>();
                    if (exp.getFechaInicio() != null) fechas.put("fechaInicio", exp.getFechaInicio().toString());
                    if (exp.getFechaFin() != null) fechas.put("fechaFin", exp.getFechaFin().toString());
                    return fechas;
                }).toList());
        dto.setExperienciaRequerida(vacante.getExperienciaRequerida() == null ? null : vacante.getExperienciaRequerida().name());
        dto.setModalidad(vacante.getModalidad() == null ? null : vacante.getModalidad().name());
        dto.setTipoContrato(vacante.getTipoContrato() == null ? null : vacante.getTipoContrato().name());
        return dto;
    }
}
