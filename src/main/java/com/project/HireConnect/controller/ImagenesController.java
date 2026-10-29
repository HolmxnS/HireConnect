package com.project.HireConnect.controller;

import com.project.HireConnect.service.ImagenesService;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ImagenesController {
    private final ImagenesService imagenes;
    public ImagenesController(ImagenesService imagenes) { this.imagenes = imagenes; }

    @GetMapping("/uploads/{carpeta:empresas|fotos|admins}/{archivo:.+}")
    public ResponseEntity<byte[]> imagen(@PathVariable String carpeta, @PathVariable String archivo) throws IOException {
        try {
            var imagen = imagenes.leer("/uploads/" + carpeta + "/" + archivo);
            if (imagen.isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagen.get().tipo()))
                    .header("X-Content-Type-Options", "nosniff")
                    .cacheControl(CacheControl.noCache()).body(imagen.get().contenido());
        } catch (IllegalArgumentException error) { return ResponseEntity.notFound().build(); }
    }
}
