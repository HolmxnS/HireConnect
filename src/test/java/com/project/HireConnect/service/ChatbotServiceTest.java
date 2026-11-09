package com.project.HireConnect.service;

import com.project.HireConnect.enums.*;
import com.project.HireConnect.model.*;
import com.project.HireConnect.repository.VacanteRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ChatbotServiceTest {
    private Postulante person() {
        var p = new Postulante(); p.setTitulacion("Ingeniería de sistemas"); p.setHabilidades(Set.of("Java", "SQL"));
        p.setDescripcion("Me interesa el desarrollo backend"); p.setEmail("privado@example.invalid");
        return p;
    }
    private Map<String,String> turn(String role, String text) { return Map.of("rol", role, "texto", text); }

    @Test
    void laIARecibeElPerfilYElHistorialTambienEnUnaConsultaDePerfil() throws Exception {
        var service = new ChatbotService(mock(VacanteRepository.class), "test-key", "gemini-3.8-flash", "");
        var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "client", builder.build());
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
            .andExpect(header("x-goog-api-key", "test-key"))
            .andExpect(content().string(containsString("Me interesa el desarrollo backend")))
            .andExpect(content().string(containsString("Quiero mi primer empleo")))
            .andExpect(content().string(not(containsString("privado@example.invalid"))))
            .andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"Ya tienes Java y SQL registrados. ¿Tienes algún proyecto backend que quieras destacar?"}]}}]}
                """, MediaType.APPLICATION_JSON));
        var result = service.conversar(person(), "Revisa mi perfil", List.of(turn("user", "Quiero mi primer empleo")), null);
        assertEquals("ia", result.modo());
        assertTrue(result.respuesta().contains("backend"));
        assertTrue(result.acciones().stream().anyMatch(a -> a.url().equals("/postulante/perfil")));
        server.verify();
    }

    @Test
    void unaRespuestaDeEntrevistaNoSeConfundeConExplorarProyectos() {
        var service = new ChatbotService(mock(VacanteRepository.class), "", "gemini-3.8-flash", "");
        var history = List.of(turn("user", "Practicar una entrevista"), turn("model", "**Cuéntame sobre ti y qué te interesa de este puesto.**"));
        var result = service.conversar(person(), "Soy estudiante y en mis proyectos uso Java para organizar tareas del equipo", history, null);
        assertTrue(result.respuesta().contains("qué hiciste tú"));
        assertFalse(result.respuesta().contains("Explorar proyectos"));
        assertEquals(0, result.acciones().size());
    }

    @Test
    void unaBusquedaDeSeguimientoConservaElCargoYActualizaLaModalidad() {
        var repository = mock(VacanteRepository.class);
        var job = new Vacante(); job.setId("000000000000000000000001"); job.setEstado(VacanteEstado.ACTIVA);
        job.setTitulo("Desarrollador Java"); job.setModalidad(VacanteModalidad.Remoto);
        job.setExperienciaRequerida(VacanteExperienciaRequerida.SIN_EXPERIENCIA);
        when(repository.buscarParaAsistente(anyString(), any())).thenAnswer(invocation -> {
            String regex = invocation.getArgument(0);
            assertTrue(java.util.regex.Pattern.compile(regex, 2).matcher("Java").find());
            assertFalse(java.util.regex.Pattern.compile(regex, 2).matcher("Contabilidad").find());
            return List.of(job);
        });
        var service = new ChatbotService(repository, "", "gemini-3.8-flash", "");
        var history = List.of(turn("user", "Busca vacantes Java"), turn("model", "Encontré opciones"),
                turn("user", "y remotas"), turn("model", "Estas son remotas"));
        var result = service.conversar(person(), "sin experiencia", history, null);
        assertEquals(1, result.vacantes().size());
        assertEquals("Desarrollador Java", result.vacantes().getFirst().titulo());
    }

    @Test
    void saludoYCierreNoRepitenElMenuYElBorradorSePuedeAcortar() {
        var service = new ChatbotService(mock(VacanteRepository.class), "", "gemini-3.8-flash", "");
        assertTrue(service.conversar(person(), "Hola", List.of(), null).respuesta().startsWith("Hola."));
        assertTrue(service.conversar(person(), "Gracias", List.of(), null).sugerencias().isEmpty());
        var first = service.conversar(person(), "Ayúdame con mi presentación profesional", List.of(), null);
        var shortVersion = service.conversar(person(), "más corto", List.of(turn("user", "Ayúdame con mi presentación profesional"), turn("model", first.respuesta())), null);
        assertTrue(shortVersion.respuesta().length() < first.respuesta().length());
        assertTrue(shortVersion.respuesta().contains("Ingeniería de sistemas"));
    }

    @Test
    void siElProveedorFallaConservaLaRespuestaContextualYLasAcciones() {
        var service = new ChatbotService(mock(VacanteRepository.class), "test-key", "gemini-3.8-flash", "");
        var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "client", builder.build());
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent")).andRespond(withServiceUnavailable());
        var result = service.conversar(person(), "Revisa mi perfil", List.of(), null);
        assertEquals("orientacion", result.modo());
        assertFalse(result.acciones().isEmpty());
        assertEquals("orientacion", service.conversar(person(), "Revisa mi perfil", List.of(), null).modo());
        server.verify();
    }
    @Test
    void unTimeoutDelPrincipalPruebaElRespaldoConRespuestaRapida() {
        var service = new ChatbotService(mock(VacanteRepository.class), "test-key", "gemini-3.7-flash", "gemini-3.8-flash");
        var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "client", builder.build());
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent"))
            .andRespond(withException(new java.net.SocketTimeoutException("test timeout")));
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
            .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("low"))
            .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"¿Qué te gustaría preparar hoy?\"}]}}]}", MediaType.APPLICATION_JSON));
        assertEquals("ia", service.conversar(person(), "Hola", List.of(), null).modo());
        server.verify();
    }

}
