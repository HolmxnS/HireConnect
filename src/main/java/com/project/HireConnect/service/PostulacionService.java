package com.project.HireConnect.service;

import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.repository.PostulacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final NotificacionPostulanteService notificacionService;

    public PostulacionService(PostulacionRepository postulacionRepository,
            NotificacionPostulanteService notificacionService) {
        this.postulacionRepository = postulacionRepository;
        this.notificacionService = notificacionService;
    }

    public long contarPostulacionesDeHoy() {
        return postulacionRepository.countByFechaPostulacionHoy();
    }

    public int contarPostulaciones() {
        return (int) postulacionRepository.count();
    }

    public long contarPorVacante(Vacante vacante) {
        return postulacionRepository.countByVacanteId(vacante.getId());
    }

    public int postulantesEnRevision() {
        return postulacionRepository.countByEstado(PostulacionEstado.EN_REVISION);
    }

    public int postulantesAceptados() {
        return postulacionRepository.countByEstado(PostulacionEstado.ACEPTADA);
    }

    public List<Postulacion> buscarPorPostulante(Postulante postulante) {
        return postulacionRepository.findByPostulante(postulante);
    }

    public List<Postulacion> listarTodas() {
        return postulacionRepository.findAll();
    }

    public Optional<Postulacion> buscarPorId(String id) {
        return postulacionRepository.findById(id);
    }

    public List<Postulacion> buscarPorVacante(Vacante vacante) {
        return postulacionRepository.findByVacante(vacante);
    }

    public List<Postulacion> buscarPorVacantes(List<Vacante> vacantes) {
        return vacantes.isEmpty() ? List.of() : postulacionRepository.findByVacanteIn(vacantes);
    }

    public List<Postulacion> buscarPorEmpresa(Empresa empresa) {
        return postulacionRepository.findByVacante_Empresa(empresa);
    }

    public Postulacion guardar(Postulacion postulacion) {
        return postulacionRepository.save(postulacion);
    }

    // Elimina también los avisos para evitar enlaces a postulaciones retiradas.
    public void eliminar(String id) {
        postulacionRepository.deleteById(id);
        notificacionService.eliminarPorPostulacion(id);
    }

    public boolean existePostulacion(Postulante postulante, Vacante vacante) {
        if (postulante == null || vacante == null)
            return false;
        return postulacionRepository.existsByPostulanteAndVacante(postulante, vacante);
    }

    public void actualizarEstado(String id, String nuevoEstado) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postulación no encontrada con ID: " + id));

        PostulacionEstado estadoEnum;
        try {
            estadoEnum = PostulacionEstado.fromDescripcion(nuevoEstado);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado inválido: " + nuevoEstado);
        }

        boolean cambio = postulacion.getEstado() != estadoEnum;

        postulacion.setEstado(estadoEnum);
        postulacionRepository.save(postulacion);

        // Avisa al postulante solo si el estado realmente cambio
        if (cambio) {
            notificacionService.notificarCambioEstado(postulacion, estadoEnum);
        }
    }

    public Optional<Postulacion> ultimaPostulacion(Postulante postulante) {
        if (postulante == null || postulante.getId() == null) {
            return Optional.empty();
        }
        return postulacionRepository.findFirstByPostulanteIdOrderByFechaPostulacionDesc(postulante.getId());
    }
}
