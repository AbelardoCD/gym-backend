package com.gym.routine;

import com.gym.user.UserDtos.UserSummary;
import com.gym.exercise.MuscleGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

public final class RoutineDtos {
    private RoutineDtos() {}

    /** La descripción (indicaciones generales) es opcional; cada día trae sus ejercicios en orden. */
    public record RoutineRequest(@NotBlank String name, String description, @NotNull Discipline discipline, Set<Long> memberIds,
                                 @Valid List<DayItem> days) {}

    /** Un día de la semana (1 = lunes ... 7 = domingo) con su descripción opcional y sus ejercicios. */
    public record DayItem(@NotNull @Min(1) @Max(7) Integer dayOfWeek, @Size(max = 4000) String description,
                          @Valid List<ExerciseItem> exercises) {}

    public record ExerciseItem(@NotNull Long exerciseId, @Min(1) @Max(50) Integer sets, @Size(max = 30) String reps,
                               @Size(max = 255) String notes) {}

    public record ExerciseItemResponse(Long exerciseId, String name, MuscleGroup muscleGroup, Integer sets, String reps, String notes) {
        static ExerciseItemResponse from(RoutineExercise re) {
            return new ExerciseItemResponse(re.getExercise().getId(), re.getExercise().getName(), re.getExercise().getMuscleGroup(),
                    re.getSets(), re.getReps(), re.getNotes());
        }
    }

    /** Solo se devuelven los días que tienen ejercicios o descripción, de lunes a domingo. */
    public record DayResponse(int dayOfWeek, String description, List<ExerciseItemResponse> exercises) {
        static List<DayResponse> from(Routine r) {
            Map<Integer, List<ExerciseItemResponse>> byDay = new TreeMap<>();
            r.getExercises().forEach(re -> byDay.computeIfAbsent(re.getDayOfWeek(), d -> new ArrayList<>()).add(ExerciseItemResponse.from(re)));
            r.getDayDescriptions().keySet().forEach(d -> byDay.computeIfAbsent(d, k -> new ArrayList<>()));
            return byDay.entrySet().stream()
                    .map(e -> new DayResponse(e.getKey(), r.getDayDescriptions().get(e.getKey()), e.getValue()))
                    .toList();
        }
    }

    public record FileResponse(Long id, FileType fileType, String originalName, String contentType, long sizeBytes,
                               Instant uploadedAt, String url) {
        public static FileResponse from(RoutineFile f) {
            return new FileResponse(f.getId(), f.getFileType(), f.getOriginalName(), f.getContentType(), f.getSizeBytes(),
                    f.getUploadedAt(), "/api/routines/" + f.getRoutine().getId() + "/files/" + f.getId());
        }
    }

    public record RoutineResponse(Long id, String name, String description, Discipline discipline, UserSummary trainer,
                                  List<UserSummary> members, List<DayResponse> days, List<FileResponse> files,
                                  Instant createdAt) {
        public static RoutineResponse from(Routine r) {
            return new RoutineResponse(r.getId(), r.getName(), r.getDescription(), r.getDiscipline(), UserSummary.from(r.getTrainer()),
                    r.getMembers().stream().map(UserSummary::from).toList(),
                    DayResponse.from(r),
                    r.getFiles().stream().map(FileResponse::from).toList(), r.getCreatedAt());
        }
    }
}
