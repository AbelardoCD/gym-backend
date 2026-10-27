package com.gym.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** Abstracción de almacenamiento: hoy disco local, mañana S3 / Cloud Storage sin tocar el resto. */
public interface StorageService {
    /** Guarda el archivo y devuelve la clave con la que se recupera. */
    String store(MultipartFile file, String folder);

    Resource load(String key);

    void delete(String key);
}
