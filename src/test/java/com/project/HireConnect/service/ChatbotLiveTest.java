package com.project.HireConnect.service;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.repository.VacanteRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Explicit opt-in: calls Gemini only with a fictional professional profile. */
@EnabledIfSystemProperty(named="chat.live", matches="true")
class ChatbotLiveTest {
    @Test
    void respondeConElProveedorRealUsandoSoloUnPerfilFicticio() {
        String key = System.getenv("GEMINI_API_KEY");
        assertNotNull(key);
        var service = new ChatbotService(mock(VacanteRepository.class), key,
            System.getenv().getOrDefault("GEMINI_CHAT_MODEL", "gemini-3.5-flash-lite"),
            System.getenv().getOrDefault("GEMINI_MODEL_FALLBACK", "gemini-3.5-flash"));
        var p = new Postulante(); p.setTitulacion("Desarrollo de software"); p.setHabilidades(Set.of("Java", "SQL"));
        var result = service.conversar(p, "Hazla más corta y natural para un cargo de desarrollo backend.", List.of(
            Map.of("rol","user","texto","Ayúdame a escribir mi presentación profesional"),
            Map.of("rol","model","texto","Mi formación se centra en desarrollo de software y tengo conocimientos en Java y SQL.")), null);
        assertEquals("ia", result.modo(), "La respuesta debe venir del proveedor real en esta prueba explícita");
        assertFalse(result.respuesta().isBlank());
        assertTrue(result.respuesta().length() < 2500);
    }
}
