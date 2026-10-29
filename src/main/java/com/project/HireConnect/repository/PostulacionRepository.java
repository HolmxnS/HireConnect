package com.project.HireConnect.repository;

import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostulacionRepository extends MongoRepository<Postulacion, String> {

    @Query(value = "{ $expr: { $eq: [ " +
            "{ $dateToString: { format: '%Y-%m-%d', date: '$fecha_postulacion' } }, " +
            "{ $dateToString: { format: '%Y-%m-%d', date: new Date() } } " +
            "] } }", count = true)
    long countByFechaPostulacionHoy();

    List<Postulacion> findByVacante(Vacante vacante);

    List<Postulacion> findByVacanteIn(List<Vacante> vacantes);

    List<Postulacion> findByPostulante(Postulante postulante);

    List<Postulacion> findByVacante_Empresa(Empresa empresa);

    boolean existsByPostulanteAndVacante(Postulante postulante, Vacante vacante);

    int countByEstado(PostulacionEstado estado);

    Optional<Postulacion> findFirstByPostulanteIdOrderByFechaPostulacionDesc(String postulanteId);

    @Query(value = "{ 'vacanteId.$id': { $oid: ?0 } }", count = true)
    long countByVacanteId(String vacanteId);
}
