package com.project.HireConnect.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
                                        throws IOException, ServletException {

        String redirectURL = request.getContextPath();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            
            boolean esAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_PLATAFORMA"));
            
            boolean esEmpresa = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));
            
            boolean esPersona = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PERSONA"));

            if (esAdmin) {
                redirectURL = "/admin/dashboard";
            }
            else if (esEmpresa) {
                redirectURL = "/empresa/home"; 
            } else if (esPersona) {
                redirectURL = "/postulante/vacantes";
            } else {
                redirectURL = "/home"; 
            }
        }

        response.sendRedirect(redirectURL);
    }
}
