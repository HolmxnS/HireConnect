package com.project.HireConnect.view;

import com.project.HireConnect.model.Entrevista;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class EntrevistasViewTest {
    @Test
    void conservaLasAccionesSegunRolFechaYEstado() throws Exception {
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        var resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(engine);
        resolver.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(new Vista()).setViewResolvers(resolver).build();
        String postulante = mvc.perform(get("/preview/postulante")).andReturn().getResponse().getContentAsString();
        assertTrue(postulante.contains("/postulante/entrevistas/pendiente/responder"));
        assertFalse(postulante.contains("/postulante/entrevistas/pasada/responder"));
        assertFalse(postulante.contains("/postulante/entrevistas/confirmada/responder"));
        assertTrue(postulante.contains("value=\"reprogramar\""));
        assertTrue(postulante.contains("La fecha programada ya pasó"));
        assertTrue(postulante.contains("Ver indicaciones"));
        assertTrue(postulante.contains("https://meet.google.com/example"));
        String empresa = mvc.perform(get("/preview/empresa")).andReturn().getResponse().getContentAsString();
        assertTrue(empresa.contains("/empresa/entrevistas/pendiente/cancelar"));
        assertFalse(empresa.contains("/empresa/entrevistas/pasada/cancelar"));
        assertFalse(empresa.contains("/postulante/entrevistas/pendiente/responder"));
        if (Boolean.getBoolean("interviews.preview")) {
            Files.writeString(Path.of(System.getProperty("java.io.tmpdir"), "hireconnect-interviews-preview.html"), postulante);
        }
    }

    @Controller
    static class Vista {
        @GetMapping("/preview/{rol}")
        String mostrar(@org.springframework.web.bind.annotation.PathVariable String rol, Model model) {
            boolean empresa = rol.equals("empresa");
            model.addAttribute("esEmpresa", empresa);
            model.addAttribute("base", empresa ? "/empresa" : "/postulante");
            model.addAttribute("activeMenu", "entrevistas");
            var pendiente = entrevista("pendiente", "Desarrollador Java", 2);
            pendiente.setVirtual(true);
            pendiente.setLugar("https://meet.google.com/example");
            var pasada = entrevista("pasada", "Residente de obra", -2);
            pasada.setRespuesta("¿Podríamos reunirnos después de las 3:00 p. m.?");
            pasada.setEstado(Entrevista.Estado.REPROGRAMACION_SOLICITADA);
            var confirmada = entrevista("confirmada", "Auxiliar de enfermería", 4);
            confirmada.setEstado(Entrevista.Estado.CONFIRMADA);
            model.addAttribute("pagina", new PageImpl<>(List.of(pendiente, pasada, confirmada)));
            return "oportunidades/entrevistas";
        }
        private Entrevista entrevista(String id, String titulo, int dias) {
            var e = new Entrevista();
            e.setId(id);
            e.setVersion(0L);
            e.setVacanteTitulo(titulo);
            e.setEmpresaNombre("HireConnect Studio");
            e.setPostulanteNombre("Candidato de prueba");
            e.setFecha(Instant.now().plus(dias, ChronoUnit.DAYS));
            e.setDuracion(45);
            e.setLugar("Medellín, Colombia");
            e.setIndicaciones("Prepara una breve presentación y un ejemplo de tus proyectos relacionados con el puesto.");
            return e;
        }
    }
}
