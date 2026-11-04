package com.project.HireConnect.service;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;

class PredictionServiceTest {
    @Test
    void rechazaLaFormulaAnteriorYAceptaCeroDelModeloNuevo() {
        var service = new PredictionService("http://localhost/predict");
        var server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, "restTemplate")).build();
        server.expect(requestTo("http://localhost/predict"))
                .andRespond(withSuccess("{\"probabilidad\":65,\"compatibilidad\":\"Media\"}", MediaType.APPLICATION_JSON));
        assertEquals(false, service.obtenerPrediccion(new PrediccionRequestDTO()).get("disponible"));
        server.verify();
        server.reset();
        server.expect(requestTo("http://localhost/predict"))
                .andRespond(withSuccess("{\"schemaVersion\":2,\"algorithm\":\"logistic_regression\",\"probabilidad\":0,\"compatibilidad\":\"Baja\"}", MediaType.APPLICATION_JSON));
        assertEquals(0, service.obtenerPrediccion(new PrediccionRequestDTO()).get("probabilidad"));
        server.verify();
    }

    @Test
    void servicioNoDisponibleNoInventaUnaProbabilidad() {
        var service = new PredictionService("http://localhost/predict");
        var server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, "restTemplate")).build();
        server.expect(requestTo("http://localhost/predict")).andRespond(withServiceUnavailable());
        var result = service.obtenerPrediccion(new PrediccionRequestDTO());
        assertEquals(false, result.get("disponible"));
        assertFalse(result.containsKey("probabilidad"));
        server.verify();
    }
}
