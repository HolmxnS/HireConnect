package com.project.HireConnect.service;

import com.mongodb.DBRef;
import com.project.HireConnect.enums.PostulacionEstado;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.repository.PostulanteRepository;
import org.bson.Document;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AdminPostulantesService {
    private final MongoTemplate mongo;
    private final PostulanteRepository repository;

    public AdminPostulantesService(MongoTemplate mongo, PostulanteRepository repository) {
        this.mongo = mongo;
        this.repository = repository;
    }

    public Resultado pagina(String texto, String estado, String cv, String orden, int numero) {
        Map<String, String> ultimos = ultimosEstados();
        List<Criteria> filtros = new ArrayList<>();
        String patron = Pattern.quote(texto.trim());
        Document nombreCompleto = new Document("$concat", List.of(
                new Document("$ifNull", List.of("$nombre", "")), " ", new Document("$ifNull", List.of("$apellido", ""))));
        filtros.add(new Criteria().orOperator(Criteria.where("nombre").regex(patron, "i"),
                Criteria.where("apellido").regex(patron, "i"), Criteria.where("email").regex(patron, "i"),
                Criteria.where("$expr").is(new Document("$regexMatch", new Document("input", nombreCompleto).append("regex", patron).append("options", "i")))));
        if ("complete".equals(cv)) filtros.add(conCv());
        if ("incomplete".equals(cv)) filtros.add(new Criteria().orOperator(Criteria.where("cv").exists(false), Criteria.where("cv").is(null), Criteria.where("cv").is("")));
        if ("Sin Postulaciones".equals(estado)) {
            filtros.add(Criteria.where("id").nin(ultimos.entrySet().stream().filter(e -> !e.getValue().equals("Sin Postulaciones")).map(Map.Entry::getKey).toList()));
        } else if (!"all".equals(estado)) {
            filtros.add(Criteria.where("id").in(ultimos.entrySet().stream().filter(e -> e.getValue().equals(estado)).map(Map.Entry::getKey).toList()));
        }
        Criteria criteria = new Criteria().andOperator(filtros.toArray(Criteria[]::new));
        long total = mongo.count(Query.query(criteria), Postulante.class);
        int paginas = (int) Math.max(1, (total + 19) / 20);
        int actual = Math.max(0, Math.min(numero, paginas - 1));
        Sort.Direction direccion = "oldest".equals(orden) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(actual, 20, Sort.by(direccion, "fechaRegistro").and(Sort.by(direccion, "id")));
        Query query = Query.query(criteria).with(pageable);
        campos(query);
        List<Postulante> perfiles = total == 0 ? List.of() : mongo.find(query, Postulante.class);
        Map<String, String> estados = new HashMap<>();
        perfiles.forEach(p -> estados.put(p.getId(), ultimos.getOrDefault(p.getId(), "Sin Postulaciones")));
        return new Resultado(new PageImpl<>(perfiles, pageable, total), estados);
    }

    // Agrupa en MongoDB para evitar una consulta de última postulación por cada perfil.
    private Map<String, String> ultimosEstados() {
        var pipeline = Aggregation.newAggregation(
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "fechaPostulacion", "_id")),
                Aggregation.group("postulanteId").first("estado").as("estado"));
        Map<String, String> estados = new HashMap<>();
        for (Document row : mongo.aggregate(pipeline, "postulaciones", Document.class)) {
            Object ref = row.get("_id");
            Object id = ref instanceof DBRef r ? r.getId() : ref instanceof Document d ? d.get("$id") : null;
            if (id == null) continue;
            String estado = "Sin Postulaciones";
            if (row.get("estado") != null) {
                try { estado = PostulacionEstado.fromString(row.get("estado").toString()).getDescripcion(); }
                catch (IllegalArgumentException ignored) { }
            }
            estados.put(id.toString(), estado);
        }
        return estados;
    }

    public Resumen resumen(LocalDate hoy) {
        long total = repository.count();
        long completos = mongo.count(Query.query(conCv()), Postulante.class);
        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fechaRegistro").gte(hoy.withDayOfYear(1)).lt(hoy.withDayOfYear(1).plusYears(1))),
                Aggregation.project().and(DateOperators.Month.monthOf("fechaRegistro")).as("mes"),
                Aggregation.group("mes").count().as("cantidad"));
        List<Long> registros = new ArrayList<>(Collections.nCopies(12, 0L));
        for (Document row : mongo.aggregate(pipeline, "postulantes", Document.class)) {
            int mes = ((Number) row.get("_id")).intValue();
            if (mes >= 1 && mes <= 12) registros.set(mes - 1, ((Number) row.get("cantidad")).longValue());
        }
        Query recientes = new Query().with(Sort.by(Sort.Direction.DESC, "fechaRegistro", "id")).limit(5);
        campos(recientes);
        List<Persona> personas = mongo.find(recientes, Postulante.class).stream()
                .map(p -> new Persona(p.getId(), p.getNombre(), p.getApellido(), p.getFoto(), p.getFechaRegistro() == null ? "" : p.getFechaRegistro().toString())).toList();
        return new Resumen(total, completos, registros, personas);
    }

    private Criteria conCv() { return Criteria.where("cv").exists(true).nin(Arrays.asList(null, "")); }
    private void campos(Query query) { query.fields().include("nombre", "apellido", "email", "foto", "cv", "fechaRegistro"); }
    public record Resultado(Page<Postulante> pagina, Map<String, String> estados) {}
    public record Persona(String id, String nombre, String apellido, String foto, String fecha) {}
    public record Resumen(long total, long conCv, List<Long> registros, List<Persona> recientes) {}
}
