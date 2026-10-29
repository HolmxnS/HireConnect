package com.project.HireConnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.model.Empresa;

@Repository
public interface EmpresaRepository extends MongoRepository<Empresa, String> {

    Optional<Empresa> findByEmail(String email);

    Optional<Empresa> findByRut(String rut);

    int countByEstado(EmpresaEstado estado);

    @Query(value = "{ '$or': [ { 'nombreLegal': { '$regex': ?0, '$options': 'i' } }, { 'email': { '$regex': ?0, '$options': 'i' } }, { 'rut': { '$regex': ?0, '$options': 'i' } } ] }",
            fields = "{ 'nombreLegal': 1, 'email': 1, 'rut': 1, 'estado': 1, 'fechaRegistro': 1 }")
    org.springframework.data.domain.Page<Empresa> buscarPaginaAdmin(String texto, org.springframework.data.domain.Pageable pagina);

    @Query(value = "{ 'estado': ?1, '$or': [ { 'nombreLegal': { '$regex': ?0, '$options': 'i' } }, { 'email': { '$regex': ?0, '$options': 'i' } }, { 'rut': { '$regex': ?0, '$options': 'i' } } ] }",
            fields = "{ 'nombreLegal': 1, 'email': 1, 'rut': 1, 'estado': 1, 'fechaRegistro': 1 }")
    org.springframework.data.domain.Page<Empresa> buscarPaginaAdminPorEstado(String texto, EmpresaEstado estado, org.springframework.data.domain.Pageable pagina);

    @Query("{ $expr: { $and: [ " +
           "{ $eq: [{ $month: '$fecha_registro' }, { $month: new Date() }] }, " +
           "{ $eq: [{ $year: '$fecha_registro' }, { $year: new Date() }] } " +
           "] } }")
    List<Empresa> findEmpresasRegistradasMesActual();
}
