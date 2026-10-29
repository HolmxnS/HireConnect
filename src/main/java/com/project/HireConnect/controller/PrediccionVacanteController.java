package com.project.HireConnect.controller;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.PredictionService;
import com.project.HireConnect.service.VacanteService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class PrediccionVacanteController {

    private final VacanteService vacanteService;
    private final PostulanteService postulanteService;
    private final PredictionService predictionService;

    public PrediccionVacanteController(VacanteService vacanteService,
                                       PostulanteService postulanteService,
                                       PredictionService predictionService) {
        this.vacanteService = vacanteService;
        this.postulanteService = postulanteService;
        this.predictionService = predictionService;
    }

    @GetMapping("/postulante/vacante/{id}/prediccion")
    public ResponseEntity<Map<String, Object>> prediccion(@PathVariable String id,
                                                          Authentication authentication) {
        Vacante vacante = vacanteService.buscarPorId(id).orElse(null);
        if (vacante == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<Postulante> postulante = postulanteService.buscarPorEmail(authentication.getName());
        if (postulante.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        PrediccionRequestDTO dto = new PrediccionRequestDTO();
        dto.setEdad(22);                    
        dto.setExperienciaAnos(2);
        dto.setCoincidenciaHabilidades(80);
        dto.setModalidad(vacante.getModalidad() != null ? vacante.getModalidad().name() : "");
        dto.setTipoContrato(vacante.getTipoContrato() != null ? vacante.getTipoContrato().name() : "");

        return ResponseEntity.ok(predictionService.obtenerPrediccion(dto));
    }
}