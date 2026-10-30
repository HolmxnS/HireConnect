package com.project.HireConnect.controller;

import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.dto.PrediccionRequestDTO;
import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.enums.VacanteExperienciaRequerida;
import com.project.HireConnect.enums.VacanteModalidad;
import com.project.HireConnect.enums.VacanteTipocontrato;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.EmpresaResumen;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.VacanteService;

import jakarta.validation.Valid;

import com.project.HireConnect.service.EmpresaService;
import com.project.HireConnect.service.PostulanteService;
import com.project.HireConnect.service.PredictionService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/empresa/vacantes")
public class VacanteController {

    private final VacanteService vacanteService;
    private final EmpresaService empresaService;
    private final PostulanteService postulanteService;
    private final PredictionService predictionService;

    public VacanteController(VacanteService vacanteService, EmpresaService empresaService,
            PostulanteService postulanteService, PredictionService predictionService) {
        this.vacanteService = vacanteService;
        this.empresaService = empresaService;
        this.postulanteService = postulanteService;
        this.predictionService = predictionService;
    }

    private Optional<Empresa> getAuthenticatedEmpresa(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return Optional.empty();
        }
        return empresaService.buscarPorEmailEmpresa(auth.getName());
    }

    @ModelAttribute
    public void cargarOpcionesSelects(Model model) {
        model.addAttribute("modalidades", VacanteModalidad.values());
        model.addAttribute("tiposContrato", VacanteTipocontrato.values());
        model.addAttribute("nivelesExperiencia", VacanteExperienciaRequerida.values());
    }

    @GetMapping
    public String listarVacantesEmpresa(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);

        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada o perfil de empresa no encontrado.");
            return "redirect:/login";
        }

        Empresa empresa = empresaOpt.get();
        List<Vacante> vacantesFiltradas = vacanteService.buscarPorEmpresa(empresa).stream()
                .filter(java.util.Objects::nonNull)
                .toList();

        model.addAttribute("empresa", empresa);
        model.addAttribute("vacantes", vacantesFiltradas);
        model.addAttribute("activeMenu", "vacantes");

        return "empresa/dashboard_vacantes";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);

        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada o perfil de empresa no encontrado.");
            return "redirect:/login";
        }

        Empresa empresa = empresaOpt.get();
        Vacante vacante = new Vacante();
        vacante.setEmpresa(new EmpresaResumen(empresa.getId(), empresa.getNombreLegal(), empresa.getFoto()));

        model.addAttribute("vacante", vacante);
        model.addAttribute("empresa", empresa);

        return "empresa/nueva_vacante";
    }

    @PostMapping("/guardar")
    public String guardarVacante(
            @Valid @ModelAttribute("vacante") Vacante vacante,
            BindingResult result,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);
        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada. Por favor, inicie sesión nuevamente.");
            return "redirect:/login";
        }
        Empresa empresaPublicadora = empresaOpt.get();

        if (vacante.getFechaInicio() != null && vacante.getFechaFin() != null
                && vacante.getFechaFin().isBefore(vacante.getFechaInicio())) {
            result.rejectValue("fechaFin", "vacante.fechas", "El cierre debe ser igual o posterior a la apertura.");
        }

        if (result.hasErrors()) {
            model.addAttribute("vacante", vacante);
            model.addAttribute("empresa", empresaPublicadora);
            return (vacante.getId() == null || vacante.getId().isBlank()) ? "empresa/nueva_vacante" : "empresa/editar_vacante";
        }

        try {
            if (vacante.getId() == null || vacante.getId().trim().isEmpty()) {
                vacante.setId(null);
                vacante.setEmpresa(new EmpresaResumen(empresaPublicadora.getId(), empresaPublicadora.getNombreLegal(),
                        empresaPublicadora.getFoto()));

                if (vacante.getFechaPublicacion() == null) {
                    vacante.setFechaPublicacion(LocalDate.now());
                }
                if (vacante.getFechaInicio() != null && vacante.getFechaInicio().isAfter(LocalDate.now())) {
                    vacante.setEstado(VacanteEstado.PENDIENTE);
                } else {
                    vacante.setEstado(VacanteEstado.ACTIVA);
                }

                vacanteService.guardar(vacante);
            } else {
                Vacante existente = vacanteService.buscarPorId(vacante.getId())
                        .orElseThrow(() -> new RuntimeException("Vacante no encontrada con ID: " + vacante.getId()));
                existente.setTitulo(vacante.getTitulo());
                existente.setDescripcion(vacante.getDescripcion());
                existente.setHabilidadesRequeridas(vacante.getHabilidadesRequeridas());
                existente.setSalario(vacante.getSalario());
                existente.setTipoContrato(vacante.getTipoContrato());
                existente.setModalidad(vacante.getModalidad());
                existente.setExperienciaRequerida(vacante.getExperienciaRequerida());
                existente.setFechaInicio(vacante.getFechaInicio());
                existente.setFechaFin(vacante.getFechaFin());

                LocalDate hoy = LocalDate.now();
                if (existente.getFechaInicio().isAfter(hoy)) {
                    existente.setEstado(VacanteEstado.PENDIENTE);
                } else if (existente.getFechaFin() != null && existente.getFechaFin().isBefore(hoy)) {
                    existente.setEstado(VacanteEstado.CERRADA);
                } else {
                    existente.setEstado(VacanteEstado.ACTIVA);
                }

                vacanteService.guardar(existente);
            }

            redirectAttributes.addFlashAttribute("success", "Vacante procesada con éxito");
            return "redirect:/empresa/vacantes";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("vacante", vacante);
            model.addAttribute("empresa", empresaPublicadora);
            model.addAttribute("error", "Error al procesar la vacante: " + e.getMessage());
            return (vacante.getId() == null) ? "empresa/nueva_vacante" : "empresa/editar_vacante";
        }
    }

    @GetMapping("/editar/{id}")
    public String editarVacante(@PathVariable String id,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);

        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada.");
            return "redirect:/login";
        }

        Empresa empresaSesion = empresaOpt.get();
        Vacante vacante = vacanteService.buscarPorId(id).orElse(null);

        if (vacante == null || !vacante.getEmpresa().getId().equals(empresaSesion.getId())) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado o vacante no encontrada.");
            return "redirect:/empresa/vacantes";
        }

        model.addAttribute("vacante", vacante);
        model.addAttribute("empresa", empresaSesion);
        return "empresa/editar_vacante";
    }

    @GetMapping("/activar/{id}")
    public String activarVacante(@PathVariable String id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);

        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada.");
            return "redirect:/login";
        }

        Empresa empresaSesion = empresaOpt.get();

        try {
            Vacante vacante = vacanteService.buscarPorId(id).orElse(null);

            if (vacante == null) {
                redirectAttributes.addFlashAttribute("error", "Vacante no encontrada.");
                return "redirect:/empresa/vacantes";
            }
            if (!vacante.getEmpresa().getId().equals(empresaSesion.getId())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permiso para activar esta vacante.");
                return "redirect:/empresa/vacantes";
            }
            if (vacante.getEstado() != VacanteEstado.CERRADA) {
                redirectAttributes.addFlashAttribute("error", "Solo se pueden activar vacantes cerradas.");
                return "redirect:/empresa/vacantes";
            }
            LocalDate hoy = LocalDate.now();

            if (vacante.getFechaFin() != null && vacante.getFechaFin().isBefore(hoy)) {
                redirectAttributes.addFlashAttribute("error",
                        "No se puede activar una vacante cuya fecha de fin ya expiró. " +
                                "Por favor, edita la vacante y actualiza las fechas.");
                return "redirect:/empresa/vacantes";
            }
            if (vacante.getFechaInicio().isAfter(hoy)) {
                vacante.setEstado(VacanteEstado.PENDIENTE);
                redirectAttributes.addFlashAttribute("success",
                        "Vacante programada. Se activará automáticamente el " + vacante.getFechaInicio());
            } else {
                vacante.setEstado(VacanteEstado.ACTIVA);
                redirectAttributes.addFlashAttribute("success", "Vacante activada exitosamente.");
            }

            vacanteService.guardar(vacante);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al activar la vacante: " + e.getMessage());
        }

        return "redirect:/empresa/vacantes";
    }

    @GetMapping("/cerrar/{id}")
    public String cerrarVacante(@PathVariable String id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Optional<Empresa> empresaOpt = getAuthenticatedEmpresa(auth);

        if (empresaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sesión expirada.");
            return "redirect:/login";
        }

        Empresa empresaSesion = empresaOpt.get();
        Vacante vacante = vacanteService.buscarPorId(id).orElse(null);

        if (vacante == null || !vacante.getEmpresa().getId().equals(empresaSesion.getId())) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado o vacante no encontrada.");
            return "redirect:/empresa/vacantes";
        }
        vacante.setEstado(VacanteEstado.CERRADA);
        vacanteService.guardar(vacante);

        redirectAttributes.addFlashAttribute("success", "La vacante ha sido cerrada");
        return "redirect:/empresa/vacantes";
    }

    @GetMapping("/listar")
    public String listarVacantesPublicas(
            @RequestParam(value = "filtro", required = false) String filtro,
            @RequestParam(value = "modalidad", required = false) String modalidad,
            @RequestParam(value = "tipoContrato", required = false) String tipoContrato,
            @RequestParam(value = "fechaFiltro", required = false) String fechaFiltro,
            @RequestParam(value = "salarioMin", required = false) Double salarioMin,
            @RequestParam(value = "salarioMax", required = false) Double salarioMax,
            Authentication authentication,
            Model model) {

        // Los enlaces antiguos también usan el listado paginado de postulantes.
        var destino = org.springframework.web.util.UriComponentsBuilder.fromPath("/postulante/vacantes/listar")
                .queryParamIfPresent("filtro", Optional.ofNullable(filtro))
                .queryParamIfPresent("modalidad", Optional.ofNullable(modalidad))
                .queryParamIfPresent("tipoContrato", Optional.ofNullable(tipoContrato))
                .queryParamIfPresent("fechaFiltro", Optional.ofNullable(fechaFiltro))
                .queryParamIfPresent("salarioMin", Optional.ofNullable(salarioMin))
                .queryParamIfPresent("salarioMax", Optional.ofNullable(salarioMax));
        return "redirect:" + destino.build().encode().toUriString();
    }

    @GetMapping("/{id}")
    public String verDetalleVacante(@PathVariable String id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Vacante vacante = vacanteService.buscarPorId(id).orElse(null);

        if (vacante == null) {
            redirectAttributes.addFlashAttribute("error", "La vacante no existe o fue eliminada.");
            return "redirect:/vacantes/listar";
        }

        String email = authentication.getName();
        Optional<Postulante> postulanteOpt = postulanteService.buscarPorEmail(email);

        if (postulanteOpt.isEmpty()) {
            return "redirect:/logout";
        }

        PrediccionRequestDTO dto = new PrediccionRequestDTO();
        // Esta integración todavía envía valores de prueba, no datos del perfil real.
        dto.setEdad(22);
        dto.setExperienciaAnos(2);
        dto.setCoincidenciaHabilidades(80);
        dto.setModalidad(vacante.getModalidad() != null ? vacante.getModalidad().name() : "");
        dto.setTipoContrato(vacante.getTipoContrato() != null ? vacante.getTipoContrato().name() : "");

        Map<String, Object> prediccion = predictionService.obtenerPrediccion(dto);

        model.addAttribute("vacante", vacante);
        model.addAttribute("postulante", postulanteOpt.get());
        model.addAttribute("probabilidad", prediccion.get("probabilidad"));
        model.addAttribute("compatibilidad", prediccion.get("compatibilidad"));

        return "postulante/detalle_vacante";
    }
}
