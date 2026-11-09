package com.project.HireConnect.security;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.PostulanteService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LandingSearchLoginTest {
    private MockHttpServletRequest search(String context, String path, String text) {
        var original = new MockHttpServletRequest("GET", context + path);
        original.setContextPath(context);
        original.setServerName("untrusted.example");
        if (text != null) original.setParameter("filtro", text);
        new HttpSessionRequestCache().saveRequest(original, new MockHttpServletResponse());
        var login = new MockHttpServletRequest("POST", context + "/login");
        login.setContextPath(context);
        login.setSession(original.getSession());
        return login;
    }

    @Test
    void elLoginLocalConservaElTextoYNoElHostDeLaPeticion() throws Exception {
        var request = search("", "/postulante/vacantes", " C++ & SQL ");
        var response = new MockHttpServletResponse();
        new CustomLoginSuccessHandler().onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken("persona", null, List.of(new SimpleGrantedAuthority("ROLE_PERSONA"))));
        assertEquals("/postulante/vacantes?filtro=C%2B%2B%20%26%20SQL", response.getRedirectedUrl());
        assertNull(new HttpSessionRequestCache().getRequest(request, response));
    }

    @Test
    void loginRegresaALaVacanteSinCrearUnaPostulacionAutomaticamente() {
        var original = new MockHttpServletRequest("GET", "/postulante/postular");
        original.setParameter("vacanteId", "vacante-elegida");
        new HttpSessionRequestCache().saveRequest(original, new MockHttpServletResponse());
        var login = new MockHttpServletRequest("POST", "/login");
        login.setSession(original.getSession());
        assertEquals("/postulante/vacante/vacante-elegida", SavedVacancySearch.destination(login, new MockHttpServletResponse()));
    }

    @Test
    void soportaContextoYBusquedaVacia() {
        var response = new MockHttpServletResponse();
        assertEquals("/postulante/vacantes?filtro=Java", SavedVacancySearch.destination(
                search("/hireconnect", "/postulante/vacantes/listar", "Java"), response));
        assertEquals("/postulante/vacantes", SavedVacancySearch.destination(
                search("", "/postulante/vacantes", "  "), response));
    }

    @Test
    void ignoraOtrosDestinosYConservaElInicioDeEmpresa() throws Exception {
        var request = search("", "/empresa/home", "Java");
        assertEquals("/postulante/vacantes", SavedVacancySearch.destination(request, new MockHttpServletResponse()));
        request = search("", "/postulante/vacantes", "Java");
        var response = new MockHttpServletResponse();
        new CustomLoginSuccessHandler().onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken("empresa", null, List.of(new SimpleGrantedAuthority("ROLE_EMPRESA"))));
        assertEquals("/empresa/home", response.getRedirectedUrl());
    }

    @Test
    void googleConservaLaBusquedaParaUnCandidatoConPerfilCompleto() throws Exception {
        var people = mock(PostulanteService.class);
        var companies = mock(EmpresaService.class);
        var person = new Postulante();
        person.setEmail("persona@example.invalid");
        person.setPerfilCompleto(true);
        when(people.buscarPorEmail(person.getEmail())).thenReturn(Optional.of(person));
        var request = search("", "/postulante/vacantes", "enfermería");
        var response = new MockHttpServletResponse();
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_PERSONA")),
                Map.of("email", person.getEmail()), "email");
        new CustomOAuth2SuccessHandler(people, companies).onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken(principal, null));
        assertEquals("/postulante/vacantes?filtro=enfermer%C3%ADa", response.getRedirectedUrl());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
