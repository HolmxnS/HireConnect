package com.project.HireConnect.service;

import org.springframework.stereotype.Service;
import com.project.HireConnect.dto.Notificacion;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    // Lista thread-safe compartida por todos los admins
    private final List<Notificacion> notificaciones = new CopyOnWriteArrayList<>();

    // Crear notificación
    public void crearNotificacion(String nombre, Notificacion.TipoNotificacion tipo) {
        Notificacion n = new Notificacion();
        n.setNombreRelacionado(nombre);
        n.setMensaje(
            tipo == Notificacion.TipoNotificacion.POSTULANTE
                ? "Nuevo postulante registrado"
                : "Nueva empresa registrada"
        );
        n.setTipo(tipo);
        
        notificaciones.add(n);
    }

    // Obtener notificaciones no leídas
    public List<Notificacion> obtenerNuevas() {
        return notificaciones.stream()
            .filter(n -> !n.isLeida())
            .sorted(Comparator.comparing(Notificacion::getFecha).reversed())
            .collect(Collectors.toList());
    }

    // Contar notificaciones no leídas
    public int contarNuevas() {
        return (int) notificaciones.stream()
            .filter(n -> !n.isLeida())
            .count();
    }

    // Obtener todas ordenadas por fecha
    public List<Notificacion> obtenerTodasOrdenadas() {
        return notificaciones.stream()
            .sorted(Comparator.comparing(Notificacion::getFecha).reversed())
            .collect(Collectors.toList());
    }

    // Marcar una como leída
    public void marcarUnaComoLeida(String id) {
        notificaciones.stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .ifPresent(n -> n.setLeida(true));
    }

    // Marcar todas como leídas
    public void marcarTodasComoLeidas() {
        notificaciones.forEach(n -> n.setLeida(true));
    }

    // Eliminar una notificación
    public void eliminarUna(String id) {
        notificaciones.removeIf(n -> n.getId().equals(id));
    }

    // Eliminar todas las notificaciones
    public void eliminarTodas() {
        notificaciones.clear();
    }
}