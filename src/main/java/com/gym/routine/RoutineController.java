package com.gym.routine;

import com.gym.routine.RoutineDtos.FileResponse;
import com.gym.routine.RoutineDtos.RoutineRequest;
import com.gym.routine.RoutineDtos.RoutineResponse;
import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import com.gym.storage.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineController {
    private final RoutineService routineService;
    private final StorageService storage;

    /** Catálogo: el administrador ve todas, los demás las que crearon. */
    @GetMapping
    @PreAuthorize("hasAuthority('RUTINAS')")
    public List<RoutineResponse> list(@CurrentUser AuthUser me) {
        return routineService.catalog(me).stream().map(RoutineResponse::from).toList();
    }

    /** Rutinas asignadas al usuario conectado (pantalla de inicio). */
    @GetMapping("/mine")
    public List<RoutineResponse> mine(@CurrentUser AuthUser me) {
        return routineService.assignedTo(me).stream().map(RoutineResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoutineResponse get(@PathVariable Long id, @CurrentUser AuthUser me) {
        return RoutineResponse.from(routineService.getVisible(id, me));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RUTINAS')")
    @Transactional
    public RoutineResponse create(@Valid @RequestBody RoutineRequest req, @CurrentUser AuthUser me) {
        return RoutineResponse.from(routineService.create(req, me));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RUTINAS')")
    @Transactional
    public RoutineResponse update(@PathVariable Long id, @Valid @RequestBody RoutineRequest req, @CurrentUser AuthUser me) {
        return RoutineResponse.from(routineService.update(id, req, me));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('RUTINAS')")
    @Transactional
    public void delete(@PathVariable Long id, @CurrentUser AuthUser me) {
        routineService.delete(id, me);
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RUTINAS')")
    @Transactional
    public FileResponse upload(@PathVariable Long id, @RequestParam("file") MultipartFile file, @CurrentUser AuthUser me) {
        return FileResponse.from(routineService.addFile(id, file, me));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('RUTINAS')")
    @Transactional
    public void deleteFile(@PathVariable Long id, @PathVariable Long fileId, @CurrentUser AuthUser me) {
        routineService.deleteFile(id, fileId, me);
    }

    /** Descarga o reproduce el archivo. Spring atiende cabeceras Range, así que los videos se pueden adelantar. */
    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long fileId, @CurrentUser AuthUser me) {
        RoutineFile f = routineService.getVisibleFile(id, fileId, me);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .body(storage.load(f.getStorageKey()));
    }
}
