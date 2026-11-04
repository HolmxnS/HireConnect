package com.project.HireConnect.controller;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.PredictionService;
import com.project.HireConnect.service.PrediccionPerfilService;
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
    private final PrediccionPerfilService perfilService;

    public PrediccionVacanteController(VacanteService vacanteService,
                                       PostulanteService postulanteService,
                                       PredictionService predictionService, PrediccionPerfilService perfilService) {
        this.vacanteService = vacanteService;
        this.postulanteService = postulanteService;
        this.predictionService = predictionService;
        this.perfilService = perfilService;
    }

    @GetMapping("/postulante/vacante/{id}/prediccion")
    public ResponseEntity<Map<String, Object>> prediccion(@PathVariable String id,
                                                          Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        Vacante vacante = vacanteService.buscarPorId(id).orElse(null);
        if (vacante == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<Postulante> postulante = postulanteService.buscarPorEmail(authentication.getName());
        if (postulante.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        PrediccionRequestDTO dto = perfilService.preparar(postulante.get(), vacante);

        return ResponseEntity.ok(predictionService.obtenerPrediccion(dto));
    }
}