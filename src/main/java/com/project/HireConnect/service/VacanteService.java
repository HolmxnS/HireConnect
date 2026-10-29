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

    public org.springframework.data.domain.Page<Vacante> paginaAdmin(String texto, String estado, String orden, int numero) {
        String patron = java.util.regex.Pattern.quote(texto.trim());
        List<Criteria> filtros = new ArrayList<>();
        filtros.add(new Criteria().orOperator(Criteria.where("titulo").regex(patron, "i"),
                Criteria.where("empresa.nombre").regex(patron, "i")));
        if (List.of("ACTIVA", "PENDIENTE", "CERRADA").contains(estado))
            filtros.add(Criteria.where("estado").is(VacanteEstado.valueOf(estado)));
        Criteria criteria = new Criteria().andOperator(filtros.toArray(Criteria[]::new));
        long total = mongoTemplate.count(Query.query(criteria), Vacante.class);
        int paginas = (int) Math.max(1, (total + 19) / 20);
        int actual = Math.max(0, Math.min(numero, paginas - 1));
        var direccion = "oldest".equals(orden) ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        var pageable = org.springframework.data.domain.PageRequest.of(actual, 20,
                org.springframework.data.domain.Sort.by(direccion, "fechaPublicacion").and(org.springframework.data.domain.Sort.by(direccion, "id")));
        Query query = Query.query(criteria).with(pageable);
        query.fields().include("titulo", "empresa", "fechaPublicacion", "estado");
        List<Vacante> vacantes = total == 0 ? List.of() : mongoTemplate.find(query, Vacante.class);
        return new org.springframework.data.domain.PageImpl<>(vacantes, pageable, total);
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

        List<Vacante> candidatas = mongoTemplate.find(
                new Query(Criteria.where("estado").is(VacanteEstado.ACTIVA)),
                Vacante.class);

        // Ordena por coincidencias textuales de habilidades; no calcula una probabilidad.
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
