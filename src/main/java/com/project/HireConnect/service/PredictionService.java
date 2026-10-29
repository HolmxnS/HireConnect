package com.project.HireConnect.service;


import com.project.HireConnect.dto.PrediccionRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.ml.api.url}")
    private String pythonApiUrl;

    public Map<String, Object> obtenerPrediccion(PrediccionRequestDTO dto) {
        try {
            return restTemplate.postForObject(pythonApiUrl, dto, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("probabilidad", 0.0);
            fallback.put("compatibilidad", "No disponible");
            return fallback;
        }
    }
}
