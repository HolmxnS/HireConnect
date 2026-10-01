package com.project.HireConnect.service;

import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.repository.VacanteRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class VacanteService {

    private final VacanteRepository vacanteRepository;
    private final MongoTemplate mongoTemplate;

    public VacanteService(VacanteRepository vacanteRepository, MongoTemplate mongoTemplate) {
        this.vacanteRepository = vacanteRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public long contarVacantes() {
        return vacanteRepository.count();
    }

    public Vacante guardar(Vacante vacante) {
        return vacanteRepository.save(vacante);
    }

    public List<Vacante> listarTodas() {
        return vacanteRepository.findAll();
    }

    public Optional<Vacante> buscarPorId(String id) {
        return vacanteRepository.findById(id);
    }

    public List<Vacante> buscarPorEmpresa(Empresa empresa) {
        Query query = new Query(Criteria.where("empresa._id").is(new org.bson.types.ObjectId(empresa.getId())));
        return mongoTemplate.find(query, Vacante.class);
    }

    public void eliminar(String id) {
        vacanteRepository.deleteById(id);
    }

    public List<Vacante> buscarPorFiltros(String titulo, String modalidad, Double salarioMin, Double salarioMax) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (titulo != null && !titulo.isBlank()) {
            criteriaList.add(Criteria.where("titulo").regex(titulo, "i"));
        }

        if (modalidad != null && !modalidad.isBlank()) {
            criteriaList.add(Criteria.where("modalidad").is(modalidad));
        }

        if (salarioMin != null) {
            criteriaList.add(Criteria.where("salario").gte(salarioMin));
        }

        if (salarioMax != null) {
            criteriaList.add(Criteria.where("salario").lte(salarioMax));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Vacante.class);
    }

    public List<Vacante> buscarPorTituloYModalidad(String titulo, String modalidad) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (titulo != null && !titulo.isBlank()) {
            criteriaList.add(Criteria.where("titulo").regex(titulo, "i"));
        }

        if (modalidad != null && !modalidad.isBlank()) {
            criteriaList.add(Criteria.where("modalidad").is(modalidad));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Vacante.class);
    }

    public void actualizarEstadosVacantes() {
        List<Vacante> porActivar = vacanteRepository.findVacantesParaActivarHoy();
        for (Vacante v : porActivar) {
            v.setEstado(VacanteEstado.ACTIVA);
            vacanteRepository.save(v);
        }

        List<Vacante> porCerrar = vacanteRepository.findVacantesParaCerrarHoy();
        for (Vacante v : porCerrar) {
            v.setEstado(VacanteEstado.CERRADA);
            vacanteRepository.save(v);
        }
    }

    public List<Vacante> recomendarVacantes(Set<String> habilidades) {
        if (habilidades == null || habilidades.isEmpty()) {
            return List.of();
        }

        // Buscar vacantes activas donde habilidadesRequeridas contenga alguna habilidad
        List<Vacante> candidatas = mongoTemplate.find(
                new Query(Criteria.where("estado").is(VacanteEstado.ACTIVA)),
                Vacante.class);

        // Ordenar por cantidad de coincidencias
        return candidatas.stream()
                .filter(v -> v.getHabilidadesRequeridas() != null)
                .map(v -> {
                    long coincidencias = habilidades.stream()
                            .filter(h -> v.getHabilidadesRequeridas().toLowerCase().contains(h.toLowerCase()))
                            .count();
                    return new Object[] { v, coincidencias };
                })
                .filter(pair -> (long) pair[1] > 0)
                .sorted((a, b) -> Long.compare((long) b[1], (long) a[1]))
                .map(pair -> (Vacante) pair[0])
                .limit(6)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> sugerenciasTitulo(String texto) {
        Query query = new Query();

        query.addCriteria(Criteria.where("titulo").regex(texto, "i"));
        query.limit(5);

        List<Vacante> vacantes = mongoTemplate.find(query, Vacante.class);

        return vacantes.stream()
                .map(Vacante::getTitulo)
                .distinct()
                .toList();
    }
}