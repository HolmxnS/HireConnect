package com.project.HireConnect.controller;

import com.project.HireConnect.dto.FormulariosOportunidades.*;
import com.project.HireConnect.service.*;
import com.project.HireConnect.model.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.OptimisticLockingFailureException;

@Controller
@RequestMapping("/{rol:empresa|postulante}")
public class OportunidadesController {
    private final OportunidadesService oportunidades;
    private final EmpresaService empresas;
    private final PostulanteService postulantes;
    public OportunidadesController(OportunidadesService oportunidades, EmpresaService empresas, PostulanteService postulantes) {
        this.oportunidades = oportunidades; this.empresas = empresas; this.postulantes = postulantes;
    }
    private Empresa empresa(Authentication auth) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return empresas.buscarPorEmailEmpresa(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
    private Postulante postulante(Authentication auth) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return postulantes.buscarPorEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
    private String preparar(String rol, Authentication auth, Model model) {
        boolean esEmpresa = "empresa".equals(rol);
        model.addAttribute("esEmpresa", esEmpresa); model.addAttribute("base", "/" + rol);
        if (esEmpresa) { var e = empresa(auth); model.addAttribute("empresa", e); return e.getId(); }
        var p = postulante(auth); model.addAttribute("postulante", p); return p.getId();
    }
    private void exigirEmpresa(String rol) {
        if (!"empresa".equals(rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private void exigirPostulante(String rol) {
        if (!"postulante".equals(rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    @GetMapping("/entrevistas")
    public String entrevistas(@PathVariable String rol, Authentication auth, Model model,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(required=false) String postulacion) {
        String id = preparar(rol, auth, model);
        model.addAttribute("pagina", oportunidades.entrevistas(id, "empresa".equals(rol), pagina));
        if ("empresa".equals(rol) && postulacion != null) {
            model.addAttribute("candidatura", oportunidades.candidatura(postulacion, id));
            try { model.addAttribute("cita", oportunidades.entrevista(postulacion, id, true)); }
            catch (ResponseStatusException ex) { if (ex.getStatusCode().value() != 404) throw ex; }
        }
        model.addAttribute("activeMenu", "entrevistas"); return "oportunidades/entrevistas";
    }
    @PostMapping("/entrevistas/programar")
    public String programar(@PathVariable String rol, @Valid @ModelAttribute Cita form, BindingResult errors,
            Authentication auth, RedirectAttributes flash) {
        exigirEmpresa(rol);
        if (errors.hasErrors()) flash.addFlashAttribute("error", "Revisa fecha, duración, lugar e indicaciones de la entrevista.");
        else { oportunidades.programar(empresa(auth), form); flash.addFlashAttribute("exito", "Invitación enviada. El candidato debe confirmar el horario."); }
        return "redirect:/empresa/entrevistas";
    }
    @PostMapping("/entrevistas/{id}/responder")
    public String responder(@PathVariable String rol, @PathVariable String id, @RequestParam long version,
            @RequestParam String accion, @RequestParam(defaultValue="") String mensaje, Authentication auth, RedirectAttributes flash) {
        exigirPostulante(rol); oportunidades.responder(id, postulante(auth).getId(), version, accion, mensaje);
        flash.addFlashAttribute("exito", "Respuesta guardada. La empresa ya puede consultarla."); return "redirect:/postulante/entrevistas";
    }
    @PostMapping("/entrevistas/{id}/cancelar")
    public String cancelarCita(@PathVariable String rol, @PathVariable String id, @RequestParam long version,
            Authentication auth, RedirectAttributes flash) {
        exigirEmpresa(rol); oportunidades.cancelarEntrevista(id, empresa(auth).getId(), version);
        flash.addFlashAttribute("exito", "Entrevista cancelada. Se avisó al candidato."); return "redirect:/empresa/entrevistas";
    }
    @GetMapping("/proyectos")
    public String proyectos(@PathVariable String rol, Authentication auth, Model model,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="") String categoria, @RequestParam(defaultValue="false") boolean propios) {
        String id = preparar(rol, auth, model);
        model.addAttribute("pagina", oportunidades.proyectos(id, "empresa".equals(rol), propios, q, categoria, pagina));
        model.addAttribute("q", q); model.addAttribute("categoria", categoria); model.addAttribute("propios", propios);
        model.addAttribute("categorias", OportunidadesService.CATEGORIAS); model.addAttribute("activeMenu", "proyectos");
        return "oportunidades/proyectos";
    }
    @PostMapping("/proyectos")
    public String publicar(@PathVariable String rol, @Valid @ModelAttribute Proyecto form, BindingResult errors,
            Authentication auth, RedirectAttributes flash) {
        exigirEmpresa(rol);
        if (errors.hasErrors()) flash.addFlashAttribute("error", "Completa los datos. El presupuesto debe ser de al menos $1.000 COP y el plazo de 1 a 365 días.");
        else { oportunidades.publicar(empresa(auth), form); flash.addFlashAttribute("exito", "Proyecto publicado."); }
        return "redirect:/empresa/proyectos";
    }
    @GetMapping("/proyectos/{id}")
    public String detalle(@PathVariable String rol, @PathVariable String id, Authentication auth, Model model) {
        String actorId = preparar(rol, auth, model); boolean esEmpresa = "empresa".equals(rol);
        var p = oportunidades.proyecto(id, actorId, esEmpresa);
        model.addAttribute("proyecto", p);
        var propuestas = p.getPropuestas().stream().filter(x -> esEmpresa || actorId.equals(x.getPostulanteId())).toList();
        model.addAttribute("propuestas", propuestas); model.addAttribute("yaPropuso", !propuestas.isEmpty());
        var seleccionada = p.getPropuestas().stream()
                .filter(x -> x.getEstado() == PropuestaFreelance.Estado.SELECCIONADA).findFirst();
        if (seleccionada.isPresent()) {
            if (esEmpresa) postulantes.buscarPorId(seleccionada.get().getPostulanteId())
                    .ifPresent(x -> model.addAttribute("contactoEmail", x.getEmail()));
            else if (actorId.equals(seleccionada.get().getPostulanteId())) empresas.obtenerPorId(p.getEmpresaId())
                    .ifPresent(x -> model.addAttribute("contactoEmail", x.getEmail()));
        }

        model.addAttribute("activeMenu", "proyectos"); return "oportunidades/proyecto";
    }
    @PostMapping("/proyectos/{id}/propuestas")
    public String proponer(@PathVariable String rol, @PathVariable String id, @Valid @ModelAttribute Propuesta form,
            BindingResult errors, Authentication auth, RedirectAttributes flash) {
        exigirPostulante(rol);
        if (errors.hasErrors()) flash.addFlashAttribute("error", "Revisa tu mensaje, precio y plazo de entrega.");
        else { oportunidades.proponer(id, postulante(auth), form); flash.addFlashAttribute("exito", "Propuesta enviada."); }
        return "redirect:/postulante/proyectos/" + id;
    }
    @PostMapping("/proyectos/{id}/seleccionar")
    public String seleccionar(@PathVariable String rol, @PathVariable String id, @RequestParam String propuestaId,
            @RequestParam long version, Authentication auth, RedirectAttributes flash) {
        exigirEmpresa(rol); oportunidades.seleccionar(id, propuestaId, empresa(auth).getId(), version);
        flash.addFlashAttribute("exito", "Profesional seleccionado. El proyecto está en curso."); return "redirect:/empresa/proyectos/" + id;
    }
    @PostMapping("/proyectos/{id}/cerrar")
    public String cerrar(@PathVariable String rol, @PathVariable String id, @RequestParam long version,
            @RequestParam(defaultValue="false") boolean finalizar, Authentication auth, RedirectAttributes flash) {
        exigirEmpresa(rol); oportunidades.cerrarProyecto(id, empresa(auth).getId(), version, finalizar);
        flash.addFlashAttribute("exito", finalizar ? "Proyecto finalizado." : "Proyecto cancelado."); return "redirect:/empresa/proyectos/" + id;
    }
    @ExceptionHandler({ResponseStatusException.class, OptimisticLockingFailureException.class})
    public Object error(RuntimeException ex, HttpServletRequest request, RedirectAttributes flash) {
        if (!"POST".equals(request.getMethod())) {
            var status = ex instanceof ResponseStatusException r ? r.getStatusCode() : HttpStatus.CONFLICT;
            return org.springframework.http.ResponseEntity.status(status).body("No se pudo acceder a esta información.");
        }
        String mensaje = ex instanceof ResponseStatusException r && r.getReason() != null ? r.getReason() : "La información cambió. Recarga la página e inténtalo de nuevo.";
        flash.addFlashAttribute("error", mensaje);
        String rol = request.getRequestURI().startsWith("/empresa/") ? "empresa" : "postulante";
        return new org.springframework.web.servlet.view.RedirectView("/" + rol + (request.getRequestURI().contains("/entrevistas") ? "/entrevistas" : "/proyectos"), true);
    }
}
