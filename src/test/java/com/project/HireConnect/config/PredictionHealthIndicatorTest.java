package com.project.HireConnect.config;

import com.project.HireConnect.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredictionHealthIndicatorTest {
    @Test
    void servicioJavaNoEstaSanoSiFaltaElModelo() {
        var service = mock(PredictionService.class);
        var indicator = new PredictionHealthIndicator(service);
        when(service.estaListo()).thenReturn(false);
        assertEquals(Status.DOWN, indicator.health().getStatus());
        when(service.estaListo()).thenReturn(true);
        assertEquals(Status.UP, indicator.health().getStatus());
    }
}
