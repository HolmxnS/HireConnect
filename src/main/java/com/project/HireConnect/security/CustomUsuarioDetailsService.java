package com.project.HireConnect.security;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Admin;
import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.AdminService;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUsuarioDetailsService implements UserDetailsService {

    private final AdminService adminService;
    private final PostulanteService postulanteService;
    private final EmpresaService empresaService;

    public CustomUsuarioDetailsService(AdminService adminService,
            PostulanteService postulanteService,
            EmpresaService empresaService) {
        this.adminService = adminService;
        this.postulanteService = postulanteService;
        this.empresaService = empresaService;
    }

    // busca usuario por email
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // admin
        Optional<Admin> adminOpt = adminService.buscarPorEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            return new User(
                    admin.getEmail(),
                    admin.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(admin.getRol().name())));
        }

        // persona
        Optional<Postulante> postulanteOpt = postulanteService.buscarPorEmail(email);
        if (postulanteOpt.isPresent()) {
            Postulante postulante = postulanteOpt.get();

            String password = (postulante.getPassword() == null || postulante.getPassword().isBlank())
                    ? "{noop}"
                    : postulante.getPassword();

            return new User(
                    postulante.getEmail(),
                    password,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PERSONA")));
        }

        // empresa
        Optional<Empresa> empresaOpt = empresaService.buscarPorEmailEmpresa(email);
        if (empresaOpt.isPresent()) {
            Empresa empresa = empresaOpt.get();
            return new User(
                    empresa.getEmail(),
                    empresa.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPRESA")));
        }
        throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
    }
}