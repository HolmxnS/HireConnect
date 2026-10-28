package com.project.HireConnect.service;

import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.model.NotificacionPostulante;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.repository.NotificacionPostulanteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionPostulanteService {

    private final NotificacionPostulanteRepository notificacionRepository;

    public NotificacionPostulanteService(NotificacionPostulanteRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Crea el aviso para el postulante cuando el estado de su postulacion cambia.
     */
    public void notificarCambioEstado(Postulacion postulacion, PostulacionEstado nuevoEstado) {
        if (postulacion == null || postulacion.getPostulante() == null || nuevoEstado == null) {
            return;
        }

        String titulo = (postulacion.getVacante() != null && postulacion.getVacante().getTitulo() != null)
                ? postulacion.getVacante().getTitulo()
                : "una vacante";

        NotificacionPostulante n = new NotificacionPostulante();
        n.setPostulanteId(postulacion.getPostulante().getId());
        n.setPostulacionId(postulacion.getId());
        n.setVacanteTitulo(titulo);
        n.setEstado(nuevoEstado.name());
        n.setMensaje("Tu postulación a " + titulo + " pasó a " + etiqueta(nuevoEstado));
        n.setFecha(LocalDateTime.now());
        n.setLeida(false);

        notificacionRepository.save(n);
    }

    public List<NotificacionPostulante> recientes(String postulanteId) {
        return notificacionRepository.findTop20ByPostulanteIdOrderByFechaDesc(postulanteId);
    }

    public long contarNoLeidas(String postulanteId) {
        return notificacionRepository.countByPostulanteIdAndLeidaFalse(postulanteId);
    }

    /** Solo marca la notificacion si pertenece al postulante indicado. */
    public void marcarLeida(String id, String postulanteId) {
        notificacionRepository.findByIdAndPostulanteId(id, postulanteId).ifPresent(n -> {
            if (!n.isLeida()) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        });
    }

    public void marcarTodasLeidas(String postulanteId) {
        List<NotificacionPostulante> pendientes = notificacionRepository.findByPostulanteIdAndLeidaFalse(postulanteId);
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    /** Si el postulante retira la postulacion, sus avisos ya no apuntan a nada. */
    public void eliminarPorPostulacion(String postulacionId) {
        if (postulacionId != null) {
            notificacionRepository.deleteByPostulacionId(postulacionId);
        }
    }

    // Texto con tilde para mostrar al usuario
    private String etiqueta(PostulacionEstado estado) {
        switch (estado.name()) {
            case "EN_REVISION":
                return "En revisión";
            case "ACEPTADA":
                return "Aceptada";
            case "RECHAZADA":
                return "Rechazada";
            case "PENDIENTE":
                return "Pendiente";
            default:
                String texto = estado.name().replace('_', ' ').toLowerCase();
                return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        }
    }
}