package com.project.HireConnect.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.HireConnect.repository.PasswordResetTokenRepository;
import com.project.HireConnect.security.CustomLoginSuccessHandler;
import com.project.HireConnect.security.CustomOAuth2SuccessHandler;
import com.project.HireConnect.security.CustomUsuarioDetailsService;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogoutSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeAll
    void configurarMvcConSeguridadReal() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(ConfiguracionPrueba.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @AfterAll
    void cerrarContexto() {
        if (context != null) context.close();
    }

    private MockHttpSession sesion(String rol) {
        var session = new MockHttpSession(context.getServletContext());
        var authentication = new UsernamePasswordAuthenticationToken("cuenta@example.invalid", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContext security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        return session;
    }

    private String formulario(String tipo, MockHttpSession session) throws Exception {
        String html = mvc.perform(get("/" + tipo + "/menu-prueba").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var form = Pattern.compile("<form\\b[^>]*class=\"sidebar__logout-form\"[^>]*>.*?</form>",
                Pattern.DOTALL).matcher(html);
        assertTrue(form.find(), "El menú debe contener el formulario real de cierre de sesión");
        assertFalse(Pattern.compile("<a\\b[^>]*href=\"/logout\"").matcher(html).find(),
                "Cerrar sesión no puede navegar por GET");
        String rendered = form.group();
        assertTrue(rendered.contains("method=\"post\""));
        assertTrue(rendered.contains("action=\"/logout\""));
        assertTrue(rendered.contains("type=\"submit\""));
        return rendered;
    }

    private String token(String form) {
        var input = Pattern.compile("<input\\b[^>]*name=\"_csrf\"[^>]*>").matcher(form);
        assertTrue(input.find(), "Spring Security y Thymeleaf deben insertar el token CSRF");
        var value = Pattern.compile("value=\"([^\"]+)\"").matcher(input.group());
        assertTrue(value.find(), "El token CSRF no puede estar vacío");
        return value.group(1);
    }

    @Test
    void googleSolicitaElegirCuentaYConservaParametrosOAuth() throws Exception {
        var response = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound()).andReturn().getResponse();
        var redirect = UriComponentsBuilder.fromUriString(response.getRedirectedUrl()).build();
        var parameters = redirect.getQueryParams();
        assertEquals("accounts.example.invalid", redirect.getHost());
        assertEquals("select_account", parameters.getFirst("prompt"));
        assertEquals("cliente-prueba", parameters.getFirst("client_id"));
        assertEquals("code", parameters.getFirst("response_type"));
        assertNotNull(parameters.getFirst("state"));
        assertNotNull(parameters.getFirst("redirect_uri"));
    }

    @ParameterizedTest
    @CsvSource({"postulante,PERSONA", "empresa,EMPRESA", "admin,ADMIN_PLATAFORMA"})
    void formularioRealCierraSesionYVuelveAlInicio(String tipo, String rol) throws Exception {
        var session = sesion(rol);
        String csrf = token(formulario(tipo, session));
        MvcResult result = mvc.perform(post("/logout").session(session).param("_csrf", csrf))
                .andExpect(status().isFound()).andReturn();
        assertEquals("/", result.getResponse().getRedirectedUrl());
        assertTrue(session.isInvalid(), "La sesión autenticada debe invalidarse");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        for (String name : List.of("JSESSIONID", "remember-me")) {
            var cookie = result.getResponse().getCookie(name);
            assertNotNull(cookie, "Falta eliminar la cookie " + name);
            assertEquals(0, cookie.getMaxAge());
        }
        var siguiente = mvc.perform(get("/" + tipo + "/menu-prueba"))
                .andExpect(status().isFound()).andReturn();
        assertTrue(siguiente.getResponse().getRedirectedUrl().endsWith("/login"),
                "Una petición posterior debe requerir autenticación");
    }

    @ParameterizedTest
    @CsvSource({"postulante,PERSONA", "empresa,EMPRESA", "admin,ADMIN_PLATAFORMA"})
    void cierreSinTokenSeRechazaYConservaLaSesion(String tipo, String rol) throws Exception {
        var session = sesion(rol);
        token(formulario(tipo, session));
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        assertFalse(session.isInvalid());
        var security = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(security.getAuthentication());
        mvc.perform(get("/" + tipo + "/menu-prueba").session(session)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"postulante,PERSONA", "empresa,EMPRESA", "admin,ADMIN_PLATAFORMA"})
    void abrirUrlPorGetNoCierraSesion(String tipo, String rol) throws Exception {
        var session = sesion(rol);
        formulario(tipo, session);
        mvc.perform(get("/logout").session(session));
        assertFalse(session.isInvalid(), "Navegar por GET no debe cerrar la sesión");
        mvc.perform(get("/" + tipo + "/menu-prueba").session(session)).andExpect(status().isOk());
    }

    @Test
    void lecturaPublicaNoAbrePostulacionesNiDatosPrivados() throws Exception {
        for (String path : List.of("/postulante/vacantes", "/postulante/vacantes/resultados", "/postulante/vacante/oferta"))
            mvc.perform(get(path)).andExpect(status().isNotFound()); // No controller in this security-only fixture.
        for (String path : List.of("/postulante/postular?vacanteId=oferta", "/postulante/perfil", "/postulante/historial", "/postulante/vacante/oferta/prediccion"))
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
    }

    @Controller
    static class MenuPrueba {
        @GetMapping("/{tipo}/menu-prueba")
        String menu(@PathVariable String tipo) {
            return "fragments/" + tipo + "/sidebar_" + tipo + " :: sidebar(activeMenu='vacantes')";
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class ConfiguracionPrueba {
        @Bean CustomLoginSuccessHandler loginSuccessHandler() { return mock(CustomLoginSuccessHandler.class); }
        @Bean CustomOAuth2SuccessHandler oauthSuccessHandler() { return mock(CustomOAuth2SuccessHandler.class); }
        @Bean CustomUsuarioDetailsService usuarios() { return mock(CustomUsuarioDetailsService.class); }
        @Bean PasswordResetTokenRepository passwordTokens() { return mock(PasswordResetTokenRepository.class); }
        @Bean MenuPrueba menuPrueba() { return new MenuPrueba(); }

        @Bean
        ClientRegistrationRepository clientesOAuth() {
            var registro = ClientRegistration.withRegistrationId("google")
                    .clientId("cliente-prueba").clientSecret("secreto-prueba")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("profile").authorizationUri("https://accounts.example.invalid/authorize")
                    .tokenUri("https://accounts.example.invalid/token")
                    .userInfoUri("https://accounts.example.invalid/me").userNameAttributeName("id")
                    .clientName("Google de prueba").build();
            return new InMemoryClientRegistrationRepository(registro);
        }

        @Bean
        SpringTemplateEngine templateEngine() {
            var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8"); resolver.setCacheable(false);
            var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            var resolver = new ThymeleafViewResolver(); resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
