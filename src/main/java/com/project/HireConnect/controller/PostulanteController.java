package com.project.HireConnect.controller;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.Educacion_postulante;
import com.project.HireConnect.model.Experiencia_postulante;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.HistorialService;
import com.project.HireConnect.service.PostulacionService;
import com.project.HireConnect.service.VacanteService;

import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/postulante")
public class PostulanteController {

    private final PostulanteService postulanteService;
    private final PostulacionService postulacionService;
    private final VacanteService vacanteService;
    private final HistorialService historialService;
    private final String UPLOAD_DIR = "uploads/cv/";

    public PostulanteController(PostulanteService postulanteService,
            PostulacionService postulacionService,
            VacanteService vacanteService,
            HistorialService historialService) {
        this.postulanteService = postulanteService;
        this.postulacionService = postulacionService;
        this.vacanteService = vacanteService;
        this.historialService = historialService;
    }

    private Optional<Postulante> getAuthenticatedPostulante(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return postulanteService.buscarPorEmail(authentication.getName());
    }

    // Compatibilidad con marcadores y enlaces antiguos.
    @GetMapping("/home")
    public String redirigirInicio() {
        return "redirect:/postulante/vacantes";
    }

    @GetMapping("/vacantes/sugerencias")
    @ResponseBody
    public List<String> obtenerSugerencias(@RequestParam String texto) {
        return vacanteService.sugerenciasTitulo(texto);
    }

    @PostMapping("/historial/guardar")
    @ResponseBody
    public void guardarHistorial(@RequestParam String texto, Authentication auth) {
        historialService.guardar(auth.getName(), texto);
    }

    @GetMapping("/historial")
    @ResponseBody
    public List<String> obtenerHistorial(Authentication auth) {
        return historialService.obtenerHistorial(auth.getName());
    }

    @PostMapping("/historial/limpiar")
    @ResponseBody
    public void limpiarHistorial(Authentication auth) {
        historialService.limpiar(auth.getName());
    }

