package com.project.HireConnect.controller;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.EmpresaService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class DashboardController {

    private final PostulanteService postulanteService;
    private final EmpresaService empresaService;

    public DashboardController(PostulanteService postulanteService, EmpresaService empresaService) {
        this.postulanteService = postulanteService;
        this.empresaService = empresaService;
    }

    // Muestra una vista segun el rol del usuario
    @GetMapping("/dashboard")
    public String determineHome(Authentication authentication, RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN_PLATAFORMA")) {
                return "redirect:/admin/dashboard";
            }
        }
        Optional<Postulante> optPostulante = postulanteService.buscarPorEmail(email);
        if (optPostulante.isPresent()) {
            return "redirect:/postulante/home";
        }
        
        Optional<Empresa> optEmpresa = empresaService.buscarPorEmailEmpresa(email);
        if (optEmpresa.isPresent()) {
            return "redirect:/empresa/home";
        }
        redirectAttributes.addFlashAttribute("error", "No se encontró un perfil asociado a su cuenta.");
        
        SecurityContextHolder.getContext().setAuthentication(null); 
        return "redirect:/login"; 
    }
}