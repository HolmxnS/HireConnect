package com.project.HireConnect.service;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@Service
public class PredictionService {
    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private final RestTemplate restTemplate;
    private final String pythonApiUrl;

    public PredictionService(@Value("${python.ml.api.url}") String pythonApiUrl) {
        this.pythonApiUrl = pythonApiUrl;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> obtenerPrediccion(PrediccionRequestDTO dto) {
        try {
            Map<String, Object> result = restTemplate.postForObject(pythonApiUrl, dto, Map.class);
            if (result != null && Integer.valueOf(2).equals(result.get("schemaVersion"))
                    && "logistic_regression".equals(result.get("algorithm"))
                    && result.get("probabilidad") instanceof Number probability
                    && Double.isFinite(probability.doubleValue())
                    && probability.doubleValue() >= 0 && probability.doubleValue() <= 100) {
                return result;
            }
            log.warn("La API predictiva devolvio una respuesta incompatible.");
        } catch (Exception e) {
            log.warn("API predictiva no disponible: {}", e.getClass().getSimpleName());
        }
        return Map.of("disponible", false, "compatibilidad", "No disponible");
    }
}
