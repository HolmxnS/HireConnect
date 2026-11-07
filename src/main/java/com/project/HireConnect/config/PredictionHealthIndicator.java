package com.project.HireConnect.config;

import com.project.HireConnect.service.PredictionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("prediction")
public class PredictionHealthIndicator implements HealthIndicator {
    private final PredictionService predictionService;

    public PredictionHealthIndicator(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @Override
    public Health health() {
        return predictionService.estaListo() ? Health.up().build() : Health.down().build();
    }
}
