package com.project.HireConnect.view;

import com.project.HireConnect.enums.*;
import com.project.HireConnect.model.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

class AdminVacantesViewTest {
    @Test
    void listadoYDetalleConservanFiltrosNavegacionYAccionesSegunEstado() throws Exception {
        var templates = new ClassLoaderTemplateResolver(); templates.setPrefix("templates/");
        templates.setSuffix(".html"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var resolver = new ThymeleafViewResolver(); resolver.setTemplateEngine(engine); resolver.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(new Vista()).setViewResolvers(resolver).build();
        String list = mvc.perform(get("/preview/admin-vacantes")).andReturn().getResponse().getContentAsString();
        assertTrue(list.contains("Gestión de vacantes"));
        assertTrue(list.contains("/admin/vacantes/ver/oferta-0"));
        assertTrue(list.contains("data-vacancy-page"));
        assertTrue(list.contains("jobs-status--ACTIVA"));
        assertTrue(list.contains("jobs-status--PENDIENTE"));
        assertTrue(list.contains("jobs-status--CERRADA"));
        String detail = mvc.perform(get("/preview/admin-detalle")).andReturn().getResponse().getContentAsString();
        assertTrue(detail.contains("/admin/vacantes/cerrar/oferta-0"));
        assertTrue(detail.contains("data-admin-confirm="));
        assertTrue(detail.contains("A convenir"));
        assertTrue(detail.contains("Aún no hay postulaciones"));
        assertTrue(detail.contains("Análisis &lt;financiero&gt;"));
        String closed = mvc.perform(get("/preview/admin-detalle").param("cerrada", "true")).andReturn().getResponse().getContentAsString();
        assertFalse(closed.contains("/admin/vacantes/cerrar/"));
        if (Boolean.getBoolean("admin.jobs.preview")) {
            var dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));
            java.nio.file.Files.writeString(dir.resolve("hireconnect-admin-jobs.html"), list);
            java.nio.file.Files.writeString(dir.resolve("hireconnect-admin-job-detail.html"), detail);
        }
    }

    @Controller
    static class Vista {
        @GetMapping("/preview/admin-vacantes") String listado(Model model) {
            var jobs = List.of(job(0, VacanteEstado.ACTIVA), job(1, VacanteEstado.PENDIENTE), job(2, VacanteEstado.CERRADA));
            model.addAttribute("vacantes", jobs);
            model.addAttribute("paginaVacantes", new PageImpl<>(jobs, PageRequest.of(0,20), 41));
            model.addAttribute("resumenVacantes", Map.of("total",41L,"ACTIVA",25L,"PENDIENTE",10L,"CERRADA",6L));
            model.addAttribute("q", ""); model.addAttribute("estadoFiltro", ""); model.addAttribute("ordenFiltro", "most_recent");
            model.addAttribute("notificacion", List.of());
            return "admin/gestion_vacantes";
        }
        @GetMapping("/preview/admin-detalle") String detalle(@RequestParam(defaultValue="false") boolean cerrada, Model model) {
            model.addAttribute("vacante", job(0, cerrada ? VacanteEstado.CERRADA : VacanteEstado.ACTIVA));
            model.addAttribute("postulantes", List.of()); model.addAttribute("notificacion", List.of());
            return "admin/ver_vacante";
        }
        private Vacante job(int i, VacanteEstado state) {
            var v = new Vacante(); v.setId("oferta-"+i); v.setEstado(state);
            v.setTitulo(List.of("Asistente de tesorería", "Desarrollador Java", "Ingeniero civil").get(i));
            v.setEmpresa(new EmpresaResumen("empresa", "Felina Cúcuta Operaciones", null));
            v.setDescripcion("Análisis <financiero> y gestión de tesorería.\nBuscamos una persona organizada y con atención al detalle.");
            v.setHabilidadesRequeridas("Excel, Contabilidad, Análisis financiero");
            v.setModalidad(VacanteModalidad.Hibrido); v.setTipoContrato(VacanteTipocontrato.INDEFINIDO);
            v.setExperienciaRequerida(VacanteExperienciaRequerida.DE_2_A_3_ANIOS);
            return v;
        }
    }
}