    @GetMapping("/vacantes")
    public String vacantesPostulantes(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "modalidad", required = false) String modalidad,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
            return "redirect:/login";
        }
        Postulante postulante = optPostulante.get();

        List<Vacante> vacantes;
        if ((query == null || query.isBlank()) && (modalidad == null || modalidad.isBlank())) {
            vacantes = vacanteService.listarTodas();
        } else {
            vacantes = vacanteService.buscarPorTituloYModalidad(query, modalidad);
        }

        model.addAttribute("postulante", postulante);
        model.addAttribute("vacantes", vacantes.stream()
                .filter(v -> v.getEstado() == VacanteEstado.ACTIVA).toList());
        model.addAttribute("activeMenu", "vacantes");
        return "postulante/vacantePostulante";
    }

    @GetMapping("/vacantes/listar")
    public String listarVacantesPublicas(
            @RequestParam(value = "filtro", required = false) String filtro,
            @RequestParam(value = "modalidad", required = false) String modalidad,
            @RequestParam(value = "salarioMin", required = false) Double salarioMin,
            @RequestParam(value = "fechaFiltro", required = false) String fechaFiltro,
            @RequestParam(value = "tipoContrato", required = false) String tipoContrato,
            @RequestParam(value = "salarioMax", required = false) Double salarioMax,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        Optional<Postulante> postulanteOpt = postulanteService.buscarPorEmail(email);

        if (postulanteOpt.isEmpty()) {
            return "redirect:/logout";
        }

        model.addAttribute("postulante", postulanteOpt.get());

        model.addAttribute("vacantes",
                vacanteService.buscarPorFiltros(
                        filtro,
                        modalidad,
                        salarioMin,
                        salarioMax).stream()
                        .filter(v -> v.getEstado() == VacanteEstado.ACTIVA)
                        .toList());

        model.addAttribute("filtro", filtro);
        model.addAttribute("modalidad", modalidad);
        model.addAttribute("tipoContrato", tipoContrato);
        model.addAttribute("fechaFiltro", fechaFiltro);
        model.addAttribute("salarioMin", salarioMin);
        model.addAttribute("salarioMax", salarioMax);

        return "postulante/vacantePostulante";
    }

    @GetMapping("/vacante/{id}")
    public String verDetalleVacante(@PathVariable("id") String idVacante,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);

        if (optPostulante.isEmpty()) {
            return "redirect:/login";
        }

        Optional<Vacante> optVacante = vacanteService.buscarPorId(idVacante);

        if (optVacante.isEmpty()) {
            model.addAttribute("error", "Vacante no encontrada con ID: " + idVacante);
            return "error";
        }

        Vacante vacante = optVacante.get();

        String primeraPalabra = vacante.getTitulo().split(" ")[0];

        List<Vacante> vacantesSimilares = vacanteService.buscarPorTituloYModalidad(
                primeraPalabra,
                null).stream()
                .filter(v -> !v.getId().equals(vacante.getId()))
                .limit(10)
                .toList();

        model.addAttribute("vacantesSimilares", vacantesSimilares);
        model.addAttribute("vacante", vacante);
        model.addAttribute("postulante", optPostulante.get());
        return "postulante/detalle_vacante";
    }

    @GetMapping("/postular")
    public String postular(@RequestParam String vacanteId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        Optional<Vacante> optVacante = vacanteService.buscarPorId(vacanteId);

        if (optPostulante.isEmpty()) {
            return "redirect:/login";
        }

        if (optVacante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Error: Vacante no encontrada.");
            return "redirect:/postulante/vacantes";
        }

        Postulante postulante = optPostulante.get();
        Vacante vacante = optVacante.get();

        if (postulacionService.existePostulacion(postulante, vacante)) {
            redirectAttributes.addFlashAttribute("error", "Ya te has postulado a esta vacante.");
            return "redirect:/postulante/vacantes";
        }

        Postulacion nuevaPostulacion = new Postulacion();
        nuevaPostulacion.setPostulante(postulante);
        nuevaPostulacion.setVacante(vacante);
        nuevaPostulacion.setFechaPostulacion(LocalDate.now());
        nuevaPostulacion.setEstado(PostulacionEstado.EN_REVISION);

        postulacionService.guardar(nuevaPostulacion);

        redirectAttributes.addFlashAttribute("exito",
                "¡Postulación exitosa! Ahora puedes ver el estado en 'Mis Postulaciones'.");
        return "redirect:/postulante/vacantes";
    }

    @GetMapping("/postulacion/detalle/{id}")
    public String verDetallePostulacion(@PathVariable("id") String idPostulacion,
            @RequestParam(name = "panel", defaultValue = "false") boolean panel,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Postulacion> optPostulacion = postulacionService.buscarPorId(idPostulacion);
        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);

        if (optPostulacion.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Postulación no encontrada.");
            return "redirect:/postulante/postulaciones";
        }

        if (optPostulante.isEmpty()) {
            return "redirect:/login";
        }

        Postulacion postulacion = optPostulacion.get();
        Postulante postulante = optPostulante.get();

        if (!postulacion.getPostulante().getId().equals(postulante.getId())) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado a esta postulación.");
            return "redirect:/postulante/postulaciones";
        }

        model.addAttribute("postulacion", postulacion);
        model.addAttribute("vacante", postulacion.getVacante());
        model.addAttribute("postulante", postulante);

        return panel ? "postulante/fragments/detalle_postulacion_panel :: contenido"
                : "postulante/detalle_postulacion";
    }

    @GetMapping("/postulaciones")
    public String verPostulaciones(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada.");
            return "redirect:/login";
        }

        Postulante postulante = optPostulante.get();
        List<Postulacion> postulaciones = postulacionService.buscarPorPostulante(postulante).stream()
                .sorted(java.util.Comparator.comparing(Postulacion::getFechaPostulacion,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Postulacion::getId,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();

        model.addAttribute("postulante", postulante);
        model.addAttribute("postulaciones", postulaciones);
        model.addAttribute("postulacionesEnProceso", postulaciones.stream()
                .filter(p -> p.getEstado() == PostulacionEstado.PENDIENTE || p.getEstado() == PostulacionEstado.EN_REVISION).count());
        model.addAttribute("postulacionesEntrevista", postulaciones.stream()
                .filter(p -> p.getEstado() == PostulacionEstado.ENTREVISTA_PENDIENTE).count());
        model.addAttribute("postulacionesAceptadas", postulaciones.stream()
                .filter(p -> p.getEstado() == PostulacionEstado.ACEPTADA).count());
        model.addAttribute("activeMenu", "mis_postulaciones");

        return "postulante/mis_postulaciones";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarPostulacion(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Optional<Postulacion> opt = postulacionService.buscarPorId(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La postulación no existe o ya fue eliminada.");
                return "redirect:/postulante/postulaciones";
            }
            Optional<Postulante> actual = getAuthenticatedPostulante(authentication);
            if (actual.isEmpty()) return "redirect:/login";
            if (opt.get().getPostulante() == null || !actual.get().getId().equals(opt.get().getPostulante().getId())) {
                redirectAttributes.addFlashAttribute("error", "No puedes retirar una postulación de otra cuenta.");
                return "redirect:/postulante/postulaciones";
            }
            postulacionService.eliminar(id);
            redirectAttributes.addFlashAttribute("exito", "Has retirado tu postulación correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la postulación: " + e.getMessage());
        }
        return "redirect:/postulante/postulaciones";
    }

    @GetMapping("/perfil")
    public String verPerfil(Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);

        if (optPostulante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para acceder a este perfil.");
            return "redirect:/login";
        }

        Optional<Postulante> optPerfilCompleto = postulanteService.buscarPorId(optPostulante.get().getId());

        if (optPerfilCompleto.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Perfil no encontrado o incompleto.");
            return "redirect:/postulante/vacantes";
        }

        model.addAttribute("postulante", optPerfilCompleto.get());
        model.addAttribute("activeMenu", "perfil");

        return "postulante/perfil_postulante";
    }

    @PostMapping("/actualizar")
    public String actualizarDatos(
            @ModelAttribute Postulante postulanteForm,
            Authentication authentication,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulanteExistente = getAuthenticatedPostulante(authentication);

        if (optPostulanteExistente.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada o perfil no encontrado.");
            return "redirect:/login";
        }

        Postulante postulanteExistente = optPostulanteExistente.get();

        try {

            if (result.hasErrors())
                return "redirect:/postulante/perfil";

            BeanUtils.copyProperties(postulanteForm, postulanteExistente,
                    "id", "email", "password", "cv", "fechaRegistro",
                    "habilidades", "educacion", "experiencia", "postulaciones",
                    "tipoDocumento", "identificacion", "fechaNacimiento", "genero");

            postulanteService.guardar(postulanteExistente);

            redirectAttributes.addFlashAttribute("exito", "Perfil actualizado correctamente");
            return "redirect:/postulante/perfil";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/postulante/perfil";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al guardar el perfil.");
            return "redirect:/postulante/perfil";
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.HireConnect.service.FotoPostulanteService fotoPostulanteService;

    @PostMapping("/subirFoto")
    public String subirFoto(@RequestParam("id") String id,
            @RequestParam("file") MultipartFile file, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";
        try {
            fotoPostulanteService.guardar(authentication.getName(), file);
            redirectAttributes.addFlashAttribute("exito", "Foto actualizada.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos guardar la foto. Inténtalo de nuevo.");
        }
        return "redirect:/postulante/perfil";
    }

    @GetMapping("/eliminarFoto")
    public String eliminarFoto(@RequestParam("id") String id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";
        try {
            fotoPostulanteService.eliminar(authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Foto eliminada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos quitar la foto. Inténtalo de nuevo.");
        }
        return "redirect:/postulante/perfil";
    }

    @PostMapping("/agregarHabilidad")
    public String agregarHabilidad(@RequestParam String id,
            @RequestParam String habilidad,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        Postulante postulante = postulanteService.buscarPorId(id).get();
        postulante.getHabilidades().add(habilidad.trim());
        postulanteService.guardar(postulante);
        redirectAttributes.addFlashAttribute("exito", "Habilidad agregada correctamente");

        return "redirect:/postulante/perfil";
    }

    @PostMapping("/agregarEstudio")
    public String agregarEstudio(@RequestParam String id,
            @RequestParam String institucion,
            @RequestParam String titulo,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        Postulante postulante = postulanteService.buscarPorId(id).get();

        Educacion_postulante edu = new Educacion_postulante();
        edu.setInstitucion(institucion);
        edu.setTitulo(titulo);
        edu.setFechaInicio(fechaInicio);
        edu.setFechaFin(fechaFin);

        postulante.getEducacion().add(edu);
        postulanteService.guardar(postulante);

        redirectAttributes.addFlashAttribute("exito", "Estudio agregado correctamente");
        return "redirect:/postulante/perfil";
    }

    @PostMapping("/agregarExperiencia")
    public String agregarExperiencia(
            @RequestParam String id,
            @RequestParam String empresa,
            @RequestParam String cargo,
            @RequestParam String descripcion,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulanteAuth = getAuthenticatedPostulante(authentication);
        if (optPostulanteAuth.isEmpty() || !optPostulanteAuth.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        Optional<Postulante> opt = postulanteService.buscarPorId(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró el postulante.");
            return "redirect:/postulante/perfil";
        }

        Postulante postulante = opt.get();

        if (empresa.isBlank() || cargo.isBlank() || descripcion.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios.");
            return "redirect:/postulante/perfil";
        }

        Experiencia_postulante exp = new Experiencia_postulante();
        exp.setEmpresa(empresa);
        exp.setCargo(cargo);
        exp.setDescripcion(descripcion);
        exp.setFechaInicio(fechaInicio);
        exp.setFechaFin(fechaFin);

        postulante.getExperiencia().add(exp);
        postulanteService.guardar(postulante);

        redirectAttributes.addFlashAttribute("exito", "Experiencia agregada correctamente.");
        return "redirect:/postulante/perfil";
    }

    @GetMapping("/eliminarHabilidad")
    public String eliminarHabilidad(@RequestParam String id, @RequestParam int index,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        postulanteService.buscarPorId(id).ifPresent(postulante -> {
            List<String> lista = new ArrayList<>(postulante.getHabilidades());
            if (index >= 0 && index < lista.size()) {
                lista.remove(index);
                postulante.setHabilidades(new HashSet<>(lista));
                postulanteService.guardar(postulante);
                redirectAttributes.addFlashAttribute("exito", "Habilidad eliminada correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Índice de habilidad inválido.");
            }
        });

        return "redirect:/postulante/perfil";
    }

    @GetMapping("/eliminarEstudio")
    public String eliminarEstudio(@RequestParam String id,
            @RequestParam int index,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        Postulante postulante = postulanteService.buscarPorId(id).orElseThrow();
        if (index >= 0 && index < postulante.getEducacion().size()) {
            postulante.getEducacion().remove(index);
            postulanteService.guardar(postulante);
            redirectAttributes.addFlashAttribute("exito", "Estudio eliminado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Índice de estudio inválido.");
        }

        return "redirect:/postulante/perfil";
    }

    @GetMapping("/eliminarExperiencia")
    public String eliminarExperiencia(@RequestParam String id,
            @RequestParam int index,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);
        if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado.");
            return "redirect:/postulante/vacantes";
        }

        Postulante postulante = postulanteService.buscarPorId(id).orElseThrow();
        if (index >= 0 && index < postulante.getExperiencia().size()) {
            postulante.getExperiencia().remove(index);
            postulanteService.guardar(postulante);
            redirectAttributes.addFlashAttribute("exito", "Experiencia eliminada correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Índice de experiencia inválido.");
        }

        return "redirect:/postulante/perfil";
    }

    @GetMapping("/descargarCV/{id}")
    public ResponseEntity<Resource> descargarCV(@PathVariable String id, Authentication authentication) {
        Optional<Postulante> optPostulanteAuth = getAuthenticatedPostulante(authentication);
        if (optPostulanteAuth.isEmpty() || !optPostulanteAuth.get().getId().equals(id)) {
            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_PLATAFORMA"));
            boolean esEmpresa = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));
            if (!esAdmin && !esEmpresa) {
                return ResponseEntity.status(403).build();
            }
        }

        Optional<Postulante> optPostulante = postulanteService.buscarPorId(id);

        if (optPostulante.isEmpty() || optPostulante.get().getCv() == null) {
            return ResponseEntity.notFound().build();
        }

        Postulante postulante = optPostulante.get();
        String filenameInDB = Paths.get(postulante.getCv()).getFileName().toString();
        Path path = Paths.get(UPLOAD_DIR).resolve(filenameInDB);

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(path.toUri());

            String contentDisposition = "attachment; filename=\"" + path.getFileName().toString() + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/subirCV")
    public String subirCV(@RequestParam("file") MultipartFile file,
            @RequestParam("id") String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Optional<Postulante> optPostulanteAuth = getAuthenticatedPostulante(authentication);
        if (optPostulanteAuth.isEmpty() || !optPostulanteAuth.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado para subir CV.");
            return "redirect:/postulante/vacantes";
        }

        try {
            if (file == null || file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se ha seleccionado ningún archivo.");
                return "redirect:/postulante/perfil";
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El archivo no tiene nombre válido.");
                return "redirect:/postulante/perfil";
            }

            originalFilename = Paths.get(originalFilename).getFileName().toString();

            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String fileName = "cv_" + id + "_" + originalFilename;
            Path filePath = uploadDir.resolve(fileName);

            String cvActual = optPostulanteAuth.get().getCv();
            if (cvActual != null) {
                Path viejo = Paths.get(cvActual);
                if (Files.exists(viejo)) {
                    Files.delete(viejo);
                }
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String dbPath = UPLOAD_DIR + fileName;
            postulanteService.actualizarCv(id, dbPath);

            redirectAttributes.addFlashAttribute("exito", "CV subido y actualizado correctamente.");

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al subir el CV: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al subir el CV o actualizar la base de datos: " + e.getMessage());
        }
        return "redirect:/postulante/perfil";
    }

    @GetMapping("/configuracion")
    public String mostrarConfiguracion(Authentication authentication, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Postulante> optPostulante = getAuthenticatedPostulante(authentication);

        if (optPostulante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada o perfil no encontrado.");
            return "redirect:/login";
        }

        Postulante postulante = optPostulante.get();

        model.addAttribute("postulante", postulante);
        model.addAttribute("activeMenu", "configuracion");
        return "postulante/configuracion_postulante";
    }

    @GetMapping("/configuracion/datos")
    @ResponseBody
    public ResponseEntity<?> descargarDatos(Authentication authentication) {
        Optional<Postulante> actual = getAuthenticatedPostulante(authentication);
        if (actual.isEmpty()) return ResponseEntity.status(401).build();
        Postulante p = actual.get();
        // Lista explícita para excluir credenciales y referencias internas de MongoDB.
        java.util.Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("nombre", p.getNombre());
        datos.put("apellido", p.getApellido());
        datos.put("email", p.getEmail());
        datos.put("telefono", p.getTelefono());
        datos.put("tipoDocumento", p.getTipoDocumento());
        datos.put("identificacion", p.getIdentificacion());
        datos.put("fechaNacimiento", p.getFechaNacimiento());
        datos.put("genero", p.getGenero());
        datos.put("nacionalidad", p.getNacionalidad());
        datos.put("titulacion", p.getTitulacion());
        datos.put("fechaRegistro", p.getFechaRegistro());
        datos.put("habilidades", p.getHabilidades());
        datos.put("educacion", p.getEducacion());
        datos.put("experiencia", p.getExperiencia());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hireconnect-perfil.json")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(datos);
    }

    @PostMapping("/cambiarPassword")
    public String cambiarPassword(
            @RequestParam("id") String postulanteId,
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevaPassword") String nuevaPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Optional<Postulante> optPostulanteAuth = getAuthenticatedPostulante(authentication);
        if (optPostulanteAuth.isEmpty() || !optPostulanteAuth.get().getId().equals(postulanteId)) {
            redirectAttributes.addFlashAttribute("error", "Error de seguridad: Acceso no autorizado para esta acción.");
            return "redirect:/postulante/configuracion";
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            redirectAttributes.addFlashAttribute("error", "La nueva contraseña y la confirmación no coinciden.");
            return "redirect:/postulante/configuracion";
        }
        if (nuevaPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "La nueva contraseña debe tener al menos 8 caracteres.");
            return "redirect:/postulante/configuracion";
        }
        if (nuevaPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            redirectAttributes.addFlashAttribute("error", "La nueva contraseña es demasiado larga. Usa una más corta.");
            return "redirect:/postulante/configuracion";
        }
        if (nuevaPassword.equals(passwordActual)) {
            redirectAttributes.addFlashAttribute("error", "Elige una contraseña diferente a la actual.");
            return "redirect:/postulante/configuracion";
        }
        boolean exito = postulanteService.actualizarPassword(postulanteId, passwordActual, nuevaPassword);

        if (exito) {
            redirectAttributes.addFlashAttribute("exito", "¡Contraseña actualizada! Tu cuenta ahora es más segura.");
        } else {
            redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta. Intenta de nuevo.");
        }

        return "redirect:/postulante/configuracion";
    }

    @PostMapping("/eliminar-cuenta")
    public String eliminarCuenta(
            @RequestParam("id") String id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Postulante> optPostulante = getAuthenticatedPostulante(auth);
            if (optPostulante.isEmpty() || !optPostulante.get().getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error",
                        "No tienes permiso para eliminar esta cuenta.");
                return "redirect:/postulante/configuracion";
            }
            postulanteService.eliminar(id);
            SecurityContextHolder.clearContext();
            redirectAttributes.addFlashAttribute("exito",
                    "Tu cuenta ha sido eliminada exitosamente.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al eliminar la cuenta: " + e.getMessage());
            return "redirect:/postulante/configuracion";
        }
    }
}
