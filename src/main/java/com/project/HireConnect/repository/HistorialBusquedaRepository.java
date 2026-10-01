package com.project.HireConnect.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.project.HireConnect.model.HistorialBusqueda;

public interface HistorialBusquedaRepository extends MongoRepository<HistorialBusqueda, String> {

    List<HistorialBusqueda> findTop5ByEmailOrderByFechaDesc(String email);
    void deleteByEmail(String email);
}
