package com.project.HireConnect.service;

import com.project.HireConnect.model.*;
import com.project.HireConnect.dto.FormulariosOportunidades.*;
import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.repository.NotificacionPostulanteRepository;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OportunidadesService {
    private final MongoTemplate mongo;
    private final PostulacionService postulaciones;
    private final NotificacionPostulanteRepository notificaciones;
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final List<String> CATEGORIAS = List.of("Diseño", "Tecnología", "Marketing", "Redacción", "Audiovisual", "Administración", "Otros");
    public OportunidadesService(MongoTemplate mongo, PostulacionService postulaciones, NotificacionPostulanteRepository notificaciones) {
        this.mongo = mongo; this.postulaciones = postulaciones; this.notificaciones = notificaciones;
    }
    private static void exigir(boolean condicion, HttpStatus estado, String mensaje) {
        if (!condicion) throw new ResponseStatusException(estado, mensaje);
    }
    private static void validarId(String id) {
        exigir(id != null && org.bson.types.ObjectId.isValid(id), HttpStatus.NOT_FOUND, "No encontrado");
    }
    public Postulacion candidatura(String id, String empresaId) {
        validarId(id);
        var p = postulaciones.buscarPorId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        exigir(p.getVacante() != null && p.getVacante().getEmpresa() != null && empresaId.equals(p.getVacante().getEmpresa().getId()), HttpStatus.FORBIDDEN, "Esta candidatura no pertenece a tu empresa");
        exigir(p.getPostulante() != null, HttpStatus.NOT_FOUND, "Candidato no disponible");
        return p;
    }
    public Entrevista entrevista(String id, String actorId, boolean empresa) {
        validarId(id);
        var e = mongo.findById(id, Entrevista.class);
        exigir(e != null, HttpStatus.NOT_FOUND, "Entrevista no encontrada");
        exigir(actorId.equals(empresa ? e.getEmpresaId() : e.getPostulanteId()), HttpStatus.FORBIDDEN, "No tienes acceso a esta entrevista");
        return e;
    }
    public Page<Entrevista> entrevistas(String actorId, boolean empresa, int pagina) {
        var q = Query.query(Criteria.where(empresa ? "empresaId" : "postulanteId").is(actorId));
        long total = mongo.count(q, Entrevista.class);
        var page = PageRequest.of((int) Math.min(Math.max(0, pagina), Math.max(0, (total - 1) / 20)), 20, Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by("id")));
        return new PageImpl<>(mongo.find(q.with(page), Entrevista.class), page, total);
    }
    public void programar(Empresa empresa, Cita form) {
        var p = candidatura(form.postulacionId(), empresa.getId());
        exigir(empresa.getEstado() == EmpresaEstado.APROBADA, HttpStatus.FORBIDDEN, "La empresa no está activa");
        Instant fecha = form.fecha().atZone(ZONA).toInstant();
        exigir(fecha.isAfter(Instant.now()), HttpStatus.BAD_REQUEST, "Elige una fecha futura");
        exigir(form.duracion() >= 15 && form.duracion() <= 240 && form.lugar() != null && !form.lugar().isBlank(), HttpStatus.BAD_REQUEST, "Revisa los datos de la entrevista");
        String lugar = form.virtual() ? url(form.lugar(), true) : form.lugar().trim();
        var e = mongo.findById(p.getId(), Entrevista.class);
        exigir(e == null ? form.version() == -1 : Objects.equals(e.getVersion(), form.version()), HttpStatus.CONFLICT, "La entrevista cambió. Recarga la página");
        if (e == null) { e = new Entrevista(); e.setId(p.getId()); }
        e.setEmpresaId(empresa.getId()); e.setEmpresaNombre(empresa.getNombreLegal());
        e.setPostulanteId(p.getPostulante().getId()); e.setPostulanteNombre(p.getPostulante().getNombre() + " " + p.getPostulante().getApellido());
        e.setVacanteTitulo(p.getVacante().getTitulo()); e.setFecha(fecha); e.setDuracion(form.duracion());
        e.setVirtual(form.virtual()); e.setLugar(lugar); e.setIndicaciones(form.indicaciones());
        e.setRespuesta(null); e.setEstado(Entrevista.Estado.POR_CONFIRMAR);
        mongo.save(e);
        avisar(e, "Tienes una invitación a entrevista para el " + e.getFechaLocal().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " (hora de Colombia). Abre tu postulación y entra en Entrevistas para responder.");
    }
    public void responder(String id, String postulanteId, long version, String accion, String mensaje) {
        var e = entrevista(id, postulanteId, false);
        exigir(Objects.equals(e.getVersion(), version), HttpStatus.CONFLICT, "La invitación cambió. Revisa el nuevo horario");
        exigir(e.isFutura() && e.getEstado() == Entrevista.Estado.POR_CONFIRMAR, HttpStatus.CONFLICT, "Esta invitación ya no admite respuestas");
        var estado = switch (accion) {
            case "confirmar" -> Entrevista.Estado.CONFIRMADA;
            case "reprogramar" -> Entrevista.Estado.REPROGRAMACION_SOLICITADA;
            case "rechazar" -> Entrevista.Estado.NO_ASISTIRA;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Respuesta no válida");
        };
        exigir(mensaje == null || mensaje.length() <= 1000, HttpStatus.BAD_REQUEST, "El mensaje es demasiado largo");
        exigir(estado != Entrevista.Estado.REPROGRAMACION_SOLICITADA || (mensaje != null && !mensaje.isBlank()), HttpStatus.BAD_REQUEST, "Indica tu disponibilidad para otro horario");
        e.setEstado(estado); e.setRespuesta(mensaje == null ? null : mensaje.trim()); mongo.save(e);
    }
    public void cancelarEntrevista(String id, String empresaId, long version) {
        var e = entrevista(id, empresaId, true);
        exigir(Objects.equals(e.getVersion(), version) && e.isFutura() && e.getEstado() != Entrevista.Estado.CANCELADA, HttpStatus.CONFLICT, "La entrevista cambió o ya finalizó");
        e.setEstado(Entrevista.Estado.CANCELADA); mongo.save(e); avisar(e, "La empresa canceló tu entrevista. Consulta los detalles en Entrevistas.");
    }
    private void avisar(Entrevista e, String mensaje) {
        var n = new NotificacionPostulante(); n.setPostulanteId(e.getPostulanteId()); n.setPostulacionId(e.getId());
        n.setVacanteTitulo(e.getVacanteTitulo()); n.setEstado("ENTREVISTA_PENDIENTE"); n.setMensaje(mensaje); notificaciones.save(n);
    }
    public static String url(String valor, boolean obligatorio) {
        if (valor == null || valor.isBlank()) {
            exigir(!obligatorio, HttpStatus.BAD_REQUEST, "Añade un enlace válido"); return null;
        }
        try {
            var uri = java.net.URI.create(valor.trim());
            exigir(("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null && uri.getUserInfo() == null, HttpStatus.BAD_REQUEST, "Usa un enlace http o https válido");
            return uri.toString();
        } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enlace no válido"); }
    }
    public Page<ProyectoFreelance> proyectos(String actorId, boolean empresa, boolean propios, String texto, String categoria, int pagina) {
        var c = new ArrayList<Criteria>();
        if (empresa) c.add(Criteria.where("empresaId").is(actorId));
        else if (propios) c.add(Criteria.where("propuestas.postulanteId").is(actorId));
        else c.add(Criteria.where("estado").is(ProyectoFreelance.Estado.ABIERTO));
        if (texto != null && !texto.isBlank()) c.add(Criteria.where("titulo").regex(Pattern.quote(texto.substring(0, Math.min(texto.length(), 100))), "i"));
        if (categoria != null && !categoria.isBlank()) c.add(Criteria.where("categoria").is(categoria));
        var q = Query.query(new Criteria().andOperator(c.toArray(Criteria[]::new)));
        long total = mongo.count(q, ProyectoFreelance.class);
        // La lista no necesita descargar los mensajes de todas las propuestas.
        q.fields().exclude("propuestas");
        var page = PageRequest.of((int) Math.min(Math.max(0, pagina), Math.max(0, (total - 1) / 20)), 20, Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by("id")));
        return new PageImpl<>(mongo.find(q.with(page), ProyectoFreelance.class), page, total);
    }
    public ProyectoFreelance proyecto(String id, String actorId, boolean empresa) {
        validarId(id); var p = mongo.findById(id, ProyectoFreelance.class);
        exigir(p != null, HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        if (empresa) exigir(actorId.equals(p.getEmpresaId()), HttpStatus.FORBIDDEN, "Este proyecto pertenece a otra empresa");
        return p;
    }
    public void publicar(Empresa empresa, Proyecto f) {
        exigir(empresa.getEstado() == EmpresaEstado.APROBADA, HttpStatus.FORBIDDEN, "La empresa no está activa");
        exigir(CATEGORIAS.contains(f.categoria()) && List.of("Remoto", "Presencial", "Híbrido").contains(f.modalidad()), HttpStatus.BAD_REQUEST, "Categoría o modalidad no válida");
        var p = new ProyectoFreelance(); p.setEmpresaId(empresa.getId()); p.setEmpresaNombre(empresa.getNombreLegal());
        p.setTitulo(f.titulo().trim()); p.setDescripcion(f.descripcion().trim()); p.setCategoria(f.categoria());
        p.setPresupuesto(f.presupuesto()); p.setDias(f.dias()); p.setModalidad(f.modalidad()); mongo.insert(p);
    }
    public void proponer(String id, Postulante actor, Propuesta f) {
        var p = proyecto(id, actor.getId(), false);
        exigir(p.getEstado() == ProyectoFreelance.Estado.ABIERTO, HttpStatus.CONFLICT, "El proyecto ya no recibe propuestas");
        exigir(p.getPropuestas().stream().noneMatch(x -> actor.getId().equals(x.getPostulanteId())), HttpStatus.CONFLICT, "Ya enviaste una propuesta");
        exigir(p.getPropuestas().size() < 200, HttpStatus.CONFLICT, "Este proyecto alcanzó su límite de propuestas");
        var x = new PropuestaFreelance(); x.setId(new org.bson.types.ObjectId().toHexString()); x.setPostulanteId(actor.getId());
        x.setNombre(actor.getNombre() + " " + actor.getApellido()); x.setMensaje(f.mensaje().trim());
        x.setPrecio(f.precio()); x.setDias(f.dias()); x.setPortafolio(url(f.portafolio(), false)); p.getPropuestas().add(x); mongo.save(p);
    }
    public void seleccionar(String id, String propuestaId, String empresaId, long version) {
        var p = proyecto(id, empresaId, true);
        exigir(Objects.equals(p.getVersion(), version) && p.getEstado() == ProyectoFreelance.Estado.ABIERTO, HttpStatus.CONFLICT, "El proyecto cambió. Recarga la página");
        var elegida = p.getPropuestas().stream().filter(x -> x.getId().equals(propuestaId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        for (var x : p.getPropuestas()) x.setEstado(x == elegida ? PropuestaFreelance.Estado.SELECCIONADA : PropuestaFreelance.Estado.NO_SELECCIONADA);
        p.setEstado(ProyectoFreelance.Estado.EN_CURSO); mongo.save(p);
    }
    public void cerrarProyecto(String id, String empresaId, long version, boolean finalizar) {
        var p = proyecto(id, empresaId, true);
        exigir(Objects.equals(p.getVersion(), version) && (finalizar ? p.getEstado() == ProyectoFreelance.Estado.EN_CURSO : p.getEstado() == ProyectoFreelance.Estado.ABIERTO || p.getEstado() == ProyectoFreelance.Estado.EN_CURSO), HttpStatus.CONFLICT, "Esta acción ya no está disponible");
        p.setEstado(finalizar ? ProyectoFreelance.Estado.FINALIZADO : ProyectoFreelance.Estado.CANCELADO);
        if (!finalizar) p.getPropuestas().forEach(x -> x.setEstado(PropuestaFreelance.Estado.CANCELADA));
        mongo.save(p);
    }
}
