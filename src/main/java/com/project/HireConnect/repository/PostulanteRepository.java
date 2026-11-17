package com.project.HireConnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.HireConnect.model.Postulante;

@Repository
public interface PostulanteRepository extends MongoRepository<Postulante, String> {

    Optional<Postulante> findByEmail(String email);

    @Query("{ '_id': ?0 }")
    @org.springframework.data.mongodb.repository.Update("{ '$set': { 'ocultarInvitacionPerfil': ?1 } }")
    void actualizarInvitacionPerfil(String id, boolean ocultar);


    @Query(value = "{ 'cv' : { $ne : null } }", count = true)
    long countByCvIsNotNull();

    
    @Query("{ $expr: { $and: [ " +
           "{ $eq: [{ $month: '$fecha_registro' }, { $month: new Date() }] }, " +
           "{ $eq: [{ $year: '$fecha_registro' }, { $year: new Date() }] } " +
           "] } }")
    List<Postulante> findPostulantesRegistradosMesActual();

}
