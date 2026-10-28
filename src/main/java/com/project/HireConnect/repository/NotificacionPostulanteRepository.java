package com.project.HireConnect.repository;

import com.project.HireConnect.model.NotificacionPostulante;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionPostulanteRepository extends MongoRepository<NotificacionPostulante, String> {

    List<NotificacionPostulante> findTop20ByPostulanteIdOrderByFechaDesc(String postulanteId);

    long countByPostulanteIdAndLeidaFalse(String postulanteId);

    List<NotificacionPostulante> findByPostulanteIdAndLeidaFalse(String postulanteId);

    Optional<NotificacionPostulante> findByIdAndPostulanteId(String id, String postulanteId);

    void deleteByPostulacionId(String postulacionId);
}