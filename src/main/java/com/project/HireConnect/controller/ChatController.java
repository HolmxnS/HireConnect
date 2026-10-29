package com.project.HireConnect.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.ChatbotService;
import com.project.HireConnect.service.PostulanteService;

@RestController
@RequestMapping("/postulante/chat")
public class ChatController {

    private static final int MAX_CARACTERES = 1000;

    private final ChatbotService chatbotService;
    private final PostulanteService postulanteService;

    public ChatController(ChatbotService chatbotService, PostulanteService postulanteService) {
        this.chatbotService = chatbotService;
        this.postulanteService = postulanteService;
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> procesarMensaje(@RequestBody Map<String, Object> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("respuesta", "Inicia sesión para usar el asistente."));
        }

        Object rawMensaje = request.get("mensaje");
        String mensaje = rawMensaje == null ? "" : rawMensaje.toString().trim();
        if (mensaje.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("respuesta", "Escribe un mensaje."));
        }
        if (mensaje.length() > MAX_CARACTERES) {
            mensaje = mensaje.substring(0, MAX_CARACTERES);
        }

        Optional<Postulante> postulante = postulanteService.buscarPorEmail(authentication.getName());
        if (postulante.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("respuesta", "No encontré tu perfil de postulante."));
        }

        List<Map<String, String>> historial = request.get("historial") instanceof List<?> h
                ? (List<Map<String, String>>) h
                : List.of();

        String respuesta = chatbotService.responder(postulante.get(), mensaje, historial);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }
}