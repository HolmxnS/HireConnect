package com.project.HireConnect.view;

import com.project.HireConnect.controller.PostulanteController;
import com.project.HireConnect.dto.FiltrosVacantes;
import com.project.HireConnect.enums.*;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class PublicVacanciesViewTest {
    @Test
    void visitanteBuscaAbreDetallesYNoRecibeFuncionesPersonales() throws Exception {
        var people = mock(PostulanteService.class);
        var jobs = mock(VacanteService.class);
        var applications = mock(PostulacionService.class);
        var companies = mock(EmpresaService.class);
        var job = new Vacante();
        job.setId("vacante-publica"); job.setTitulo("Desarrollador Java");
        job.setEstado(VacanteEstado.ACTIVA); job.setModalidad(VacanteModalidad.Remoto);
        job.setTipoContrato(VacanteTipocontrato.INDEFINIDO);
        job.setExperienciaRequerida(VacanteExperienciaRequerida.DE_1_A_2_ANIOS);
        when(jobs.paginaPostulante(any())).thenReturn(new PageImpl<>(List.of(job)));
        when(jobs.sectoresDisponibles()).thenReturn(List.of("Tecnología"));
        when(jobs.buscarPorId(job.getId())).thenReturn(Optional.of(job));
        when(applications.contarPorVacantes(anyList())).thenReturn(Map.of());
        when(companies.sectoresPorIds(anySet())).thenReturn(Map.of());
        var controller = new PostulanteController(people, applications, jobs, mock(HistorialService.class), companies);
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/"); templates.setSuffix(".html"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var resolver = new ThymeleafViewResolver(); resolver.setTemplateEngine(engine); resolver.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(resolver).build();
        var response = mvc.perform(get("/postulante/vacantes").param("filtro", "Java")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        String html = response.getContentAsString();
        assertTrue(html.contains("vacancy-browser--guest"));
        assertTrue(html.contains("Desarrollador Java"));
        assertTrue(html.contains("value=\"Java\""));
        assertTrue(html.contains("/postulante/postular?vacanteId=vacante-publica"));
        assertFalse(html.contains("data-prediction hidden"));
        assertFalse(html.contains("sidebar__logout-form"));
        assertFalse(html.contains("hc-chat-root"));
        verify(jobs).paginaPostulante(argThat(f -> f.texto().equals("Java")));
        var ajax = mvc.perform(get("/postulante/vacantes/resultados").param("filtro", "Java")).andReturn().getResponse();
        assertEquals(200, ajax.getStatus());
        assertTrue(ajax.getContentAsString().contains("Desarrollador Java"));
        var detail = mvc.perform(get("/postulante/vacante/vacante-publica")).andReturn().getResponse();
        assertEquals(200, detail.getStatus());
        assertFalse(detail.getContentAsString().contains("hc-chat-root"));
        verifyNoInteractions(people);
        if (Boolean.getBoolean("public.preview")) java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "hireconnect-public-preview.html"), html);
    }
}
