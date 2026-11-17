package com.project.HireConnect.controller;

import com.project.HireConnect.model.*;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.service.*;
import java.util.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostulanteModalesTest {
    final PostulanteService people = mock(PostulanteService.class);
    final Postulante person = new Postulante();
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        "test@example.invalid", null, List.of(new SimpleGrantedAuthority("ROLE_PERSONA")));

    MockMvc mvc() {
        person.setId("candidate"); person.setEmail(auth.getName());
        when(people.buscarPorEmail(auth.getName())).thenReturn(Optional.of(person));
        var jobs = mock(VacanteService.class);
        var applications = mock(PostulacionService.class);
        var companies = mock(EmpresaService.class);
        var job = new Vacante(); job.setId("job"); job.setTitulo("Java <Senior>"); job.setEstado(VacanteEstado.ACTIVA);
        when(jobs.paginaPostulante(any())).thenReturn(new PageImpl<>(List.of(job)));
        when(jobs.sectoresDisponibles()).thenReturn(List.of());
        when(applications.contarPorVacantes(anyList())).thenReturn(Map.of());
        when(companies.sectoresPorIds(anySet())).thenReturn(Map.of());
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/"); templates.setSuffix(".html"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        return MockMvcBuilders.standaloneSetup(new PostulanteController(people, applications, jobs,
            mock(HistorialService.class), companies)).setViewResolvers(views).build();
    }

    @Test void invitaUnaVezPorSesionYConfirmaLaOfertaSinPostularAlAbrir() throws Exception {
        var mvc = mvc(); var session = new MockHttpSession();
        String html = mvc.perform(get("/postulante/vacantes").principal(auth).session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("data-profile-invitation"));
        assertTrue(html.contains("No volver a mostrar este aviso"));
        assertTrue(html.contains("data-confirm-application=\"true\""));
        assertTrue(html.contains("data-application-title=\"Java &lt;Senior&gt;\""));
        assertTrue(html.contains("id=\"confirm-application-dialog\""));
        String next = mvc.perform(get("/postulante/vacantes").principal(auth).session(session))
            .andReturn().getResponse().getContentAsString();
        assertFalse(next.contains("data-profile-invitation"));
        if (Boolean.getBoolean("modals.preview")) java.nio.file.Files.writeString(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "hireconnect-modals-preview.html"), html);
    }

    @Test void respetaLaPreferenciaEnUnaNuevaSesion() throws Exception {
        var mvc = mvc(); person.setOcultarInvitacionPerfil(true);
        String html = mvc.perform(get("/postulante/vacantes").principal(auth))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("data-profile-invitation"));
    }

    @Test void noInvitaSiElPerfilEstaAlCienPorCiento() throws Exception {
        var mvc = mvc();
        person.setNombre("Ana"); person.setApellido("Pérez"); person.setTelefono("3001234567");
        person.setTitulacion("Desarrolladora"); person.setNacionalidad("Colombiana");
        person.setFoto("/foto.jpg"); person.setCv("cv.pdf"); person.setHabilidades(Set.of("Java"));
        person.setEducacion(List.of(Educacion_postulante.builder().institucion("Universidad")
            .titulo("Ingeniería").fechaInicio(LocalDate.of(2020, 1, 1)).build()));
        person.setExperiencia(List.of(Experiencia_postulante.builder().empresa("Empresa")
            .cargo("Desarrolladora").fechaInicio(LocalDate.of(2024, 1, 1)).build()));
        String html = mvc.perform(get("/postulante/vacantes").principal(auth))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("data-profile-invitation"));
    }

    @Test void guardaSoloLaPreferenciaDelUsuarioAutenticado() throws Exception {
        var mvc = mvc();
        mvc.perform(post("/postulante/perfil/invitacion").principal(auth).param("ocultar", "true").param("id", "otro"))
            .andExpect(status().isNoContent());
        verify(people).actualizarInvitacionPerfil("candidate", true);
        mvc.perform(post("/postulante/perfil/invitacion").param("ocultar", "true"))
            .andExpect(status().isUnauthorized());
        verify(people, times(1)).actualizarInvitacionPerfil(anyString(), anyBoolean());
    }
}
