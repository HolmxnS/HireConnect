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

    // getEmpresa
    private Empresa obtenerEmpresaLogueada(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        String email = authentication.getName();

        return empresaService.buscarPorEmailEmpresa(email)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada para el email: " + email));
    }

    // inicio empresa
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

    // gestion de vacantes
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

    // Mostrar vacantes
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

    // Mostrar postulaciones de vacante seleccionada
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

    // Mostrar detalles de postulacion
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

    // Perfil
    @GetMapping("/editar")
    public String mostrarFormularioEdicionEmpresa(
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = obtenerEmpresaLogueada(auth);

            model.addAttribute("activeMenu", "perfil");
            model.addAttribute("empresa", empresa);
            return "empresa/editar_perfil";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el perfil.");
            return "redirect:/empresa/home";
        }
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

            if (empresaForm.getPassword() == null || empresaForm.getPassword().isEmpty()) {
                empresaForm.setPassword(oldEmpresa.getPassword());
            } else {

                empresaForm.setPassword(oldEmpresa.getPassword());
            }

            empresaService.guardar(empresaForm);

            redirectAttributes.addFlashAttribute("success", "Perfil de la empresa actualizado exitosamente.");
            return "redirect:/empresa/editar";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar los cambios: " + e.getMessage());
            return "redirect:/empresa/editar";
        }
    }

    @PostMapping("/subirFoto")
    public String subirFotoEmpresa(@RequestParam("id") String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = empresaService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Por favor, selecciona un archivo.");
                return "redirect:/empresa/editar";
            }
            // Validar tipo de archivo
            String tipo = file.getContentType();
            if (tipo == null || !(tipo.equals("image/jpeg") || tipo.equals("image/png"))) {
                redirectAttributes.addFlashAttribute("error", "Solo se permiten imágenes JPG o PNG.");
                return "redirect:/empresa/editar";
            }
            // Validar tamaño de archivo
            long maxSizeBytes = 2 * 1024 * 1024; // 2MB
            if (file.getSize() > maxSizeBytes) {
                redirectAttributes.addFlashAttribute("error", "El archivo excede el tamaño máximo permitido (2MB).");
                return "redirect:/empresa/editar";
            }
            // Crear carpeta si no existe
            String uploadDir = "uploads/empresas/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            // Guardar archivo
            String extension = tipo.equals("image/png") ? ".png" : ".jpg";
            String nombreArchivo = "empresa_" + id + "_" + System.currentTimeMillis() + extension;
            Path archivoPath = uploadPath.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), archivoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // Guardar ruta en la empresa
            empresa.setFoto("/" + uploadDir + nombreArchivo);
            empresaService.guardar(empresa);
            redirectAttributes.addFlashAttribute("success", "Foto subida correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al subir la foto: " + e.getMessage());
        }
        return "redirect:/empresa/editar";
    }

    @GetMapping("/eliminarFoto")
    public String eliminarFotoEmpresa(@RequestParam("id") String id,
            RedirectAttributes redirectAttributes) {
        try {
            Empresa empresa = empresaService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
            if (empresa.getFoto() != null) {
                String rutaFoto = empresa.getFoto().replaceFirst("/", "");
                Path fotoPath = Paths.get(rutaFoto);
                if (Files.exists(fotoPath)) {
                    Files.delete(fotoPath);
                }
                empresa.setFoto(null);
                empresaService.guardar(empresa);
                redirectAttributes.addFlashAttribute("success", "Foto eliminada correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("info", "No tienes una foto guardada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la foto: " + e.getMessage());
        }
        return "redirect:/empresa/editar";
    }

    // Configuracion
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

            // Validar contraseña
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