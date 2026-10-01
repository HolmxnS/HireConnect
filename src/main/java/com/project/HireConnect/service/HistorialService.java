package com.project.HireConnect.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.HireConnect.model.HistorialBusqueda;
import com.project.HireConnect.repository.HistorialBusquedaRepository;

@Service
public class HistorialService {

    private final HistorialBusquedaRepository repo;

    public HistorialService(HistorialBusquedaRepository repo) {
        this.repo = repo;
    }

    public void guardar(String email, String texto) {
        HistorialBusqueda h = new HistorialBusqueda();
        h.setEmail(email);
        h.setTexto(texto);
        repo.save(h);
    }

    public List<String> obtenerHistorial(String email) {
        return repo.findTop5ByEmailOrderByFechaDesc(email)
                .stream()
                .map(HistorialBusqueda::getTexto)
                .distinct()
                .toList();
    }

    public void limpiar(String email) {
        repo.deleteByEmail(email);
    }
}
