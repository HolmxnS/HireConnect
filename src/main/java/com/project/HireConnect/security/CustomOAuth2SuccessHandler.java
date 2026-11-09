package com.project.HireConnect.security;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

        private final PostulanteService postulanteService;
        private final EmpresaService empresaService;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) throws IOException {

                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");
                String nombre = oauth2User.getAttribute("given_name");
                String apellido = oauth2User.getAttribute("family_name");
                String foto = oauth2User.getAttribute("picture");

                Optional<Postulante> postulanteOpt = postulanteService.buscarPorEmail(email);
                if (postulanteOpt.isPresent()) {
                        Postulante p = postulanteOpt.get();
                        autenticarYRedirigir(request, response, p.getEmail(), "ROLE_PERSONA", p.isPerfilCompleto());
                        return;
                }

                Optional<Empresa> empresaOpt = empresaService.buscarPorEmailEmpresa(email);
                if (empresaOpt.isPresent()) {
                        Empresa e = empresaOpt.get();
                        autenticarYRedirigir(request, response, e.getEmail(), "ROLE_EMPRESA", e.isPerfilCompleto());
                        return;
                }

                // Si no existe, guardar temporalmente en sesión y redirigir a completar registro
                request.getSession().setAttribute("oauth_temp_email", email);
                request.getSession().setAttribute("oauth_temp_nombre", nombre);
                request.getSession().setAttribute("oauth_temp_apellido", apellido);
                request.getSession().setAttribute("oauth_temp_foto", foto);
                response.sendRedirect("/register?oauth_pending=true");
        }

        
        private void autenticarYRedirigir(HttpServletRequest request, HttpServletResponse response,
                        String email, String rol, boolean perfilCompleto) throws IOException {

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                email, null, Collections.singletonList(new SimpleGrantedAuthority(rol)));

                SecurityContextHolder.getContext().setAuthentication(auth);
                // Guarda la autenticación local para conservarla en la siguiente petición.
                request.getSession(true).setAttribute(
                                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                SecurityContextHolder.getContext());

                if ("ROLE_PERSONA".equals(rol)) {
                        response.sendRedirect(perfilCompleto ? request.getContextPath() + SavedVacancySearch.destination(request, response)
                                        : "/postulante/completar_perfil_postulante");
                } else {
                        response.sendRedirect(perfilCompleto ? "/empresa/home" : "/empresa/completar_perfil_empresa");
                }
        }
}
