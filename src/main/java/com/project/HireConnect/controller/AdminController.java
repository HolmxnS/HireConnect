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
        Long totalEmpresas = empresaService.contarEmpresas();
        Long totalPostulantes = postulanteService.contarPostulantes();
        Long totalVacantes = vacanteService.contarVacantes();
        Long postulacionesHoy = postulacionService.contarPostulacionesDeHoy();

        model.addAttribute("activeMenu", "home");
        model.addAttribute("empresas", totalEmpresas);
        model.addAttribute("postulantes", totalPostulantes);
        model.addAttribute("vacantes", totalVacantes);
        model.addAttribute("postulacionesHoy", postulacionesHoy);

        List<RegistroMensualDTO> registrosMes = registroMensualService.registrosDelMes();
        model.addAttribute("registrosMes", registrosMes);
        model.addAttribute("totalRegistrosMes", registrosMes.size());

        Map<String, List<Integer>> registrosPorMes = registroMensualService.registrosPorMes();
        model.addAttribute("registrosPorMes", registrosPorMes);

        List<Notificacion> todas = notificacionService.obtenerTodasOrdenadas();

        model.addAttribute("notificacion", todas);

        Long nuevasNoLeidas = todas.stream().filter(n -> !n.isLeida()).count();
        if (nuevasNoLeidas > 0) {
            model.addAttribute("notificacionesNuevas", nuevasNoLeidas);
        }

        if (auth != null) {
            Admin admin = adminService.buscarPorEmail(auth.getName())
                    .orElse(null);
            model.addAttribute("admin", admin);
        }

        model.addAttribute("activeMenu", "dashboard");
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
    public String revisarEmpresas(Model model, Authentication auth) {

        List<Empresa> listaEmpresas = empresaService.listarTodas();
        model.addAttribute("empresas", listaEmpresas);

        model.addAttribute("totalEmpresas", empresaService.contarEmpresas());
        model.addAttribute("empresasActivas", empresaService.contarEmpresasActivas());
        model.addAttribute("empresasDeshabilitadas", empresaService.contarEmpresasDeshabilitadas());
        model.addAttribute("vacantes", vacanteService.contarVacantes());

        model.addAttribute("activeMenu", "empresas");

        List<Notificacion> todas = notificacionService.obtenerTodasOrdenadas();

        model.addAttribute("notificacion", todas);

        Long nuevasNoLeidas = todas.stream().filter(n -> !n.isLeida()).count();
        if (nuevasNoLeidas > 0) {
            model.addAttribute("notificacionesNuevas", nuevasNoLeidas);
        }

        if (auth != null) {
            Admin admin = adminService.buscarPorEmail(auth.getName())
                    .orElse(null);
            model.addAttribute("admin", admin);
        }

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
    public String aprobarEmpresa(@PathVariable String id) {
        Empresa empresa = empresaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
        if (empresa != null) {
            empresa.setEstado(EmpresaEstado.APROBADA);
            empresaService.guardar(empresa);
        }
        return "redirect:/admin/empresas";
    }

    @GetMapping("/empresas/suspender/{id}")
    public String suspenderEmpresa(@PathVariable String id) {
        Empresa empresa = empresaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
        if (empresa != null) {
            empresa.setEstado(EmpresaEstado.SUSPENDIDA);
            empresaService.guardar(empresa);
        }
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

    @GetMapping("/vacantes/ver/{id}")
    public String verVacante(@PathVariable String id, Model model) {
        Optional<Vacante> vacanteOpt = vacanteService.buscarPorId(id);

        if (vacanteOpt.isEmpty()) {
            return "redirect:/admin/vacantes?error=notfound";
        }

        Vacante vacante = vacanteOpt.get();
        model.addAttribute("vacante", vacante);
        List<Postulacion> postulantes = vacante.getPostulaciones();
        model.addAttribute("postulantes", postulantes != null ? postulantes : new ArrayList<>());

        model.addAttribute("activeMenu", "vacantes");
        return "admin/ver_vacante";
    }

    @GetMapping("/vacantes/cerrar/{id}")
    public String cerrarVacante(@PathVariable String id) {
        Optional<Vacante> vacanteOpt = vacanteService.buscarPorId(id);

        if (vacanteOpt.isPresent()) {
            Vacante vacante = vacanteOpt.get();
            vacante.setEstado(VacanteEstado.CERRADA);
            vacanteService.guardar(vacante);
        }

        return "redirect:/admin/vacantes";
    }

    @GetMapping("/postulantes")
    public String revisarPostulantes(Model model, Authentication auth) {

        List<Postulante> postulantes = postulanteService.listarTodos();
        model.addAttribute("postulantes", postulantes);
        model.addAttribute("activeMenu", "postulantes");

        Map<String, String> estados = new HashMap<>();
        for (Postulante p : postulantes) {
            PostulacionEstado estado = postulacionService.ultimaPostulacion(p)
                    .map(Postulacion::getEstado)
                    .orElse(null);
            String estadoStr = (estado != null) ? estado.getDescripcion() : "Sin Postulaciones";
            estados.put(p.getId(), estadoStr);
        }
        model.addAttribute("estados", estados);

        model.addAttribute("totalPostulantes", postulanteService.contarPostulantes());
        model.addAttribute("postulantesCompletados", postulanteService.contarPostulantesConCV());
        model.addAttribute("postulantesRevision", postulacionService.postulantesEnRevision());
        model.addAttribute("postulantesAceptados", postulacionService.postulantesAceptados());
        model.addAttribute("totalPostulaciones", postulacionService.contarPostulaciones());

        List<Notificacion> todas = notificacionService.obtenerTodasOrdenadas();

        model.addAttribute("notificacion", todas);

        Long nuevasNoLeidas = todas.stream().filter(n -> !n.isLeida()).count();
        if (nuevasNoLeidas > 0) {
            model.addAttribute("notificacionesNuevas", nuevasNoLeidas);
        }

        if (auth != null) {
            Admin admin = adminService.buscarPorEmail(auth.getName())
                    .orElse(null);
            model.addAttribute("admin", admin);
        }

        return "admin/gestion_postulantes";
    }

    @GetMapping("/resumen")
    public String verResumen(Model model) {
        model.addAttribute("activeMenu", "resumen");
        return "admin/resumen_admin";
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
            adminForm.setId(oldAdmin.getId());
            adminForm.setEmail(oldAdmin.getEmail());
            adminForm.setRol(oldAdmin.getRol());
            adminForm.setFoto(oldAdmin.getFoto());
            adminForm.setPassword(oldAdmin.getPassword());
            adminService.guardar(adminForm);

            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
            return "redirect:/admin/editar";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar los cambios: " + e.getMessage());
            return "redirect:/admin/editar";
        }
    }

    @PostMapping("/subirFoto")
    public String subirFotoAdmin(@RequestParam("id") String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            Admin admin = adminService.buscarPorId(id)
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
            String uploadDir = "uploads/admins/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = tipo.equals("image/png") ? ".png" : ".jpg";
            String nombreArchivo = "admin_" + id + "_" + System.currentTimeMillis() + extension;
            Path archivoPath = uploadPath.resolve(nombreArchivo);

            Files.copy(file.getInputStream(), archivoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            admin.setFoto("/" + uploadDir + nombreArchivo);
            adminService.guardar(admin);

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
            RedirectAttributes redirectAttributes) {
        try {
            Admin admin = adminService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

            if (admin.getFoto() != null && !admin.getFoto().isEmpty()) {
                String rutaFoto = admin.getFoto().replaceFirst("/", "");
                Path fotoPath = Paths.get(rutaFoto);

                if (Files.exists(fotoPath)) {
                    Files.delete(fotoPath);
                }

                admin.setFoto(null);
                adminService.guardar(admin);
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

}