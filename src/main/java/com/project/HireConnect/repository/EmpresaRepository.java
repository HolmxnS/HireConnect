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

    @Query("{ $expr: { $and: [ " +
           "{ $eq: [{ $month: '$fecha_registro' }, { $month: new Date() }] }, " +
           "{ $eq: [{ $year: '$fecha_registro' }, { $year: new Date() }] } " +
           "] } }")
    List<Empresa> findEmpresasRegistradasMesActual();
}