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
@RequestMapping("/empresa/portada")
public class PortadaEmpresaController {
    private final FotoEmpresaService service;
    public PortadaEmpresaController(FotoEmpresaService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("portada", service.guardarPortada(authentication.getName(), file), "mensaje", "Portada actualizada."));
    }

    @DeleteMapping
    public ResponseEntity<?> eliminar(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        service.eliminarPortada(authentication.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Portada eliminada."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> solicitudInvalida(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("mensaje", error.getReason() == null ? "No se pudo actualizar la portada." : error.getReason()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> falloAlmacenamiento(IOException error) {
        return ResponseEntity.internalServerError().body(Map.of("mensaje", "No pudimos guardar la portada. Inténtalo de nuevo."));
    }
}
