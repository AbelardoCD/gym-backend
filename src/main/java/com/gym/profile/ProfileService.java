package com.gym.profile;

import com.gym.common.ApiException;
import com.gym.profile.ProfileDtos.*;
import com.gym.security.AuthUser;
import com.gym.storage.StorageService;
import com.gym.user.Permission;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final UserRepository users;
    private final BodyMeasurementRepository measurements;
    private final StorageService storage;

    @Transactional(readOnly = true)
    public User get(Long id, AuthUser me) {
        requireCanView(id, me);
        return find(id);
    }

    /** El propio usuario o quien administra usuarios. */
    public User update(Long id, ProfileRequest req, AuthUser me) {
        requireCanEdit(id, me);
        User u = find(id);
        u.setPhone(blankToNull(req.phone()));
        u.setBirthDate(req.birthDate());
        u.setEmergencyName(blankToNull(req.emergencyName()));
        u.setEmergencyPhone(blankToNull(req.emergencyPhone()));
        u.setMedicalNotes(blankToNull(req.medicalNotes()));
        return u;
    }

    public User setPhoto(Long id, MultipartFile file, AuthUser me) {
        requireCanEdit(id, me);
        if (file.isEmpty() || !PHOTO_TYPES.contains(String.valueOf(file.getContentType()).toLowerCase())) {
            throw ApiException.badRequest("La foto debe ser JPG, PNG o WEBP");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw ApiException.badRequest("La foto no puede pasar de 5 MB");
        }
        User u = find(id);
        String old = u.getPhotoKey();
        u.setPhotoKey(storage.store(file, "photos/" + id));
        if (old != null) {
            storage.delete(old);
        }
        return u;
    }

    public User deletePhoto(Long id, AuthUser me) {
        requireCanEdit(id, me);
        User u = find(id);
        if (u.getPhotoKey() != null) {
            storage.delete(u.getPhotoKey());
            u.setPhotoKey(null);
        }
        return u;
    }

    /** Cualquier usuario conectado puede ver fotos (aparecen en el chat y en recepción). */
    @Transactional(readOnly = true)
    public Resource photo(Long id) {
        User u = find(id);
        if (u.getPhotoKey() == null) {
            throw ApiException.notFound("Sin foto");
        }
        return storage.load(u.getPhotoKey());
    }

    @Transactional(readOnly = true)
    public List<MeasurementResponse> measurements(Long id, AuthUser me) {
        if (!id.equals(me.id()) && !isStaff(me)) {
            throw ApiException.forbidden("No puedes ver las medidas de este socio");
        }
        return withBmi(measurements.findByUserIdOrderByMeasuredOnAscIdAsc(id));
    }

    public MeasurementResponse addMeasurement(Long id, MeasurementRequest req, AuthUser me) {
        requireStaff(me);
        User u = find(id);
        if (!u.hasRole(Role.SOCIO)) {
            throw ApiException.badRequest("Solo se registran medidas de socios");
        }
        boolean empty = Stream.of(req.weightKg(), req.heightCm(), req.bodyFatPct(), req.waistCm(), req.chestCm(),
                req.hipCm(), req.armCm(), req.thighCm()).allMatch(Objects::isNull);
        if (empty) {
            throw ApiException.badRequest("Captura al menos una medida");
        }
        BodyMeasurement m = new BodyMeasurement();
        m.setUser(u);
        m.setMeasuredOn(req.measuredOn());
        m.setWeightKg(req.weightKg());
        m.setHeightCm(req.heightCm());
        m.setBodyFatPct(req.bodyFatPct());
        m.setWaistCm(req.waistCm());
        m.setChestCm(req.chestCm());
        m.setHipCm(req.hipCm());
        m.setArmCm(req.armCm());
        m.setThighCm(req.thighCm());
        m.setNotes(blankToNull(req.notes()));
        m.setRegisteredBy(users.getReferenceById(me.id()));
        measurements.save(m);
        List<MeasurementResponse> all = measurements(id, me);
        return all.stream().filter(r -> r.id().equals(m.getId())).findFirst().orElseThrow();
    }

    public void deleteMeasurement(Long id, Long measurementId, AuthUser me) {
        requireStaff(me);
        BodyMeasurement m = measurements.findById(measurementId)
                .filter(x -> x.getUser().getId().equals(id))
                .orElseThrow(() -> ApiException.notFound("Medida no encontrada"));
        measurements.delete(m);
    }

    /** Socios activos con su última toma, para la pantalla de progreso. */
    @Transactional(readOnly = true)
    public List<ProgressRow> progress() {
        Map<Long, BodyMeasurement> last = new HashMap<>();
        measurements.findAllByOrderByMeasuredOnAscIdAsc().forEach(m -> last.put(m.getUser().getId(), m));
        return users.findDistinctByRoles_NameOrderByFullName(Role.SOCIO).stream()
                .filter(User::isActive)
                .map(u -> {
                    BodyMeasurement m = last.get(u.getId());
                    return new ProgressRow(u.getId(), u.getFullName(), new TreeSet<>(u.getDisciplines()),
                            u.photoVersion(),
                            m == null ? null : m.getMeasuredOn(), m == null ? null : m.getWeightKg());
                })
                .toList();
    }

    /** El IMC usa la estatura de la toma o, si no se capturó, la última conocida. */
    private List<MeasurementResponse> withBmi(List<BodyMeasurement> list) {
        List<MeasurementResponse> out = new ArrayList<>();
        BigDecimal height = null;
        for (BodyMeasurement m : list) {
            if (m.getHeightCm() != null) {
                height = m.getHeightCm();
            }
            out.add(MeasurementResponse.from(m, height));
        }
        return out;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    private static boolean isStaff(AuthUser me) {
        return me.can(Permission.USUARIOS) || me.can(Permission.PROGRESO);
    }

    private void requireStaff(AuthUser me) {
        if (!isStaff(me)) {
            throw ApiException.forbidden("No puedes registrar medidas");
        }
    }

    /** Recepción también ve el perfil, por el contacto de emergencia. */
    private void requireCanView(Long id, AuthUser me) {
        if (!id.equals(me.id()) && !isStaff(me) && !me.can(Permission.ASISTENCIA_GESTION)) {
            throw ApiException.forbidden("No puedes ver este perfil");
        }
    }

    private void requireCanEdit(Long id, AuthUser me) {
        if (!id.equals(me.id()) && !me.can(Permission.USUARIOS)) {
            throw ApiException.forbidden("No puedes editar este perfil");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
