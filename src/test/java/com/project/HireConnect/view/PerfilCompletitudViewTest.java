package com.project.HireConnect.view;

import com.project.HireConnect.dto.CompletitudPerfil;
import com.project.HireConnect.model.Postulante;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class PerfilCompletitudViewTest {
    @Test
    void muestraElPorcentajeYLosApartadosDelPerfilGuardado() throws Exception {
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
        var response = mvc.perform(get("/preview/perfil")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        String html = response.getContentAsString();
        assertTrue(html.contains("aria-valuenow=\"20\""));
        assertTrue(html.contains("--profile-progress:20%"));
        assertTrue(html.contains("2 de 10 apartados completos"));
        assertTrue(html.contains("data-profile-part=\"foto\" data-complete=\"false\""));
        assertFalse(html.contains("Próximamente"));
        if (Boolean.getBoolean("profile.preview")) {
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "hireconnect-profile-preview.html"), html);
        }
    }
    @Controller
    static class Vista {
        @GetMapping("/preview/perfil")
        String mostrar(Model model) {
            var p = new Postulante();
            p.setId("preview");
            p.setNombre("Ana");
            p.setApellido("Pérez");
            p.setEmail("ana@example.invalid");
            model.addAttribute("postulante", p);
            model.addAttribute("completitud", CompletitudPerfil.calcular(p));
            return "postulante/perfil_postulante";
        }
    }
}
