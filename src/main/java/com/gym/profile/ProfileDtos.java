package com.gym.profile;

import com.gym.routine.Discipline;
import com.gym.user.User;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record ProfileResponse(Long id, String fullName, String email, String phone, Set<String> roles,
                                  Set<Discipline> disciplines, LocalDate enrolledOn, LocalDate birthDate,
                                  String photoVersion, String emergencyName, String emergencyPhone, String medicalNotes) {
        public static ProfileResponse from(User u) {
            return new ProfileResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.roleNames(),
                    new TreeSet<>(u.getDisciplines()), u.getEnrolledOn(), u.getBirthDate(), u.photoVersion(),
                    u.getEmergencyName(), u.getEmergencyPhone(), u.getMedicalNotes());
        }
    }

    public record ProfileRequest(
            @Size(max = 40) String phone,
            LocalDate birthDate,
            @Size(max = 120) String emergencyName,
            @Size(max = 40) String emergencyPhone,
            @Size(max = 1000) String medicalNotes) {}

    public record MeasurementRequest(
            @NotNull LocalDate measuredOn,
            @DecimalMin(value = "20", message = "Debe estar entre 20 y 400") @DecimalMax(value = "400", message = "Debe estar entre 20 y 400") BigDecimal weightKg,
            @DecimalMin(value = "80", message = "Debe estar entre 80 y 250") @DecimalMax(value = "250", message = "Debe estar entre 80 y 250") BigDecimal heightCm,
            @DecimalMin(value = "2", message = "Debe estar entre 2 y 70") @DecimalMax(value = "70", message = "Debe estar entre 2 y 70") BigDecimal bodyFatPct,
            @DecimalMin(value = "1", message = "Debe estar entre 1 y 300") @DecimalMax(value = "300", message = "Debe estar entre 1 y 300") BigDecimal waistCm,
            @DecimalMin(value = "1", message = "Debe estar entre 1 y 300") @DecimalMax(value = "300", message = "Debe estar entre 1 y 300") BigDecimal chestCm,
            @DecimalMin(value = "1", message = "Debe estar entre 1 y 300") @DecimalMax(value = "300", message = "Debe estar entre 1 y 300") BigDecimal hipCm,
            @DecimalMin(value = "1", message = "Debe estar entre 1 y 100") @DecimalMax(value = "100", message = "Debe estar entre 1 y 100") BigDecimal armCm,
            @DecimalMin(value = "1", message = "Debe estar entre 1 y 150") @DecimalMax(value = "150", message = "Debe estar entre 1 y 150") BigDecimal thighCm,
            @Size(max = 500) String notes) {}

    public record MeasurementResponse(Long id, LocalDate measuredOn, BigDecimal weightKg, BigDecimal heightCm,
                                      BigDecimal bodyFatPct, BigDecimal waistCm, BigDecimal chestCm, BigDecimal hipCm,
                                      BigDecimal armCm, BigDecimal thighCm, BigDecimal bmi, String notes, String registeredBy) {
        public static MeasurementResponse from(BodyMeasurement m, BigDecimal height) {
            return new MeasurementResponse(m.getId(), m.getMeasuredOn(), m.getWeightKg(), m.getHeightCm(), m.getBodyFatPct(),
                    m.getWaistCm(), m.getChestCm(), m.getHipCm(), m.getArmCm(), m.getThighCm(), ProfileDtos.bmi(m.getWeightKg(), height),
                    m.getNotes(), m.getRegisteredBy() == null ? null : m.getRegisteredBy().getFullName());
        }
    }

    /** Fila del listado de progreso: el socio y su última toma. */
    public record ProgressRow(Long userId, String fullName, Set<Discipline> disciplines, String photoVersion,
                              LocalDate lastMeasuredOn, BigDecimal lastWeightKg) {}

    /** IMC = kg / m². La estatura puede venir de una toma anterior. */
    static BigDecimal bmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.signum() == 0) {
            return null;
        }
        BigDecimal m = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(m.multiply(m), 1, RoundingMode.HALF_UP);
    }
}
