package com.project.HireConnect.controller;

import com.project.HireConnect.dto.RespuestaChat;
import com.project.HireConnect.service.ChatbotService;
import com.project.HireConnect.service.PostulanteService;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/postulante/chat")
public class ChatController {
    private final ChatbotService chatbot;
    private final PostulanteService postulantes;
    public ChatController(ChatbotService chatbot, PostulanteService postulantes) { this.chatbot=chatbot;this.postulantes=postulantes; }
    @PostMapping
    public ResponseEntity<?> procesarMensaje(@RequestBody Map<String,Object> request, Authentication auth, HttpSession session) {
        if(auth==null||!auth.isAuthenticated()) return error(HttpStatus.UNAUTHORIZED,"Inicia sesión para usar el asistente.");
        if(!(request.get("mensaje") instanceof String raw)||raw.isBlank()) return error(HttpStatus.BAD_REQUEST,"Escribe una pregunta.");
        String mensaje=raw.trim();
        if(mensaje.length()>1000) return error(HttpStatus.BAD_REQUEST,"Tu mensaje puede tener hasta 1.000 caracteres.");
        var p=postulantes.buscarPorEmail(auth.getName());
        if(p.isEmpty()) return error(HttpStatus.FORBIDDEN,"No encontré tu perfil de postulante.");
        synchronized(session) {
            String key="hc-chat-limit-"+p.get().getId();Object saved=session.getAttribute(key);
            var tiempos=new ArrayList<Long>();long now=System.currentTimeMillis();
            if(saved instanceof List<?> l) for(Object t:l) if(t instanceof Long n&&now-n<60000) tiempos.add(n);
            if(tiempos.size()>=12) return ResponseEntity.status(429).header("Retry-After","60").body(Map.of("respuesta","Has enviado varios mensajes seguidos. Espera un minuto y vuelve a intentarlo."));
            tiempos.add(now);session.setAttribute(key,tiempos);
        }
        var historial=new ArrayList<Map<String,String>>();
        if(request.get("historial") instanceof List<?> turnos) for(Object turno:turnos.subList(Math.max(0,turnos.size()-12),turnos.size())) {
            if(!(turno instanceof Map<?,?> t)||!(t.get("rol") instanceof String rol)||!Set.of("user","model").contains(rol)||!(t.get("texto") instanceof String texto)||texto.isBlank()) continue;
            historial.add(Map.of("rol",rol,"texto",texto.substring(0,Math.min(texto.length(),3000))));
        }
        String id=request.get("vacanteId") instanceof String x&&x.matches("[a-fA-F0-9]{24}")?x:null;
        RespuestaChat respuesta=chatbot.conversar(p.get(),mensaje,historial,id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> datosNoDisponibles() {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "No pude consultar tus datos ahora. Inténtalo de nuevo en unos momentos.");
    }
    private ResponseEntity<?> error(HttpStatus status,String mensaje) { return ResponseEntity.status(status).body(Map.of("respuesta",mensaje)); }
}
