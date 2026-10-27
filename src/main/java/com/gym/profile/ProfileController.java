package com.gym.profile;

import com.gym.profile.ProfileDtos.*;
import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/** Perfil del socio (foto, contacto de emergencia, datos médicos) y su historial de medidas. */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Transactional
public class ProfileController {
    private final ProfileService profiles;

    @GetMapping
    @PreAuthorize("hasAuthority('PROGRESO')")
    public List<ProgressRow> progress() {
        return profiles.progress();
    }

    @GetMapping("/{id}")
    public ProfileResponse get(@PathVariable Long id, @CurrentUser AuthUser me) {
        return ProfileResponse.from(profiles.get(id, me));
    }

    @PutMapping("/{id}")
    public ProfileResponse update(@PathVariable Long id, @Valid @RequestBody ProfileRequest req, @CurrentUser AuthUser me) {
        return ProfileResponse.from(profiles.update(id, req, me));
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file, @CurrentUser AuthUser me) {
        return ProfileResponse.from(profiles.setPhoto(id, file, me));
    }

    @DeleteMapping("/{id}/photo")
    public ProfileResponse deletePhoto(@PathVariable Long id, @CurrentUser AuthUser me) {
        return ProfileResponse.from(profiles.deletePhoto(id, me));
    }

    /** La URL lleva ?v=photoVersion, así que el navegador puede guardarla en caché sin ver fotos viejas. */
    @GetMapping("/{id}/photo")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> photo(@PathVariable Long id) {
        Resource r = profiles.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(r).orElse(MediaType.IMAGE_JPEG))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(r);
    }

    @GetMapping("/{id}/measurements")
    public List<MeasurementResponse> measurements(@PathVariable Long id, @CurrentUser AuthUser me) {
        return profiles.measurements(id, me);
    }

    @PostMapping("/{id}/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse addMeasurement(@PathVariable Long id, @Valid @RequestBody MeasurementRequest req, @CurrentUser AuthUser me) {
        return profiles.addMeasurement(id, req, me);
    }

    @DeleteMapping("/{id}/measurements/{measurementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasurement(@PathVariable Long id, @PathVariable Long measurementId, @CurrentUser AuthUser me) {
        profiles.deleteMeasurement(id, measurementId, me);
    }
}
