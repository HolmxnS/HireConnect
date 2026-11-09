package com.project.HireConnect.service;

import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.dto.FiltrosVacantes;
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

    private static final int VACANTES_POR_PAGINA_POSTULANTE = 15;

    private final VacanteRepository vacanteRepository;
    private final MongoTemplate mongoTemplate;

    public VacanteService(VacanteRepository vacanteRepository, MongoTemplate mongoTemplate) {
        this.vacanteRepository = vacanteRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public long contarVacantes() {
        return vacanteRepository.count();
    }

    public java.util.Map<String, Long> resumenAdmin() {
        var resultado = new java.util.LinkedHashMap<String, Long>();
        for (String clave : List.of("total", "ACTIVA", "PENDIENTE", "CERRADA")) resultado.put(clave, 0L);
        var grupos = mongoTemplate.aggregate(org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("estado").count().as("cantidad")),
                "vacantes", org.bson.Document.class);
        for (var grupo : grupos) {
            long cantidad = ((Number) grupo.get("cantidad")).longValue();
            resultado.merge("total", cantidad, Long::sum);
            String estado = grupo.getString("_id");
            if (estado != null && resultado.containsKey(estado)) resultado.put(estado, cantidad);
        }
        return resultado;
    }

    public Vacante guardar(Vacante vacante) {
        return vacanteRepository.save(vacante);
    }

    public List<Vacante> listarTodas() {
        return vacanteRepository.findAll();
    }

    public org.springframework.data.domain.Page<Vacante> paginaPostulante(FiltrosVacantes filtro) {
        var filtros = new ArrayList<Criteria>();
        filtros.add(Criteria.where("estado").is(VacanteEstado.ACTIVA));
        if (tieneTexto(filtro.getOferta())) filtros.add(Criteria.where("_id").is(filtro.getOferta()));
        if (!filtro.texto().isEmpty()) {
            String patron = patronLiteral(filtro.texto());
            filtros.add(new Criteria().orOperator(Criteria.where("titulo").regex(patron, "i"),
                    Criteria.where("empresa.nombre").regex(patron, "i")));
        }
        if (tieneTexto(filtro.getModalidad()))
            filtros.add(Criteria.where("modalidad").regex("^" + patronLiteral(filtro.getModalidad()) + "$", "i"));
        if (tieneTexto(filtro.getTipoContrato()))
            filtros.add(Criteria.where("tipoContrato").regex("^" + patronLiteral(filtro.getTipoContrato()) + "$", "i"));
        if (tieneTexto(filtro.getSector())) {
            var empresas = Query.query(Criteria.where("sector").regex("^" + patronLiteral(filtro.getSector()) + "$", "i"));
            empresas.fields().include("_id");
            var ids = mongoTemplate.find(empresas, org.bson.Document.class, "empresas").stream()
                    .map(e -> e.get("_id")).toList();
            filtros.add(Criteria.where("empresa._id").in(ids));
        }
        var hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        int dias = switch (filtro.getFechaFiltro() == null ? "" : filtro.getFechaFiltro()) {
            case "hoy" -> 1;
            case "3dias" -> 3;
            case "semana" -> 7;
            case "mes" -> 30;
            default -> 0;
        };
        if (dias > 0) filtros.add(Criteria.where("fechaPublicacion").gte(hoy.minusDays(dias - 1)).lte(hoy));
        Double minimo = filtro.getSalarioMin(), maximo = filtro.getSalarioMax();
        if ((minimo != null && (!Double.isFinite(minimo) || minimo < 0))
                || (maximo != null && (!Double.isFinite(maximo) || maximo < 0))
                || (minimo != null && maximo != null && minimo > maximo)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Revisa el rango de salario.");
        }
        if (minimo != null) filtros.add(Criteria.where("salario").gte(minimo));
        if (maximo != null) filtros.add(Criteria.where("salario").lte(maximo));
        if (filtro.isPocosCandidatos()) {
            // El filtro se aplica antes de paginar, sin resolver las referencias de cada postulación.
            var conteos = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                    (org.springframework.data.mongodb.core.aggregation.AggregationOperation) context -> new org.bson.Document("$group",
                            new org.bson.Document("_id", "$vacanteId.$id").append("cantidad", new org.bson.Document("$sum", 1))),
                    org.springframework.data.mongodb.core.aggregation.Aggregation.match(Criteria.where("cantidad").gte(10)));
            var conMuchos = mongoTemplate.aggregate(conteos, "postulaciones", org.bson.Document.class).getMappedResults()
                    .stream().map(c -> c.get("_id")).filter(java.util.Objects::nonNull).toList();
            if (!conMuchos.isEmpty()) filtros.add(Criteria.where("_id").nin(conMuchos));
        }
        var criteria = new Criteria().andOperator(filtros.toArray(Criteria[]::new));
        long total = mongoTemplate.count(Query.query(criteria), Vacante.class);
        int ultima = (int) Math.max(0, (total - 1) / VACANTES_POR_PAGINA_POSTULANTE);
        var pagina = org.springframework.data.domain.PageRequest.of(Math.max(0, Math.min(filtro.getPagina(), ultima)), VACANTES_POR_PAGINA_POSTULANTE,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion", "id"));
        var query = Query.query(criteria).with(pagina);
        query.fields().exclude("postulaciones");
        var vacantes = total == 0 ? List.<Vacante>of() : mongoTemplate.find(query, Vacante.class);
        return new org.springframework.data.domain.PageImpl<>(vacantes, pagina, total);
    }

    public List<String> sectoresDisponibles() {
        var empresas = mongoTemplate.findDistinct(Query.query(Criteria.where("estado").is(VacanteEstado.ACTIVA)),
                "empresa._id", "vacantes", org.bson.types.ObjectId.class);
        if (empresas.isEmpty()) return List.of();
        return mongoTemplate.findDistinct(Query.query(Criteria.where("_id").in(empresas)), "sector", "empresas", String.class)
                .stream().filter(VacanteService::tieneTexto).map(String::trim).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String patronLiteral(String texto) {
        String normal = java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        normal = normal.substring(0, Math.min(normal.length(), 100));
        var patron = new StringBuilder();
        for (char letra : normal.toCharArray()) patron.append(switch (letra) {
            case 'a' -> "[aáàäâ]";
            case 'e' -> "[eéèëê]";
            case 'i' -> "[iíìïî]";
            case 'o' -> "[oóòöô]";
            case 'u' -> "[uúùüû]";
            case 'n' -> "[nñ]";
            default -> java.util.regex.Pattern.quote(String.valueOf(letra));
        });
        return patron.toString();
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
