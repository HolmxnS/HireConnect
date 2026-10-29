package com.project.HireConnect.service;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.repository.EmpresaRepository;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FotoEmpresaService {
    private final EmpresaRepository repository;
    private final ImagenesService imagenes;

    public FotoEmpresaService(EmpresaRepository repository, ImagenesService imagenes) {
        this.repository = repository;
        this.imagenes = imagenes;
    }

    private Empresa actual(String email) {
        return repository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró tu perfil."));
    }

    public String guardar(String email, MultipartFile file) throws IOException {
        return guardarImagen(email, file, false);
    }

    public String guardarPortada(String email, MultipartFile file) throws IOException {
        return guardarImagen(email, file, true);
    }

    private String guardarImagen(String email, MultipartFile file, boolean portada) throws IOException {
        Empresa user = actual(email);
        int maxMB = portada ? 5 : 2;
        if (file.isEmpty() || file.getSize() > maxMB * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen JPG o PNG de hasta " + maxMB + " MB.");
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
        int width = portada ? 1600 : 512, height = portada ? 400 : 512;
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double ratio = (double) width / height;
            int cropWidth = Math.min(image.getWidth(), (int) (image.getHeight() * ratio));
            int cropHeight = Math.max(1, Math.min(image.getHeight(), (int) (cropWidth / ratio)));
            int x = (image.getWidth() - cropWidth) / 2, y = (image.getHeight() - cropHeight) / 2;
            graphics.drawImage(image, 0, 0, width, height, x, y, x + cropWidth, y + cropHeight, null);
        } finally { graphics.dispose(); }
        String filename = (portada ? "portada_empresa_" : "empresa_") + UUID.randomUUID() + ".jpg";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(output, "jpg", bytes);
        String previous = portada ? user.getPortada() : user.getFoto();
        String url = "/uploads/empresas/" + filename;
        imagenes.guardar(url, bytes.toByteArray(), "image/jpeg");
        if (portada) user.setPortada(url); else user.setFoto(url);
        try { repository.save(user); }
        catch (RuntimeException e) {
            if (portada) user.setPortada(previous); else user.setFoto(previous);
            imagenes.eliminar(url); throw e;
        }
        borrarArchivoAnterior(previous);
        return url;
    }

    public void eliminar(String email) { eliminarImagen(email, false); }
    public void eliminarPortada(String email) { eliminarImagen(email, true); }

    private void eliminarImagen(String email, boolean portada) {
        Empresa user = actual(email);
        String previous = portada ? user.getPortada() : user.getFoto();
        if (portada) user.setPortada(null); else user.setFoto(null);
        try { repository.save(user); }
        catch (RuntimeException e) {
            if (portada) user.setPortada(previous); else user.setFoto(previous);
            throw e;
        }
        borrarArchivoAnterior(previous);
    }

    private void borrarArchivoAnterior(String url) {
        if (url == null || !(url.startsWith("/uploads/empresas/empresa_") || url.startsWith("/uploads/empresas/portada_empresa_"))) return;
        try { imagenes.eliminar(url); }
        catch (IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo retirar la foto anterior del almacenamiento.");
        }
    }
}
