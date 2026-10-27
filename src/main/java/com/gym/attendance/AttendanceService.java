package com.gym.attendance;

import com.gym.billing.BillingService;
import com.gym.billing.MembershipStatus;
import com.gym.common.ApiException;
import com.gym.security.AuthUser;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {
    private final AttendanceRepository attendances;
    private final UserRepository users;
    private final BillingService billing;

    /**
     * Un socio registra su propia entrada; administrador y entrenador pueden registrar la de cualquiera.
     * Si el socio no tiene la membresía vigente se rechaza; quien gestiona asistencia puede forzarla con {@code force}.
     */
    public Attendance checkIn(Long userId, boolean force, AuthUser me) {
        Long target = userId == null ? me.id() : userId;
        requireCanManage(target, me);
        User user = users.findById(target).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
        if (!user.isActive()) {
            throw ApiException.badRequest("El usuario está inactivo");
        }
        openToday(target).ifPresent(a -> {
            throw new ApiException(HttpStatus.CONFLICT, user.getFullName() + " ya registró su entrada hoy");
        });
        if (user.hasRole(Role.SOCIO) && !(force && me.managesAttendance())) {
            requireActiveMembership(user);
        }
        Attendance a = new Attendance();
        a.setUser(user);
        a.setRegisteredBy(users.getReferenceById(me.id()));
        return attendances.save(a);
    }

    public enum ScanAction { ENTRADA, YA_REGISTRADA }

    public record ScanResult(ScanAction action, Attendance attendance, MembershipStatus membership) {}

    /**
     * Lectura de la credencial QR en recepción: registra la entrada (con la revisión de membresía).
     * Si ya entró hoy no se duplica. La salida no se marca aquí: es opcional y la registra el socio.
     */
    public ScanResult scan(String code, boolean force, AuthUser me) {
        // Tolera que un lector USB cambie el ":" del prefijo por otro símbolo según el teclado.
        String token = (code == null ? "" : code.trim()).replaceFirst("(?i)^GYM[^A-Za-z0-9]?", "");
        User user = users.findByQrToken(token)
                .orElseThrow(() -> ApiException.notFound("El código no corresponde a ninguna credencial"));
        Optional<Attendance> today = openToday(user.getId());
        Attendance a = today.orElseGet(() -> checkIn(user.getId(), force, me));
        MembershipStatus st = user.hasRole(Role.SOCIO) ? billing.status(user.getId()) : null;
        return new ScanResult(today.isPresent() ? ScanAction.YA_REGISTRADA : ScanAction.ENTRADA, a, st);
    }

    public Attendance checkOut(Long userId, AuthUser me) {
        Long target = userId == null ? me.id() : userId;
        requireCanManage(target, me);
        Attendance a = openToday(target)
                .orElseThrow(() -> ApiException.badRequest("No hay una entrada de hoy sin salida"));
        a.setCheckOutAt(Instant.now());
        return a;
    }

    @Transactional(readOnly = true)
    public List<Attendance> search(Long userId, Instant from, Instant to, AuthUser me) {
        Long target = me.managesAttendance() ? userId : me.id();
        return attendances.search(target, from, to);
    }

    /** La salida es opcional: una entrada sin salida solo "sigue abierta" el mismo día. */
    private Optional<Attendance> openToday(Long userId) {
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        return attendances.findFirstByUserIdAndCheckOutAtIsNullAndCheckInAtGreaterThanEqualOrderByCheckInAtDesc(userId, startOfDay);
    }

    private void requireActiveMembership(User user) {
        MembershipStatus st = billing.status(user.getId());
        boolean covered = st.state() == MembershipStatus.State.VIGENTE || st.state() == MembershipStatus.State.POR_VENCER;
        if (covered) {
            return;
        }
        String detail = st.paidUntil() == null
                ? user.getFullName() + " no tiene pagos registrados"
                : "La membresía de " + user.getFullName() + " venció el " + st.paidUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "MEMBRESIA_VENCIDA", detail).with("userId", user.getId());
    }

    private void requireCanManage(Long target, AuthUser me) {
        if (!me.managesAttendance() && !target.equals(me.id())) {
            throw ApiException.forbidden("Solo puedes registrar tu propia asistencia");
        }
    }
}
