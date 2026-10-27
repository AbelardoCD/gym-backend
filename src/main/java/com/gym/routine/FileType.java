package com.gym.routine;

import java.util.Locale;
import java.util.Optional;

public enum FileType {
    VIDEO, PDF, EXCEL, IMAGEN;

    /** Clasifica por extensión; solo se aceptan videos, PDF, Excel y fotos. */
    public static Optional<FileType> fromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return Optional.empty();
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "mp4", "mov", "webm", "m4v", "avi", "mkv" -> Optional.of(VIDEO);
            case "pdf" -> Optional.of(PDF);
            case "xls", "xlsx", "csv" -> Optional.of(EXCEL);
            case "jpg", "jpeg", "png", "webp", "heic", "gif" -> Optional.of(IMAGEN);
            default -> Optional.empty();
        };
    }
}
