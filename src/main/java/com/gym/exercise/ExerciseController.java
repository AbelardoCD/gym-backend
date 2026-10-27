package com.gym.exercise;

import com.gym.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de ejercicios. Lo administra quien arma rutinas. */
@RestController
@RequestMapping("/api/exercises")
@PreAuthorize("hasAuthority('RUTINAS')")
@RequiredArgsConstructor
@Transactional
public class ExerciseController {
    private final ExerciseRepository exercises;

    public record ExerciseRequest(@NotBlank @Size(max = 120) String name, @NotNull MuscleGroup muscleGroup, boolean active) {}

    public record ExerciseResponse(Long id, String name, MuscleGroup muscleGroup, boolean active, long uses) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<ExerciseResponse> list() {
        return exercises.findAllByOrderByMuscleGroupAscNameAsc().stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ExerciseResponse get(@PathVariable Long id) {
        return response(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse create(@Valid @RequestBody ExerciseRequest req) {
        if (exercises.existsByNameIgnoreCase(req.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un ejercicio con ese nombre");
        }
        Exercise e = new Exercise();
        apply(e, req);
        return response(exercises.save(e));
    }

    @PutMapping("/{id}")
    public ExerciseResponse update(@PathVariable Long id, @Valid @RequestBody ExerciseRequest req) {
        Exercise e = find(id);
        if (exercises.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un ejercicio con ese nombre");
        }
        apply(e, req);
        return response(e);
    }

    /** Solo se borra si ninguna rutina lo usa; si no, conviene desactivarlo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Exercise e = find(id);
        long uses = exercises.countUses(id);
        if (uses > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    (uses == 1 ? "Lo usa 1 rutina" : "Lo usan " + uses + " rutinas") + "; desactívalo para que ya no se ofrezca");
        }
        exercises.delete(e);
    }

    private void apply(Exercise e, ExerciseRequest req) {
        e.setName(req.name().trim());
        e.setMuscleGroup(req.muscleGroup());
        e.setActive(req.active());
    }

    private ExerciseResponse response(Exercise e) {
        return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroup(), e.isActive(), exercises.countUses(e.getId()));
    }

    private Exercise find(Long id) {
        return exercises.findById(id).orElseThrow(() -> ApiException.notFound("Ejercicio no encontrado"));
    }
}
