package com.project.HireConnect.service;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.repository.EmpresaRepository;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FotoEmpresaService {
    private final EmpresaRepository repository;
    private final Path directory;

    @org.springframework.beans.factory.annotation.Autowired
    public FotoEmpresaService(EmpresaRepository repository) {
        this(repository, Paths.get("uploads/empresas"));
    }

    FotoEmpresaService(EmpresaRepository repository, Path directory) {
        this.repository = repository;
        this.directory = directory.toAbsolutePath().normalize();
    }

    private Empresa actual(String email) {
        return repository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró tu perfil."));
    }

    public String guardar(String email, MultipartFile file) throws IOException {
        Empresa user = actual(email);
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen JPG o PNG de hasta 2 MB.");
        BufferedImage image;
        try (var stream = file.getInputStream(); var input = ImageIO.createImageInputStream(stream)) {
            if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen válida.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se admiten imágenes JPG o PNG.");
                // Lee las dimensiones antes de decodificar para limitar el consumo de memoria.
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > 24_000_000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe tener como máximo 24 megapíxeles.");
                image = reader.read(0);
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen. Prueba con otro archivo.");
        }
        BufferedImage output = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        var graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 512, 512);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int side = Math.min(image.getWidth(), image.getHeight());
            int x = (image.getWidth() - side) / 2, y = (image.getHeight() - side) / 2;
            graphics.drawImage(image, 0, 0, 512, 512, x, y, x + side, y + side, null);
        } finally { graphics.dispose(); }
        Files.createDirectories(directory);
        String filename = "empresa_" + UUID.randomUUID() + ".jpg";
        Path destination = directory.resolve(filename);
        ImageIO.write(output, "jpg", destination.toFile());
        String previous = user.getFoto();
        user.setFoto("/uploads/empresas/" + filename);
        try { repository.save(user); }
        catch (RuntimeException e) { user.setFoto(previous); Files.deleteIfExists(destination); throw e; }
        borrarArchivoAnterior(previous);
        return user.getFoto();
    }

    public void eliminar(String email) {
        Empresa user = actual(email);
        String previous = user.getFoto();
        user.setFoto(null);
        repository.save(user);
        borrarArchivoAnterior(previous);
    }

    private void borrarArchivoAnterior(String url) {
        if (url == null || !url.startsWith("/uploads/empresas/empresa_")) return;
        Path file = directory.resolve(url.substring("/uploads/empresas/".length())).normalize();
        if (!directory.equals(file.getParent())) return;
        try { Files.deleteIfExists(file); }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo retirar la foto anterior del almacenamiento.");
        }
    }
}
