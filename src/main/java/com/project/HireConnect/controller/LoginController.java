package com.project.HireConnect.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.HireConnect.dto.Notificacion;
import com.project.HireConnect.dto.Paso1;
import com.project.HireConnect.dto.Paso2;
import com.project.HireConnect.dto.VacanteDestacada;
import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.enums.PostulanteGenero;
import com.project.HireConnect.enums.PostulanteTipoDocumento;
import com.project.HireConnect.enums.Rol;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.EmailService;
import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.NotificacionService;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.VacanteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final PasswordEncoder passwordEncoder;
    private final PostulanteService postulanteService;
    private final EmpresaService empresaService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final VacanteService vacanteService;

    // Cantidad de vacantes que se muestran en la landing
    private static final int MAX_VACANTES_LANDING = 3;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("vacantesDestacadas", obtenerVacantesDestacadas());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerChoice(@RequestParam(value = "oauth_pending", required = false) Boolean oauthPending,
            HttpSession session,
            Model model) {
        if (Boolean.TRUE.equals(oauthPending)) {
            String email = (String) session.getAttribute("oauth_temp_email");
            model.addAttribute("oauthPending", true);
            model.addAttribute("oauthEmail", email);
        }
        return "register";
    }

    @GetMapping("/register/postulante")
    public String registerPostulante(Model model) {
        model.addAttribute("postulante", new Postulante());
        return "postulante/registro_postulante";
    }

    @PostMapping(value = "/register/postulante", params = "step=1")
    public String paso1(
            @Validated(Paso1.class) @ModelAttribute("postulante") Postulante postulante,
            BindingResult result,
            HttpSession session,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("postulante", postulante);
            return "postulante/registro_postulante";
        }

        session.setAttribute("reg_nombre", postulante.getNombre());
        session.setAttribute("reg_apellido", postulante.getApellido());
        session.setAttribute("reg_tipoDocumento", postulante.getTipoDocumento());
        session.setAttribute("reg_identificacion", postulante.getIdentificacion());
        session.setAttribute("reg_fechaNacimiento", postulante.getFechaNacimiento());
        session.setAttribute("reg_genero", postulante.getGenero());

        model.addAttribute("postulante", postulante);
        model.addAttribute("mostrarPaso2", true);
        return "postulante/registro_postulante";
    }

    @PostMapping(value = "/register/postulante", params = "step=2")
    public String paso2(
            @Validated(Paso2.class) @ModelAttribute("postulante") Postulante postulante,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("postulante", postulante);
            model.addAttribute("mostrarPaso2", true);
            return "postulante/registro_postulante";
        }

        postulante.setNombre((String) session.getAttribute("reg_nombre"));
        postulante.setApellido((String) session.getAttribute("reg_apellido"));
        postulante.setTipoDocumento((PostulanteTipoDocumento) session.getAttribute("reg_tipoDocumento"));
        postulante.setIdentificacion((String) session.getAttribute("reg_identificacion"));
        postulante.setFechaNacimiento((LocalDate) session.getAttribute("reg_fechaNacimiento"));
        postulante.setGenero((PostulanteGenero) session.getAttribute("reg_genero"));

        if (postulanteService.buscarPorEmail(postulante.getEmail()).isPresent()) {
            result.rejectValue("email", "error.postulante", "El correo ya está registrado");
            model.addAttribute("postulante", postulante);
            model.addAttribute("mostrarPaso2", true);
            return "postulante/registro_postulante";
        }

        String codigo = String.format("%06d", new Random().nextInt(999999));

        session.setAttribute("verify_tipo", "POSTULANTE");
        session.setAttribute("verify_postulante", postulante);
        session.setAttribute("verify_codigo", codigo);
        session.setAttribute("verify_expiracion", LocalDateTime.now().plusMinutes(10));
        session.setAttribute("verify_email", postulante.getEmail());

        try {
            emailService.enviarCodigoVerificacion(postulante.getEmail(), codigo);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("postulante", postulante);
            model.addAttribute("mostrarPaso2", true);
            model.addAttribute("error", "No se pudo enviar el código. Verifica tu correo.");
            return "postulante/registro_postulante";
        }

        redirectAttributes.addFlashAttribute("emailDestino", postulante.getEmail());
        return "redirect:/register/verificar-codigo";
    }

    @GetMapping("/register/verificar-codigo")
    public String mostrarVerificacion(HttpSession session, Model model,
            @ModelAttribute("emailDestino") String emailDestino) {
        if (session.getAttribute("verify_codigo") == null) {
            String tipo = (String) session.getAttribute("verify_tipo");
            return "EMPRESA".equals(tipo)
                    ? "redirect:/register/empresa"
                    : "redirect:/register/postulante";
        }

        if (emailDestino == null || emailDestino.isBlank()) {
            model.addAttribute("emailDestino", session.getAttribute("verify_email"));
        }
        model.addAttribute("tipoRegistro", session.getAttribute("verify_tipo"));

        return "verificar_correo";
    }

    // verificar código
    @PostMapping("/register/verificar-codigo")
    public String verificarCodigo(
            @RequestParam("codigo") String codigoIngresado,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        String codigoEsperado = (String) session.getAttribute("verify_codigo");
        LocalDateTime expiracion = (LocalDateTime) session.getAttribute("verify_expiracion");
        String tipo = (String) session.getAttribute("verify_tipo");
        String email = (String) session.getAttribute("verify_email");

        model.addAttribute("emailDestino", email);
        model.addAttribute("tipoRegistro", tipo);

        if (codigoEsperado == null || tipo == null) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada. Vuelve a registrarte.");
            return "EMPRESA".equals(tipo)
                    ? "redirect:/register/empresa"
                    : "redirect:/register/postulante";
        }

        if (LocalDateTime.now().isAfter(expiracion)) {
            limpiarSesionVerificacion(session);
            model.addAttribute("error", "");
            return "verificar_correo";
        }

        if (!codigoIngresado.equals(codigoEsperado)) {
            model.addAttribute("error", "");
            return "verificar_correo";
        }

        try {
            if ("POSTULANTE".equals(tipo)) {
                Postulante postulante = (Postulante) session.getAttribute("verify_postulante");

                postulante.setRol(Rol.ROLE_PERSONA);
                postulante.setPassword(passwordEncoder.encode(postulante.getPassword()));
                postulante.setAuthProvider("LOCAL");
                postulante.setPerfilCompleto(true);

                notificacionService.crearNotificacion(postulante.getNombre(),
                        Notificacion.TipoNotificacion.POSTULANTE);
                postulanteService.guardar(postulante);

                limpiarSesionVerificacion(session);
                limpiarSesionPaso1(session);

            } else {
                Empresa empresa = (Empresa) session.getAttribute("verify_empresa");

                empresa.setRol(Rol.ROLE_EMPRESA);
                empresa.setPassword(passwordEncoder.encode(empresa.getPassword()));
                empresa.setAuthProvider("LOCAL");
                empresa.setPerfilCompleto(true);

                notificacionService.crearNotificacion(empresa.getNombreLegal(),
                        Notificacion.TipoNotificacion.EMPRESA);
                empresaService.guardar(empresa);

                limpiarSesionVerificacion(session);
                session.removeAttribute("emp_nombreLegal");
                session.removeAttribute("emp_rut");
                session.removeAttribute("emp_sector");
                session.removeAttribute("emp_ubicacion");
            }

            redirectAttributes.addFlashAttribute("exito", "¡Cuenta verificada! Ya puedes iniciar sesión.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("error", "Error al crear la cuenta: " + e.getMessage());
            return "verificar_correo";
        }
    }

    // Reenviar código de verificación
    @PostMapping("/register/reenviar-codigo")
    public String reenviarCodigo(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("verify_email");
        String tipo = (String) session.getAttribute("verify_tipo");

        if (email == null || tipo == null) {
            return "redirect:/register/postulante";
        }

        String nuevoCodigo = String.format("%06d", new Random().nextInt(999999));
        session.setAttribute("verify_codigo", nuevoCodigo);
        session.setAttribute("verify_expiracion", LocalDateTime.now().plusMinutes(10));

        try {
            emailService.enviarCodigoVerificacion(email, nuevoCodigo);
            redirectAttributes.addFlashAttribute("exito", "Nuevo código enviado a " + email);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo reenviar el código.");
        }

        redirectAttributes.addFlashAttribute("emailDestino", email);
        return "redirect:/register/verificar-codigo";
    }

    // Empresa - GET
    @GetMapping("/register/empresa")
    public String registerEmpresaForm(Model model) {
        model.addAttribute("empresa", new Empresa());
        return "empresa/registro_empresa";
    }

    // Empresa - Paso 1
    @PostMapping(value = "/empresa/crear", params = "step=1")
    public String empresaPaso1(
            @Validated(Paso1.class) @ModelAttribute("empresa") Empresa empresa,
            BindingResult result,
            HttpSession session,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("empresa", empresa);
            return "empresa/registro_empresa";
        }

        session.setAttribute("emp_nombreLegal", empresa.getNombreLegal());
        session.setAttribute("emp_rut", empresa.getRut());
        session.setAttribute("emp_sector", empresa.getSector());
        session.setAttribute("emp_ubicacion", empresa.getUbicacion());

        model.addAttribute("empresa", empresa);
        model.addAttribute("mostrarPaso2", true);
        return "empresa/registro_empresa";
    }

    // Empresa - Paso 2
    @PostMapping(value = "/empresa/crear", params = "step=2")
    public String empresaPaso2(
            @Validated(Paso2.class) @ModelAttribute("empresa") Empresa empresa,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (empresa.getConfirmPassword() != null &&
                !empresa.getConfirmPassword().equals(empresa.getPassword())) {
            result.rejectValue("confirmPassword", "error.empresa", "Las contraseñas no coinciden");
        }

        if (result.hasErrors()) {
            model.addAttribute("empresa", empresa);
            model.addAttribute("mostrarPaso2", true);
            return "empresa/registro_empresa";
        }
        empresa.setNombreLegal((String) session.getAttribute("emp_nombreLegal"));
        empresa.setRut((String) session.getAttribute("emp_rut"));
        empresa.setSector((String) session.getAttribute("emp_sector"));
        empresa.setUbicacion((String) session.getAttribute("emp_ubicacion"));

        if (empresaService.buscarPorrut(empresa.getRut()).isPresent()) {
            result.rejectValue("rut", "error.empresa", "El RUT/NIT ya está registrado");
            model.addAttribute("empresa", empresa);
            model.addAttribute("mostrarPaso2", true);
            return "empresa/registro_empresa";
        }

        if (empresaService.buscarPorEmailEmpresa(empresa.getEmail()).isPresent()) {
            result.rejectValue("email", "error.empresa", "El correo ya está registrado");
            model.addAttribute("empresa", empresa);
            model.addAttribute("mostrarPaso2", true);
            return "empresa/registro_empresa";
        }
        String codigo = String.format("%06d", new Random().nextInt(999999));

        session.setAttribute("verify_tipo", "EMPRESA");
        session.setAttribute("verify_empresa", empresa);
        session.setAttribute("verify_codigo", codigo);
        session.setAttribute("verify_expiracion", LocalDateTime.now().plusMinutes(10));
        session.setAttribute("verify_email", empresa.getEmail());

        try {
            emailService.enviarCodigoVerificacion(empresa.getEmail(), codigo);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("empresa", empresa);
            model.addAttribute("mostrarPaso2", true);
            model.addAttribute("error", "No se pudo enviar el código. Verifica tu correo.");
            return "empresa/registro_empresa";
        }

        redirectAttributes.addFlashAttribute("emailDestino", empresa.getEmail());
        return "redirect:/register/verificar-codigo";
    }

    @PostMapping("/register/oauth/postulante")
    public String crearPostulanteDesdeOAuth(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttrs) {
        try {
            String email = (String) session.getAttribute("oauth_temp_email");
            if (email == null)
                return "redirect:/register";

            if (postulanteService.buscarPorEmail(email).isPresent() ||
                    empresaService.buscarPorEmailEmpresa(email).isPresent()) {
                redirectAttrs.addFlashAttribute("error", "Este correo ya está registrado.");
                limpiarSesionOAuth(session);
                return "redirect:/login";
            }

            String nombre = (String) session.getAttribute("oauth_temp_nombre");
            String apellido = (String) session.getAttribute("oauth_temp_apellido");
            String foto = (String) session.getAttribute("oauth_temp_foto");

            Postulante nuevo = new Postulante();
            nuevo.setEmail(email);
            nuevo.setNombre(nombre != null ? nombre : "Sin nombre");
            nuevo.setApellido(apellido != null ? apellido : "");
            nuevo.setFoto(foto);
            nuevo.setPassword("");
            nuevo.setRol(Rol.ROLE_PERSONA);
            nuevo.setAuthProvider("GOOGLE");
            nuevo.setPerfilCompleto(false);

            postulanteService.guardar(nuevo);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PERSONA")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            limpiarSesionOAuth(session);

            return "redirect:/postulante/completar_perfil_postulante";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "No se pudo crear tu cuenta: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @PostMapping("/register/oauth/empresa")
    public String crearEmpresaDesdeOAuth(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttrs) {
        try {
            String email = (String) session.getAttribute("oauth_temp_email");
            if (email == null)
                return "redirect:/register";

            if (postulanteService.buscarPorEmail(email).isPresent() ||
                    empresaService.buscarPorEmailEmpresa(email).isPresent()) {
                redirectAttrs.addFlashAttribute("error", "Este correo ya está registrado.");
                limpiarSesionOAuth(session);
                return "redirect:/login";
            }

            String nombre = (String) session.getAttribute("oauth_temp_nombre");
            String foto = (String) session.getAttribute("oauth_temp_foto");

            Empresa nueva = new Empresa();
            nueva.setEmail(email);
            nueva.setNombreLegal(nombre != null ? nombre : "Sin nombre");
            nueva.setFoto(foto);
            nueva.setPassword("");
            nueva.setRol(Rol.ROLE_EMPRESA);
            nueva.setAuthProvider("GOOGLE");
            nueva.setPerfilCompleto(false);

            empresaService.guardar(nueva);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPRESA")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            limpiarSesionOAuth(session);

            return "redirect:/empresa/completar_perfil_empresa";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "No se pudo crear tu cuenta: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/postulante/completar_perfil_postulante")
    public String mostrarCompletarPerfilPostulante(Model model, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Postulante postulante = postulanteService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

        if (postulante.isPerfilCompleto()) {
            return "redirect:/postulante/home";
        }

        model.addAttribute("postulante", postulante);
        return "postulante/completar_perfil_postulante";
    }

    @PostMapping("/postulante/completar_perfil_postulante")
    public String guardarCompletarPerfilPostulante(
            @ModelAttribute("postulante") Postulante postulanteActualizado,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Postulante existente = postulanteService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

        if (existente.isPerfilCompleto()) {
            redirectAttributes.addFlashAttribute("error", "El perfil ya está completo. Contacta soporte para cambios.");
            return "redirect:/postulante/home";
        }

        if (postulanteActualizado.getTipoDocumento() == null) {
            result.rejectValue("tipoDocumento", "error", "Debe seleccionar un tipo de documento");
        }
        if (postulanteActualizado.getIdentificacion() == null ||
                !postulanteActualizado.getIdentificacion().matches("\\d{5,15}")) {
            result.rejectValue("identificacion", "error", "La identificación debe tener entre 5 y 15 números");
        }
        if (postulanteActualizado.getFechaNacimiento() == null) {
            result.rejectValue("fechaNacimiento", "error", "La fecha de nacimiento es obligatoria");
        } else if (postulanteActualizado.getFechaNacimiento().isAfter(LocalDate.now())) {
            result.rejectValue("fechaNacimiento", "error", "La fecha debe ser en el pasado");
        }
        if (postulanteActualizado.getGenero() == null) {
            result.rejectValue("genero", "error", "Debe seleccionar un género");
        }
        if (postulanteActualizado.getTelefono() == null ||
                !postulanteActualizado.getTelefono().matches("\\d{7,10}")) {
            result.rejectValue("telefono", "error", "Teléfono inválido (7-10 dígitos)");
        }

        if (result.hasErrors()) {
            model.addAttribute("postulante", postulanteActualizado);
            return "postulante/completar_perfil_postulante";
        }
        existente.setTipoDocumento(postulanteActualizado.getTipoDocumento());
        existente.setIdentificacion(postulanteActualizado.getIdentificacion());
        existente.setFechaNacimiento(postulanteActualizado.getFechaNacimiento());
        existente.setGenero(postulanteActualizado.getGenero());
        existente.setTelefono(postulanteActualizado.getTelefono());
        existente.setNacionalidad(postulanteActualizado.getNacionalidad());
        existente.setPerfilCompleto(true);

        postulanteService.guardar(existente);

        redirectAttributes.addFlashAttribute("exito", "¡Perfil de postulante completado!");
        return "redirect:/postulante/home";
    }

    @GetMapping("/empresa/completar_perfil_empresa")
    public String mostrarCompletarPerfilEmpresa(Model model, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Empresa empresa = empresaService.buscarPorEmailEmpresa(email)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (empresa.isPerfilCompleto()) {
            return "redirect:/empresa/home";
        }

        model.addAttribute("empresa", empresa);
        return "empresa/completar_perfil_empresa";
    }

    @PostMapping("/empresa/completar_perfil_empresa")
    public String guardarCompletarPerfilEmpresa(
            @ModelAttribute("empresa") Empresa empresaActualizada,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Empresa existente = empresaService.buscarPorEmailEmpresa(email)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (existente.isPerfilCompleto()) {
            redirectAttributes.addFlashAttribute("error", "El perfil ya está completo. Contacta soporte para cambios.");
            return "redirect:/empresa/home";
        }

        if (empresaActualizada.getRut() == null || !empresaActualizada.getRut().matches("\\d{7,12}-?\\d?")) {
            result.rejectValue("rut", "error", "RUT/NIT inválido");
        }
        if (empresaActualizada.getTelefono() == null || !empresaActualizada.getTelefono().matches("\\d{7,10}")) {
            result.rejectValue("telefono", "error", "Teléfono inválido (7-10 dígitos)");
        }
        if (empresaActualizada.getUbicacion() == null || empresaActualizada.getUbicacion().isBlank()) {
            result.rejectValue("ubicacion", "error", "La ubicación es obligatoria");
        }
        if (empresaActualizada.getSector() == null || empresaActualizada.getSector().isBlank()) {
            result.rejectValue("sector", "error", "El sector es obligatorio");
        }

        if (result.hasErrors()) {
            model.addAttribute("empresa", empresaActualizada);
            return "empresa/completar_perfil_empresa";
        }

        existente.setRut(empresaActualizada.getRut());
        existente.setTelefono(empresaActualizada.getTelefono());
        existente.setUbicacion(empresaActualizada.getUbicacion());
        existente.setSector(empresaActualizada.getSector());
        existente.setWebsite(empresaActualizada.getWebsite());
        existente.setDescripcion(empresaActualizada.getDescripcion());

        existente.setPerfilCompleto(true);

        empresaService.guardar(existente);

        redirectAttributes.addFlashAttribute("exito", "¡Perfil de empresa completado!");
        return "redirect:/empresa/home";
    }

    // Vacantes activas mas recientes para la landing (no se muestran las de
    // empresas suspendidas)
    private List<VacanteDestacada> obtenerVacantesDestacadas() {
        List<Vacante> activas = vacanteService.buscarPorFiltros(null, null, null, null).stream()
                .filter(v -> v != null && v.getEstado() == VacanteEstado.ACTIVA && v.getEmpresa() != null)
                .sorted(Comparator.comparing(Vacante::getFechaPublicacion,
                        Comparator.nullsLast(Comparator.<java.time.LocalDate>reverseOrder())))
                .toList();

        Map<String, Optional<Empresa>> empresas = new HashMap<>();
        List<VacanteDestacada> destacadas = new ArrayList<>();

        for (Vacante vacante : activas) {
            if (destacadas.size() >= MAX_VACANTES_LANDING) {
                break;
            }
            Optional<Empresa> empresa = empresas.computeIfAbsent(vacante.getEmpresa().getId(),
                    empresaService::obtenerPorId);
            if (empresa.isEmpty() || empresa.get().getEstado() == EmpresaEstado.SUSPENDIDA) {
                continue;
            }
            destacadas.add(new VacanteDestacada(vacante,
                    empresa.get().getNombreLegal(),
                    empresa.get().getFoto(),
                    empresa.get().getSector()));
        }
        return destacadas;
    }

    private void limpiarSesionOAuth(HttpSession session) {
        session.removeAttribute("oauth_temp_email");
        session.removeAttribute("oauth_temp_nombre");
        session.removeAttribute("oauth_temp_apellido");
        session.removeAttribute("oauth_temp_foto");
    }

    private void limpiarSesionVerificacion(HttpSession session) {
        session.removeAttribute("verify_tipo");
        session.removeAttribute("verify_postulante");
        session.removeAttribute("verify_empresa");
        session.removeAttribute("verify_codigo");
        session.removeAttribute("verify_expiracion");
        session.removeAttribute("verify_email");
    }

    private void limpiarSesionPaso1(HttpSession session) {
        session.removeAttribute("reg_nombre");
        session.removeAttribute("reg_apellido");
        session.removeAttribute("reg_tipoDocumento");
        session.removeAttribute("reg_identificacion");
        session.removeAttribute("reg_fechaNacimiento");
        session.removeAttribute("reg_genero");
    }
}