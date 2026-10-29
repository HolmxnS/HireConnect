package com.project.HireConnect.service;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

@Service
public class ImagenesService implements ApplicationRunner {
    private final GridFsOperations grid;
    private final Path legacyRoot;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImagenesService.class);

    @Autowired
    public ImagenesService(MongoDatabaseFactory factory, MongoConverter converter,
            @Value("${hireconnect.uploads.legacy-directory:uploads}") String legacyDirectory) {
        this(new GridFsTemplate(factory, converter, "imagenes"), Paths.get(legacyDirectory));
    }

    ImagenesService(GridFsOperations grid, Path legacyRoot) {
        this.grid = grid; this.legacyRoot = legacyRoot.toAbsolutePath().normalize();
    }

    public record Imagen(byte[] contenido, String tipo) { }

    private Query query(String url) { return Query.query(Criteria.where("filename").is(url)); }
    private Path local(String url) {
        if (url == null || !url.matches("/uploads/(empresas|fotos|admins)/[A-Za-z0-9_.-]+\\.(jpg|jpeg|png)"))
            throw new IllegalArgumentException("Ruta de imagen no válida.");
        Path path = legacyRoot.resolve(url.substring("/uploads/".length())).normalize();
        if (!path.startsWith(legacyRoot)) throw new IllegalArgumentException("Ruta de imagen no válida.");
        return path;
    }

    public void guardar(String url, byte[] contenido, String tipo) {
        local(url);
        if (contenido.length == 0 || contenido.length > 5 * 1024 * 1024
                || !(tipo.equals("image/jpeg") || tipo.equals("image/png"))) throw new IllegalArgumentException("Imagen no válida.");
        grid.store(new ByteArrayInputStream(contenido), url, tipo, new Document("origen", "perfil"));
    }

    public Optional<Imagen> leer(String url) throws IOException {
        Path path = local(url);
        var file = grid.findOne(query(url));
        if (file != null) {
            try (var stream = grid.getResource(file).getInputStream()) {
                String tipo = file.getMetadata() == null ? "image/jpeg" : file.getMetadata().getString("_contentType");
                return Optional.of(new Imagen(stream.readAllBytes(), "image/png".equals(tipo) ? "image/png" : "image/jpeg"));
            }
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 5 * 1024 * 1024) return Optional.empty();
        byte[] bytes = Files.readAllBytes(path);
        String tipo = url.endsWith(".png") ? "image/png" : "image/jpeg";
        // Las URL antiguas se conservan; al recuperarlas se copia su contenido a MongoDB.
        guardar(url, bytes, tipo);
        return Optional.of(new Imagen(bytes, tipo));
    }

    public void eliminar(String url) throws IOException {
        Path path = local(url);
        grid.delete(query(url));
        Files.deleteIfExists(path);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        for (String folder : new String[]{"empresas", "fotos", "admins"}) {
            Path directory = legacyRoot.resolve(folder);
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) continue;
            try (var files = Files.list(directory)) {
                files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                        .filter(p -> p.getFileName().toString().matches("[A-Za-z0-9_.-]+\\.(jpg|jpeg|png)"))
                        .forEach(path -> {
                            try { leer("/uploads/" + folder + "/" + path.getFileName()); }
                            catch (IOException | RuntimeException error) { log.warn("No se pudo migrar una imagen local; se reintentará al consultarla."); }
                        });
            } catch (IOException error) { log.warn("No se pudo revisar una carpeta de imágenes antiguas."); }
        }
    }
}
