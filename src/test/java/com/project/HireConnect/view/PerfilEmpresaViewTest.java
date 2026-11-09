package com.project.HireConnect.view;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class PerfilEmpresaViewTest {
    @Test
    void conservaLaPortadaConOSinImagenYElEnlaceWeb() throws Exception {
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
        String html = mvc.perform(get("/preview/empresa-publica")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("class=\"company-visitor__cover\""));
        assertFalse(html.contains("alt=\"Portada de la empresa\""));
        assertTrue(html.contains("Felina Cúcuta Operaciones"));
        assertTrue(html.contains("href=\"https://example.org\""));
        assertTrue(html.contains("No hay vacantes abiertas por ahora."));
        String conPortada = mvc.perform(get("/preview/empresa-publica").param("portada", "true"))
            .andReturn().getResponse().getContentAsString();
        assertTrue(conPortada.contains("class=\"company-visitor__cover\""));
        assertTrue(conPortada.contains("src=\"/portada.jpg\""));
        if (Boolean.getBoolean("company.preview")) {
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "hireconnect-company-preview.html"), html);
        }
    }
    @Controller
    static class Vista {
        @GetMapping("/preview/empresa-publica")
        String mostrar(@RequestParam(defaultValue="false") boolean portada, Model model) {
            model.addAttribute("empresa", Map.of("nombre", "Felina Cúcuta Operaciones", "sector", "Finanzas",
                "ubicacion", "Manizales", "foto", "", "portada", portada ? "/portada.jpg" : "",
                "website", "https://example.org", "descripcion", "Empresa de finanzas ubicada en Manizales. Nuestro equipo desarrolla soluciones para clientes de la región con enfoque en calidad, colaboración y crecimiento profesional."));
            model.addAttribute("redes", Map.of());
            model.addAttribute("empleos", List.of());
            return "postulante/perfil_empresa";
        }
    }
}
