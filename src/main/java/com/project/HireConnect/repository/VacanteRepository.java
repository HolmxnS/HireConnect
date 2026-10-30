package com.project.HireConnect.repository;

import com.project.HireConnect.model.Vacante;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.project.HireConnect.enums.VacanteEstado;

import java.util.List;

@Repository
public interface VacanteRepository extends MongoRepository<Vacante, String> {
       List<Vacante> findByEstado(VacanteEstado estado);

       @Query("{ 'estado': 'ACTIVA', '$or': [ { 'titulo': { '$regex': ?0, '$options': 'i' } }, { 'habilidadesRequeridas': { '$regex': ?0, '$options': 'i' } } ] }")
       List<Vacante> buscarParaAsistente(String patron, org.springframework.data.domain.Pageable pagina);

    
    @Query("{ $and: [ " +
           "{ $or: [ { ?0 : null }, { 'titulo': { $regex: ?0, $options: 'i' } } ] }, " +
           "{ $or: [ { ?1 : null }, { 'modalidad': ?1 } ] }, " +
           "{ $or: [ { ?2 : null }, { 'salario': { $gte: ?2 } } ] }, " +
           "{ $or: [ { ?3 : null }, { 'salario': { $lte: ?3 } } ] } " +
           "] }")
    List<Vacante> buscarPorFiltros(String titulo, String modalidad, Double salarioMin, Double salarioMax);

    @Query("{ $and: [ " +
           "{ $or: [ { ?0 : null }, { 'titulo': { $regex: ?0, $options: 'i' } } ] }, " +
           "{ $or: [ { ?1 : null }, { 'modalidad': ?1 } ] } " +
           "] }")
    List<Vacante> buscarPorTituloYModalidad(String titulo, String modalidad);

    
    @Query("{ 'estado': 'PENDIENTE', 'fechaInicio': { $lte: new Date() } }")
    List<Vacante> findVacantesParaActivarHoy();

    
    @Query("{ 'estado': 'ACTIVA', 'fechaFin': { $lt: new Date() } }")
    List<Vacante> findVacantesParaCerrarHoy();
}
