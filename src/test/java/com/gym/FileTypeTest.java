package com.gym;

import com.gym.routine.FileType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileTypeTest {
    @Test
    void clasificaLasExtensionesPermitidas() {
        assertThat(FileType.fromFilename("sentadilla.MP4")).contains(FileType.VIDEO);
        assertThat(FileType.fromFilename("plan.pdf")).contains(FileType.PDF);
        assertThat(FileType.fromFilename("cargas.xlsx")).contains(FileType.EXCEL);
        assertThat(FileType.fromFilename("foto.jpeg")).contains(FileType.IMAGEN);
        assertThat(FileType.fromFilename("virus.exe")).isEmpty();
        assertThat(FileType.fromFilename("sin_extension")).isEmpty();
    }
}
