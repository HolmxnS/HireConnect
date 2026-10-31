package com.project.HireConnect.controller;

import com.project.HireConnect.model.NotificacionPostulante;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.NotificacionPostulanteService;
import com.project.HireConnect.service.PostulanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Endpoints JSON que consume la campana de la barra superior
 * (layout_postulante.js).
 */
@RestController
@RequestMapping("/postulante/notificaciones")
public class NotificacionPostulanteController {

    public record NotificacionDto(String id, String mensaje, String estado, String fecha, boolean leida,
            String enlace) {
    }

    public record NotificacionesRespuesta(long noLeidas, List<NotificacionDto> items) {
    }

    private final NotificacionPostulanteService notificacionService;
    private final PostulanteService postulanteService;

    public NotificacionPostulanteController(NotificacionPostulanteService notificacionService,
            PostulanteService postulanteService) {
        this.notificacionService = notificacionService;
        this.postulanteService = postulanteService;
    }

    private Optional<Postulante> postulanteActual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return postulanteService.buscarPorEmail(auth.getName());
    }

    private NotificacionDto aDto(NotificacionPostulante n) {
        String enlace = (n.getPostulacionId() != null)
                ? "/postulante/postulaciones?postulacion="
                        + java.net.URLEncoder.encode(n.getPostulacionId(), java.nio.charset.StandardCharsets.UTF_8)
                : "/postulante/postulaciones";
        return new NotificacionDto(n.getId(), n.getMensaje(), n.getEstado(),
                n.getFecha() != null ? n.getFecha().toString() : null, n.isLeida(), enlace);
    }

    @GetMapping
    public ResponseEntity<NotificacionesRespuesta> listar(Authentication auth) {
        Optional<Postulante> p = postulanteActual(auth);
        if (p.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        String id = p.get().getId();
        List<NotificacionDto> items = notificacionService.recientes(id).stream().map(this::aDto).toList();
        return ResponseEntity.ok(new NotificacionesRespuesta(notificacionService.contarNoLeidas(id), items));
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<Void> leerTodas(Authentication auth) {
        Optional<Postulante> p = postulanteActual(auth);
        if (p.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        notificacionService.marcarTodasLeidas(p.get().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<Void> leer(@PathVariable String id, Authentication auth) {
        Optional<Postulante> p = postulanteActual(auth);
        if (p.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        notificacionService.marcarLeida(id, p.get().getId());
        return ResponseEntity.noContent().build();
    }
}
