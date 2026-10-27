package com.gym.routine;

import com.gym.common.ApiException;
import com.gym.exercise.Exercise;
import com.gym.exercise.ExerciseRepository;
import com.gym.routine.RoutineDtos.DayItem;
import com.gym.routine.RoutineDtos.ExerciseItem;
import com.gym.routine.RoutineDtos.RoutineRequest;
import com.gym.security.AuthUser;
import com.gym.storage.StorageService;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineService {
    private final RoutineRepository routines;
    private final RoutineFileRepository files;
    private final UserRepository users;
    private final StorageService storage;
    private final ExerciseRepository exerciseCatalog;

    @Transactional(readOnly = true)
    public List<Routine> catalog(AuthUser me) {
        return me.isAdmin() ? routines.findAllByOrderByCreatedAtDesc() : routines.findByTrainerIdOrderByCreatedAtDesc(me.id());
    }

    @Transactional(readOnly = true)
    public List<Routine> assignedTo(AuthUser me) {
        return routines.findAssignedTo(me.id());
    }

    @Transactional(readOnly = true)
    public Routine getVisible(Long id, AuthUser me) {
        Routine r = find(id);
        boolean visible = me.isAdmin()
                || r.getTrainer().getId().equals(me.id())
                || r.getMembers().stream().anyMatch(m -> m.getId().equals(me.id()));
        if (!visible) {
            throw ApiException.forbidden("No tienes acceso a esta rutina");
        }
        return r;
    }

    public Routine create(RoutineRequest req, AuthUser me) {
        Routine r = new Routine();
        r.setTrainer(users.getReferenceById(me.id()));
        apply(r, req);
        return routines.save(r);
    }

    public Routine update(Long id, RoutineRequest req, AuthUser me) {
        Routine r = getEditable(id, me);
        apply(r, req);
        return r;
    }

    public void delete(Long id, AuthUser me) {
        Routine r = getEditable(id, me);
        r.getFiles().forEach(f -> storage.delete(f.getStorageKey()));
        routines.delete(r);
    }

    public RoutineFile addFile(Long routineId, MultipartFile upload, AuthUser me) {
        Routine r = getEditable(routineId, me);
        if (upload.isEmpty()) {
            throw ApiException.badRequest("El archivo está vacío");
        }
        FileType type = FileType.fromFilename(upload.getOriginalFilename())
                .orElseThrow(() -> ApiException.badRequest("Solo se permiten videos, PDF, Excel o fotos"));
        RoutineFile f = new RoutineFile();
        f.setRoutine(r);
        f.setFileType(type);
        f.setOriginalName(upload.getOriginalFilename());
        f.setContentType(upload.getContentType() == null ? "application/octet-stream" : upload.getContentType());
        f.setSizeBytes(upload.getSize());
        f.setStorageKey(storage.store(upload, "routines/" + r.getId()));
        r.getFiles().add(f);
        return files.save(f);
    }

    public void deleteFile(Long routineId, Long fileId, AuthUser me) {
        Routine r = getEditable(routineId, me);
        RoutineFile f = findFile(r, fileId);
        storage.delete(f.getStorageKey());
        r.getFiles().remove(f);
    }

    @Transactional(readOnly = true)
    public RoutineFile getVisibleFile(Long routineId, Long fileId, AuthUser me) {
        return findFile(getVisible(routineId, me), fileId);
    }

    private RoutineFile findFile(Routine r, Long fileId) {
        return r.getFiles().stream().filter(f -> f.getId().equals(fileId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Archivo no encontrado"));
    }

    private Routine find(Long id) {
        return routines.findById(id).orElseThrow(() -> ApiException.notFound("Rutina no encontrada"));
    }

    private Routine getEditable(Long id, AuthUser me) {
        Routine r = find(id);
        if (!me.isAdmin() && !r.getTrainer().getId().equals(me.id())) {
            throw ApiException.forbidden("Solo el entrenador de la rutina puede modificarla");
        }
        return r;
    }

    private void apply(Routine r, RoutineRequest req) {
        r.setName(req.name());
        r.setDescription(blankToNull(req.description()));
        r.setDiscipline(req.discipline());
        Set<User> members = new HashSet<>();
        if (req.memberIds() != null) {
            for (User u : users.findAllById(req.memberIds())) {
                if (!u.isActive()) {
                    throw ApiException.badRequest("No se pueden asignar rutinas a usuarios inactivos");
                }
                // Solo se valida a los nuevos, para no romper asignaciones hechas antes de los subroles.
                boolean alreadyAssigned = r.getMembers().stream().anyMatch(m -> m.getId().equals(u.getId()));
                if (!alreadyAssigned && !u.getDisciplines().contains(req.discipline())) {
                    throw ApiException.badRequest(u.getFullName() + " no es socio de " + req.discipline().label());
                }
                members.add(u);
            }
        }
        r.getMembers().clear();
        r.getMembers().addAll(members);
        applyDays(r, req.days() == null ? List.of() : req.days());
    }

    /**
     * Reemplaza los días de la rutina: su descripción y sus ejercicios en el orden recibido.
     * Un ejercicio inactivo solo se acepta si ya estaba en la rutina.
     */
    private void applyDays(Routine r, List<DayItem> days) {
        Set<Integer> seen = new HashSet<>();
        for (DayItem d : days) {
            if (!seen.add(d.dayOfWeek())) {
                throw ApiException.badRequest("Un día aparece dos veces en la rutina");
            }
        }
        Set<Long> current = new HashSet<>();
        r.getExercises().forEach(re -> current.add(re.getExercise().getId()));
        Map<Long, Exercise> byId = new HashMap<>();
        exerciseCatalog.findAllById(days.stream()
                .flatMap(d -> d.exercises() == null ? java.util.stream.Stream.empty() : d.exercises().stream())
                .map(ExerciseItem::exerciseId).toList()).forEach(e -> byId.put(e.getId(), e));
        r.getExercises().clear();
        r.getDayDescriptions().clear();
        for (DayItem day : days) {
            String description = blankToNull(day.description());
            if (description != null) {
                r.getDayDescriptions().put(day.dayOfWeek(), description);
            }
            int position = 0;
            for (ExerciseItem item : day.exercises() == null ? List.<ExerciseItem>of() : day.exercises()) {
                Exercise e = byId.get(item.exerciseId());
                if (e == null) {
                    throw ApiException.badRequest("Ejercicio no encontrado");
                }
                if (!e.isActive() && !current.contains(e.getId())) {
                    throw ApiException.badRequest(e.getName() + " está desactivado en el catálogo");
                }
                RoutineExercise re = new RoutineExercise();
                re.setRoutine(r);
                re.setExercise(e);
                re.setDayOfWeek(day.dayOfWeek());
                re.setPosition(position++);
                re.setSets(item.sets());
                re.setReps(blankToNull(item.reps()));
                re.setNotes(blankToNull(item.notes()));
                r.getExercises().add(re);
            }
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
