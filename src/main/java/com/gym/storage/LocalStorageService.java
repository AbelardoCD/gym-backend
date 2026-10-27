package com.gym.storage;

import com.gym.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {
    private final Path root;

    public LocalStorageService(@Value("${app.storage.path}") String path) throws IOException {
        this.root = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + (ext == null ? "" : "." + ext.toLowerCase());
        Path target = resolve(key);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo");
        }
        return key;
    }

    @Override
    public Resource load(String key) {
        try {
            Resource r = new UrlResource(resolve(key).toUri());
            if (!r.exists()) {
                throw ApiException.notFound("Archivo no encontrado");
            }
            return r;
        } catch (MalformedURLException e) {
            throw ApiException.notFound("Archivo no encontrado");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // si el archivo ya no existe no hay nada que limpiar
        }
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw ApiException.badRequest("Ruta inválida");
        }
        return p;
    }
}
