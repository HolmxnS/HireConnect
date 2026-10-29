package com.project.HireConnect.controller;

import com.project.HireConnect.service.FotoEmpresaService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/empresa/foto")
public class FotoEmpresaController {
    private final FotoEmpresaService service;
    public FotoEmpresaController(FotoEmpresaService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("foto", service.guardar(authentication.getName(), file), "mensaje", "Foto actualizada."));
    }

    @DeleteMapping
    public ResponseEntity<?> eliminar(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        service.eliminar(authentication.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Foto eliminada."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> solicitudInvalida(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("mensaje", error.getReason() == null ? "No se pudo actualizar la foto." : error.getReason()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> falloAlmacenamiento(IOException error) {
        return ResponseEntity.internalServerError().body(Map.of("mensaje", "No pudimos guardar la foto. Inténtalo de nuevo."));
    }
}
