package com.project.HireConnect.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.PostulacionService;
import com.project.HireConnect.service.VacanteService;

@Controller
@RequestMapping("/empresa")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final PostulacionService postulacionService;
    private final VacanteService vacanteService;

    public EmpresaController(EmpresaService empresaService,
            PostulacionService postulacionService,
            VacanteService vacanteService) {
        this.empresaService = empresaService;
        this.postulacionService = postulacionService;
        this.vacanteService = vacanteService;
    }

    private Empresa obtenerEmpresaLogueada(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        String email = authentication.getName();

        return empresaService.buscarPorEmailEmpresa(email)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada para el email: " + email));
    }

    @GetMapping("/home")
    public String homeEmpresa(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = obtenerEmpresaLogueada(auth);

            List<Vacante> vacantes = vacanteService.buscarPorEmpresa(empresa);

            model.addAttribute("activeMenu", "home");
            model.addAttribute("empresa", empresa);
            model.addAttribute("vacantes", vacantes);

            model.addAttribute("faltantes", empresa.obtenerCamposFaltantes());

            return "empresa/home_empresa";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Sesión expirada o perfil no encontrado. Por favor, inicie sesión.");
            return "redirect:/login";
        }
    }

    @GetMapping("/vacante")
    public String vacantes(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = obtenerEmpresaLogueada(auth);

            List<Vacante> vacantes = vacanteService.buscarPorEmpresa(empresa);

            model.addAttribute("activeMenu", "vacantes");
            model.addAttribute("empresa", empresa);
            model.addAttribute("vacantes", vacantes);

            return "empresa/vacantes_empresa";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Sesión expirada o perfil no encontrado. Por favor, inicie sesión.");
            return "redirect:/login";
        }
    }

    @GetMapping("/postulaciones")
    public String mostrarMenuPostulaciones(Model model, Authentication auth) {
        Empresa empresa = obtenerEmpresaLogueada(auth);
        List<Vacante> vacantesActivas = vacanteService.buscarPorEmpresa(empresa);

        vacantesActivas.forEach(v -> v.setTotalPostulaciones(postulacionService.contarPorVacante(v)));

        model.addAttribute("empresa", empresa);
        model.addAttribute("vacantesActivas", vacantesActivas);
        return "empresa/listado_postulaciones";
    }

    @GetMapping("/vacante/{id}")
    public String verDetalleVacanteEmpresa(@PathVariable("id") String idVacante,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Empresa> optEmpresa = empresaService
                .buscarPorEmailEmpresa(authentication.getName());
        if (optEmpresa.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada.");
            return "redirect:/login";
        }
        Empresa empresaSesion = optEmpresa.get();
        Optional<Vacante> optVacante = vacanteService.buscarPorId(idVacante);
        if (optVacante.isEmpty()) {
            model.addAttribute("error", "Vacante no encontrada con ID: " + idVacante);
            return "error";
        }

        Vacante vacante = optVacante.get();
        if (!vacante.getEmpresa().getId().equals(empresaSesion.getId())) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado a esta vacante.");
            return "redirect:/empresa/vacantes";
        }

        model.addAttribute("vacante", vacante);
        model.addAttribute("empresa", empresaSesion);

        return "empresa/ver_detalle_vacante";
    }

    @GetMapping("/postulaciones/lista/{vacanteId}")
    public String listarPostulacionesPorVacante(@PathVariable String vacanteId, Model model,
            Authentication auth) {
        Empresa empresa = obtenerEmpresaLogueada(auth);
        Vacante vacanteActual = vacanteService.buscarPorId(vacanteId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!vacanteActual.getEmpresa().getId().equals(empresa.getId())) {
            return "redirect:/empresa/postulaciones?error=acceso_denegado";
        }

        List<Postulacion> postulaciones = postulacionService.buscarPorVacante(vacanteActual);
        List<Vacante> vacantesActivas = vacanteService.buscarPorEmpresa(empresa);

        model.addAttribute("empresa", empresa);
        model.addAttribute("vacantesActivas", vacantesActivas);
        model.addAttribute("vacanteActual", vacanteActual);
        model.addAttribute("vacanteSeleccionadaId", vacanteId);
        model.addAttribute("postulaciones", postulaciones);

        return "empresa/listado_postulaciones";
    }

    @GetMapping("/postulacion/{id}")
    public String verDetallePostulacion(@PathVariable("id") String idPostulacion,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        Postulacion postulacion = postulacionService.buscarPorId(idPostulacion)
                .orElse(null);

        Empresa empresaAuth = obtenerEmpresaLogueada(auth);

        if (postulacion == null) {
            redirectAttributes.addFlashAttribute("error", "Postulación no encontrada.");
            return "redirect:/empresa/home";
        }
        if (!postulacion.getVacante().getEmpresa().getId().equals(empresaAuth.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "Acceso denegado. Esta postulación no es para una vacante de tu empresa.");
            return "redirect:/empresa/home";
        }

        model.addAttribute("postulacion", postulacion);
        model.addAttribute("postulante", postulacion.getPostulante());
        model.addAttribute("vacante", postulacion.getVacante());
        model.addAttribute("empresa", empresaAuth);
        return "empresa/detalle_postulacion";
    }

    @PostMapping("/actualizar-postulacion")
    public String actualizarPostulacion(
            @RequestParam("id") String id,
            @RequestParam("estado") String estado,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            obtenerEmpresaLogueada(auth);

            postulacionService.actualizarEstado(id, estado);

            redirectAttributes.addFlashAttribute("exito",
                    "El estado de la postulación se ha actualizado a '" + estado + "' correctamente.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Sesión de empresa no encontrada. Por favor, inicie sesión.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al actualizar el estado de la postulación: " + e.getMessage());
        }
        return "redirect:/empresa/postulaciones";
    }

    @GetMapping("/editar")
    public String mostrarFormularioEdicionEmpresa(
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = obtenerEmpresaLogueada(auth);

            model.addAttribute("activeMenu", "perfil");
            model.addAttribute("empresa", empresa);
            model.addAttribute("publicacionesEmpresa", com.project.HireConnect.service.PublicacionesSociales.mostrar(empresa.getPublicacionesSociales()));
            model.addAttribute("empleosEmpresa", vacanteService.buscarPorEmpresa(empresa).stream()
                    .sorted(java.util.Comparator.comparing(Vacante::getFechaPublicacion,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                            .thenComparing(Vacante::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList());
            return "empresa/editar_perfil";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el perfil.");
            return "redirect:/empresa/home";
        }
    }

    private static String validarRedSocial(String value, String nombre, String... dominios) {
        if (value == null || value.isBlank()) return null;
        String url = value.trim();
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            boolean dominioValido = host != null && java.util.Arrays.stream(dominios)
                    .anyMatch(d -> host.equalsIgnoreCase(d) || host.toLowerCase(java.util.Locale.ROOT).endsWith("." + d));
            if (url.length() <= 2048 && "https".equalsIgnoreCase(uri.getScheme()) && dominioValido
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)) return url;
        } catch (IllegalArgumentException ignored) { }
        throw new IllegalArgumentException("El enlace de " + nombre + " debe empezar por https:// y pertenecer a esa red social.");
    }

    @PostMapping("/guardar-perfil")
    public String guardarPerfil(
            @ModelAttribute("empresa") Empresa empresaForm,
            Authentication auth,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        try {

            if (result.hasErrors())
                return "redirect:/empresa/editar";

            Empresa oldEmpresa = obtenerEmpresaLogueada(auth);
            empresaForm.setId(oldEmpresa.getId());
            empresaForm.setEmail(oldEmpresa.getEmail());
            empresaForm.setRol(oldEmpresa.getRol());
            empresaForm.setPortada(oldEmpresa.getPortada());

            if (empresaForm.getPassword() == null || empresaForm.getPassword().isEmpty()) {
                empresaForm.setPassword(oldEmpresa.getPassword());
            } else {

                empresaForm.setPassword(oldEmpresa.getPassword());
            }

            empresaForm.setPublicacionesSociales(com.project.HireConnect.service.PublicacionesSociales.normalizar(empresaForm.getPublicacionesSociales()));
            empresaForm.setThreads(validarRedSocial(empresaForm.getThreads(), "Threads", "threads.com", "threads.net"));
            empresaForm.setInstagram(validarRedSocial(empresaForm.getInstagram(), "Instagram", "instagram.com"));
            empresaForm.setFacebook(validarRedSocial(empresaForm.getFacebook(), "Facebook", "facebook.com"));
            empresaForm.setX(validarRedSocial(empresaForm.getX(), "X", "x.com", "twitter.com"));
            empresaService.guardar(empresaForm);

            redirectAttributes.addFlashAttribute("success", "Perfil de la empresa actualizado exitosamente.");
            return "redirect:/empresa/editar";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar los cambios: " + e.getMessage());
            return "redirect:/empresa/editar";
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.HireConnect.service.FotoEmpresaService fotoEmpresaService;

    @PostMapping("/subirFoto")
    public String subirFoto(@RequestParam("id") String id,
            @RequestParam("file") MultipartFile file, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";
        try {
            fotoEmpresaService.guardar(authentication.getName(), file);
            redirectAttributes.addFlashAttribute("exito", "Foto actualizada.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos guardar la foto. Inténtalo de nuevo.");
        }
        return "redirect:/empresa/editar";
    }

    @GetMapping("/eliminarFoto")
    public String eliminarFoto(@RequestParam("id") String id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";
        try {
            fotoEmpresaService.eliminar(authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Foto eliminada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos quitar la foto. Inténtalo de nuevo.");
        }
        return "redirect:/empresa/editar";
    }

    @GetMapping("/configuracion")
    public String mostrarConfiguracion(Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = obtenerEmpresaLogueada(authentication);

            model.addAttribute("empresa", empresa);
            model.addAttribute("activeMenu", "configuracion");

            return "empresa/configuracion_empresa";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada o empresa no encontrada.");
            return "redirect:/login";
        }
    }

    @GetMapping("/configuracion/datos")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> descargarDatos(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        Optional<Empresa> actual = empresaService.buscarPorEmailEmpresa(authentication.getName());
        if (actual.isEmpty()) return ResponseEntity.status(401).build();
        Empresa p = actual.get();
        // Exporta únicamente información de perfil, sin credenciales ni referencias internas.
        java.util.Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("nombreLegal", p.getNombreLegal());
        datos.put("email", p.getEmail());
        datos.put("telefono", p.getTelefono());
        datos.put("rut", p.getRut());
        datos.put("sector", p.getSector());
        datos.put("ubicacion", p.getUbicacion());
        datos.put("website", p.getWebsite());
        datos.put("threads", p.getThreads());
        datos.put("publicacionesSociales", p.getPublicacionesSociales());
        datos.put("instagram", p.getInstagram());
        datos.put("facebook", p.getFacebook());
        datos.put("x", p.getX());
        datos.put("descripcion", p.getDescripcion());
        datos.put("fechaRegistro", p.getFechaRegistro());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hireconnect-empresa.json")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(datos);
    }

    @PostMapping("/cambiarPassword")
    public String cambiarPassword(
            @RequestParam("id") String empresaId,
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevaPassword") String nuevaPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Empresa empresaLogueada = obtenerEmpresaLogueada(authentication);

            if (!empresaLogueada.getId().equals(empresaId)) {
                redirectAttributes.addFlashAttribute("error",
                        "No tienes permiso para cambiar la contraseña de esta empresa.");
                return "redirect:/empresa/configuracion";
            }

            if (!nuevaPassword.equals(confirmarPassword)) {
                redirectAttributes.addFlashAttribute("error",
                        "La nueva contraseña y la confirmación no coinciden.");
                return "redirect:/empresa/configuracion";
            }

            if (nuevaPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("error",
                        "La nueva contraseña debe tener al menos 8 caracteres.");
                return "redirect:/empresa/configuracion";
            }

            if (nuevaPassword.equals(passwordActual) || nuevaPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                redirectAttributes.addFlashAttribute("error", "Usa una contraseña diferente a la actual y que no sea demasiado larga.");
                return "redirect:/empresa/configuracion";
            }
            boolean exito = empresaService.actualizarPassword(empresaId, passwordActual, nuevaPassword);

            if (exito) {
                redirectAttributes.addFlashAttribute("exito", "¡Contraseña actualizada correctamente!");
            } else {
                redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta.");
            }

            return "redirect:/empresa/configuracion";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado.");
            return "redirect:/empresa/configuracion";
        }
    }

    @PostMapping("/eliminar-cuenta")
    public String eliminarCuenta(
            @RequestParam("id") String id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            Empresa empresaAuth = obtenerEmpresaLogueada(auth);
            if (!empresaAuth.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error",
                        "No tienes permiso para eliminar esta cuenta.");
                return "redirect:/empresa/configuracion";
            }
            empresaService.eliminar(id);
            SecurityContextHolder.clearContext();

            redirectAttributes.addFlashAttribute("exito",
                    "La cuenta de tu empresa ha sido eliminada exitosamente.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al eliminar la cuenta: " + e.getMessage());
            return "redirect:/empresa/configuracion";
        }
    }

}