package com.project.HireConnect.controller;

import java.io.IOException;

import com.project.HireConnect.dto.Notificacion;
import com.project.HireConnect.dto.RegistroMensualDTO;
import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.*;
import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.NotificacionService;
import com.project.HireConnect.service.PostulacionService;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.RegistroMensualService;
import com.project.HireConnect.service.VacanteService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.HireConnect.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @ModelAttribute
    public void contextoAdmin(Model model, Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) || "true".equals(request.getParameter("fragmento"))) return;
        if (auth != null) model.addAttribute("admin", adminService.buscarPorEmail(auth.getName()).orElse(null));
        List<Notificacion> avisos = notificacionService.obtenerTodasOrdenadas();
        model.addAttribute("notificacion", avisos);
        model.addAttribute("notificacionesNuevas", avisos.stream().filter(n -> !n.isLeida()).count());
    }

    private final VacanteService vacanteService;
    private final EmpresaService empresaService;
    private final PostulanteService postulanteService;
    private final PostulacionService postulacionService;
    private final AdminService adminService;
    private final RegistroMensualService registroMensualService;
    private final NotificacionService notificacionService;

    public AdminController(VacanteService vacanteService,
            EmpresaService empresaService,
            PostulanteService postulanteService,
            PostulacionService postulacionService,
            AdminService adminService,
            RegistroMensualService registroMensualService,
            NotificacionService notificacionService) {
        this.vacanteService = vacanteService;
        this.empresaService = empresaService;
        this.postulanteService = postulanteService;
        this.postulacionService = postulacionService;
        this.adminService = adminService;
        this.registroMensualService = registroMensualService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        model.addAttribute("activeMenu", "home");
        model.addAttribute("empresas", empresaService.contarEmpresas());
        model.addAttribute("postulantes", postulanteService.contarPostulantes());
        List<Vacante> vacantes = vacanteService.listarTodas();
        Map<String, Long> estados = new java.util.LinkedHashMap<>();
        for (VacanteEstado estado : VacanteEstado.values()) {
            estados.put(estado.name(), vacantes.stream().filter(v -> v.getEstado() == estado).count());
        }
        model.addAttribute("estadosVacantes", estados);
        model.addAttribute("vacantesTotal", vacantes.size());
        model.addAttribute("vacantesActivas", estados.get("ACTIVA"));
        model.addAttribute("vacantesPendientes", estados.get("PENDIENTE"));
        model.addAttribute("vacantesSinEstado", vacantes.stream().filter(v -> v.getEstado() == null).count());
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        model.addAttribute("dashboardYear", hoy.getYear());
        model.addAttribute("dashboardMonth", hoy.getMonthValue());
        model.addAttribute("registrosPorMes", registroMensualService.registrosPorMes());

        return "admin/admin_dashboard";
    }

    @PostMapping("/notificaciones/leida/todas")
    @ResponseBody
    public Map<String, Object> marcarTodasComoLeidas() {
        notificacionService.marcarTodasComoLeidas();
        return Map.of("success", true, "unreadCount", 0);
    }

    @PostMapping("/notificaciones/leida/{id}")
    @ResponseBody
    public Map<String, Object> marcarUnaLeida(@PathVariable String id) {
        notificacionService.marcarUnaComoLeida(id);
        int unreadCount = notificacionService.contarNuevas();
        return Map.of("success", true, "unreadCount", unreadCount);
    }

    @PostMapping("/notificaciones/eliminar/{id}")
    @ResponseBody
    public Map<String, Object> eliminarUna(@PathVariable String id) {
        notificacionService.eliminarUna(id);
        int unreadCount = notificacionService.contarNuevas();
        return Map.of("success", true, "unreadCount", unreadCount);
    }

    @PostMapping("/notificaciones/eliminar/todas")
    @ResponseBody
    public Map<String, Object> eliminarTodas() {
        notificacionService.eliminarTodas();
        return Map.of("success", true, "unreadCount", 0);
    }

    @GetMapping("/empresas")
    public String revisarEmpresas(Model model, Authentication auth,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "most_recent") String orden, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "false") boolean fragmento) {
        q = q.trim();
        if (q.length() > 120) q = q.substring(0, 120);
        estado = List.of("APROBADA", "SUSPENDIDA").contains(estado) ? estado : "";
        orden = "oldest".equals(orden) ? "oldest" : "most_recent";
        var resultado = empresaService.paginaAdmin(q, estado, orden, pagina);
        model.addAttribute("empresas", resultado.getContent());
        model.addAttribute("paginaEmpresas", resultado);
        model.addAttribute("q", q);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("ordenFiltro", orden);
        if (fragmento) return "admin/gestion_empresas :: resultados";

        model.addAttribute("totalEmpresas", empresaService.contarEmpresas());
        model.addAttribute("empresasActivas", empresaService.contarEmpresasActivas());
        model.addAttribute("empresasDeshabilitadas", empresaService.contarEmpresasDeshabilitadas());
        model.addAttribute("vacantes", vacanteService.contarVacantes());

        model.addAttribute("activeMenu", "empresas");

        return "admin/gestion_empresas";
    }

    

    @GetMapping("/empresas/ver/{id}")
    public String verEmpresa(@PathVariable String id, Model model) {

        Empresa empresa = empresaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));

        List<Vacante> vacantes = vacanteService.buscarPorEmpresa(empresa);

        model.addAttribute("empresa", empresa);
        model.addAttribute("vacantes", vacantes);
        model.addAttribute("activeMenu", "empresas");

        return "admin/ver_empresa";
    }

    @GetMapping("/empresas/eliminar/{id}")
    public String eliminarEmpresa(@PathVariable String id) {
        empresaService.eliminar(id);
        return "redirect:/admin/empresas";
    }

    @GetMapping("/empresas/aprobar/{id}")
    public String aprobarEmpresa(@PathVariable String id, RedirectAttributes redirect) {
        Empresa empresa = empresaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
        if (empresa != null) {
            empresa.setEstado(EmpresaEstado.APROBADA);
            empresaService.guardar(empresa);
        }
        redirect.addFlashAttribute("success", "Empresa aprobada.");
        return "redirect:/admin/empresas";
    }

    @GetMapping("/empresas/suspender/{id}")
    public String suspenderEmpresa(@PathVariable String id, RedirectAttributes redirect) {
        Empresa empresa = empresaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
        if (empresa != null) {
            empresa.setEstado(EmpresaEstado.SUSPENDIDA);
            empresaService.guardar(empresa);
        }
        redirect.addFlashAttribute("success", "Empresa suspendida.");
        return "redirect:/admin/empresas";
    }

    @GetMapping("/empresas/deshabilitar/{id}")
    public String deshabilitarEmpresa(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = empresaService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
            empresaService.eliminar(empresa.getId());
            redirectAttributes.addFlashAttribute("success", "Empresa eliminada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la empresa: " + e.getMessage());
        }
        return "redirect:/admin/empresas";
    }

    @GetMapping("/vacantes")
    public String listarVacantesAdmin(Model model,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "most_recent") String orden, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "false") boolean fragmento) {
        q = q.trim(); if (q.length() > 120) q = q.substring(0, 120);
        estado = List.of("ACTIVA", "PENDIENTE", "CERRADA").contains(estado) ? estado : "";
        orden = "oldest".equals(orden) ? "oldest" : "most_recent";
        var resultado = vacanteService.paginaAdmin(q, estado, orden, pagina);
        model.addAttribute("vacantes", resultado.getContent());
        model.addAttribute("paginaVacantes", resultado);
        model.addAttribute("q", q); model.addAttribute("estadoFiltro", estado); model.addAttribute("ordenFiltro", orden);
        if (fragmento) return "admin/gestion_vacantes :: resultados";
        model.addAttribute("activeMenu", "vacantes");
        return "admin/gestion_vacantes";
    }

    @GetMapping("/vacantes/ver/{id}")
    public String verVacante(@PathVariable String id, Model model) {
        Optional<Vacante> vacanteOpt = vacanteService.buscarPorId(id);

        if (vacanteOpt.isEmpty()) {
            return "redirect:/admin/empresas?error=notfound";
        }

        Vacante vacante = vacanteOpt.get();
        model.addAttribute("vacante", vacante);
        List<Postulacion> postulantes = postulacionService.buscarPorVacante(vacante).stream()
                .filter(p -> p != null && p.getPostulante() != null).toList();
        model.addAttribute("postulantes", postulantes != null ? postulantes : new ArrayList<>());

        model.addAttribute("activeMenu", "vacantes");
        return "admin/ver_vacante";
    }

    @GetMapping("/vacantes/cerrar/{id}")
    public String cerrarVacante(@PathVariable String id, RedirectAttributes redirect) {
        Optional<Vacante> vacanteOpt = vacanteService.buscarPorId(id);

        if (vacanteOpt.isPresent()) {
            Vacante vacante = vacanteOpt.get();
            vacante.setEstado(VacanteEstado.CERRADA);
            vacanteService.guardar(vacante);
        }

        redirect.addFlashAttribute("success", "Vacante cerrada.");
        return "redirect:/admin/vacantes/ver/" + id;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.HireConnect.service.AdminPostulantesService adminPostulantesService;

    @GetMapping("/postulantes")
    public String revisarPostulantes(Model model, Authentication auth,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String estado,
            @RequestParam(defaultValue = "all") String cv, @RequestParam(defaultValue = "most_recent") String orden,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "false") boolean fragmento) {

        q = q.trim(); if (q.length() > 120) q = q.substring(0, 120);
        estado = List.of("Pendiente", "En revision", "Entrevista pendiente", "Aceptada", "Rechazada", "Sin Postulaciones").contains(estado) ? estado : "all";
        cv = List.of("complete", "incomplete").contains(cv) ? cv : "all";
        orden = "oldest".equals(orden) ? "oldest" : "most_recent";
        var resultado = adminPostulantesService.pagina(q, estado, cv, orden, pagina);
        model.addAttribute("postulantes", resultado.pagina().getContent());
        model.addAttribute("paginaPostulantes", resultado.pagina());
        model.addAttribute("estados", resultado.estados());
        model.addAttribute("q", q); model.addAttribute("estadoFiltro", estado);
        model.addAttribute("cvFiltro", cv); model.addAttribute("ordenFiltro", orden);
        if (fragmento) return "admin/gestion_postulantes :: resultados";
        model.addAttribute("activeMenu", "postulantes");
        java.time.LocalDate fechaPanel = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        model.addAttribute("postulantesYear", fechaPanel.getYear());
        model.addAttribute("postulantesMonth", fechaPanel.getMonthValue());
        model.addAttribute("resumenPostulantes", adminPostulantesService.resumen(fechaPanel));
        model.addAttribute("postulantesRevision", postulacionService.postulantesEnRevision());
        model.addAttribute("postulantesAceptados", postulacionService.postulantesAceptados());
        model.addAttribute("totalPostulaciones", postulacionService.contarPostulaciones());

        return "admin/gestion_postulantes";
    }

    @GetMapping("/resumen")
    public String verResumen() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/editar")
    public String mostrarFormularioEdicionAdmin(Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            String email = auth.getName();
            Admin admin = adminService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

            model.addAttribute("activeMenu", "perfil");
            model.addAttribute("admin", admin);
            return "admin/mi_perfil";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el perfil: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/guardar-perfil")
    public String guardarPerfilAdmin(@ModelAttribute("admin") Admin adminForm,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            String email = auth.getName();
            Admin oldAdmin = adminService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin no encontrado"));
            String nombre = adminForm.getNombre() == null ? "" : adminForm.getNombre().trim();
            String apellido = adminForm.getApellido() == null ? "" : adminForm.getApellido().trim();
            String about = adminForm.getAbout() == null ? "" : adminForm.getAbout().trim();
            if (nombre.isEmpty() || apellido.isEmpty() || nombre.length() > 80 || apellido.length() > 80 || about.length() > 500) {
                redirectAttributes.addFlashAttribute("error", "Completa nombre y apellido (máximo 80 caracteres) y una descripción de hasta 500 caracteres.");
                return "redirect:/admin/editar";
            }
            oldAdmin.setNombre(nombre);
            oldAdmin.setApellido(apellido);
            oldAdmin.setAbout(about);
            adminService.guardar(oldAdmin);

            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
            return "redirect:/admin/editar";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar los cambios: " + e.getMessage());
            return "redirect:/admin/editar";
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.HireConnect.service.ImagenesService imagenesService;

    @PostMapping("/subirFoto")
    public String subirFotoAdmin(@RequestParam("id") String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes, Authentication auth) {
        try {
            Admin admin = adminService.buscarPorEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Por favor, selecciona un archivo.");
                return "redirect:/admin/editar";
            }

            String tipo = file.getContentType();
            if (tipo == null || !(tipo.equals("image/jpeg") || tipo.equals("image/png"))) {
                redirectAttributes.addFlashAttribute("error", "Solo se permiten imágenes JPG o PNG.");
                return "redirect:/admin/editar";
            }

            long maxSizeBytes = 2 * 1024 * 1024; // 2MB
            if (file.getSize() > maxSizeBytes) {
                redirectAttributes.addFlashAttribute("error", "El archivo excede el tamaño máximo permitido (2MB).");
                return "redirect:/admin/editar";
            }
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(file.getInputStream());
            if (original == null || (long) original.getWidth() * original.getHeight() > 40_000_000) {
                redirectAttributes.addFlashAttribute("error", "La imagen no es válida o tiene demasiada resolución.");
                return "redirect:/admin/editar";
            }
            int lado = Math.min(original.getWidth(), original.getHeight());
            java.awt.image.BufferedImage normalizada = new java.awt.image.BufferedImage(512, 512, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = normalizada.createGraphics();
            graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, 512, 512);
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(original, 0, 0, 512, 512, (original.getWidth() - lado) / 2, (original.getHeight() - lado) / 2,
                    (original.getWidth() + lado) / 2, (original.getHeight() + lado) / 2, null);
            graphics.dispose();
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(normalizada, "jpg", bytes);
            String url = "/uploads/admins/admin_" + java.util.UUID.randomUUID() + ".jpg";
            imagenesService.guardar(url, bytes.toByteArray(), "image/jpeg");
            String anterior = admin.getFoto();
            admin.setFoto(url);
            try { adminService.guardar(admin); }
            catch (RuntimeException error) { admin.setFoto(anterior); imagenesService.eliminar(url); throw error; }
            if (anterior != null && anterior.startsWith("/uploads/admins/")) {
                try { imagenesService.eliminar(anterior); }
                catch (IOException | RuntimeException error) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo retirar la imagen anterior del administrador."); }
            }

            redirectAttributes.addFlashAttribute("success", "Foto subida correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir la foto: " + e.getMessage());
        }
        return "redirect:/admin/editar";
    }

    @GetMapping("/eliminarFoto")
    public String eliminarFotoAdmin(@RequestParam("id") String id,
            RedirectAttributes redirectAttributes, Authentication auth) {
        try {
            Admin admin = adminService.buscarPorEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

            if (admin.getFoto() != null && !admin.getFoto().isEmpty()) {
                String anterior = admin.getFoto();
                admin.setFoto(null);
                try { adminService.guardar(admin); }
                catch (RuntimeException error) { admin.setFoto(anterior); throw error; }
                imagenesService.eliminar(anterior);
                redirectAttributes.addFlashAttribute("success", "Foto eliminada correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("info", "No tienes una foto guardada.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la foto: " + e.getMessage());
        }
        return "redirect:/admin/editar";
    }

    @GetMapping("/configuracion")
    public String configuracion(Model model) {
        model.addAttribute("activeMenu", "configuracion");
        return "admin/configuracion";
    }

    @PostMapping("/configuracion/password")
    public String cambiarPassword(Authentication auth, @RequestParam String passwordActual,
            @RequestParam String nuevaPassword, @RequestParam String confirmarPassword, RedirectAttributes redirect) {
        Admin admin = adminService.buscarPorEmail(auth.getName()).orElseThrow();
        if (!passwordEncoder.matches(passwordActual, admin.getPassword())) {
            redirect.addFlashAttribute("error", "La contraseña actual no es correcta.");
        } else if (nuevaPassword.length() < 8 || nuevaPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !nuevaPassword.equals(confirmarPassword) || passwordEncoder.matches(nuevaPassword, admin.getPassword())) {
            redirect.addFlashAttribute("error", "Usa una contraseña diferente de al menos 8 caracteres, máximo 72 bytes, y confirma que coincidan.");
        } else {
            admin.setPassword(passwordEncoder.encode(nuevaPassword));
            adminService.guardar(admin);
            redirect.addFlashAttribute("success", "Contraseña actualizada.");
        }
        return "redirect:/admin/configuracion#seguridad";
    }

    @GetMapping("/configuracion/datos")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, String>> datosAdmin(Authentication auth) {
        Admin admin = adminService.buscarPorEmail(auth.getName()).orElseThrow();
        Map<String, String> datos = new java.util.LinkedHashMap<>();
        datos.put("nombre", java.util.Objects.toString(admin.getNombre(), ""));
        datos.put("apellido", java.util.Objects.toString(admin.getApellido(), ""));
        datos.put("email", java.util.Objects.toString(admin.getEmail(), ""));
        datos.put("about", java.util.Objects.toString(admin.getAbout(), ""));
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=hireconnect-admin.json")
                .header("Cache-Control", "no-store").body(datos);
    }

}
