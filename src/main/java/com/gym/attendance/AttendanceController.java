package com.gym.attendance;

import com.gym.billing.MembershipStatus;
import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("hasAnyAuthority('ASISTENCIA','ASISTENCIA_GESTION')")
@RequiredArgsConstructor
@Transactional
public class AttendanceController {
    private final AttendanceService attendanceService;

    /** force: registrar la entrada aunque la membresía esté vencida (solo quien gestiona asistencia). */
    public record AttendanceRequest(Long userId, boolean force) {}

    public record AttendanceResponse(Long id, Long userId, String userName, Instant checkInAt, Instant checkOutAt) {
        static AttendanceResponse from(Attendance a) {
            return new AttendanceResponse(a.getId(), a.getUser().getId(), a.getUser().getFullName(), a.getCheckInAt(), a.getCheckOutAt());
        }
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse checkIn(@RequestBody(required = false) AttendanceRequest req, @CurrentUser AuthUser me) {
        return AttendanceResponse.from(attendanceService.checkIn(req == null ? null : req.userId(), req != null && req.force(), me));
    }

    public record ScanRequest(String code, boolean force) {}

    public record ScanResponse(AttendanceService.ScanAction action, AttendanceResponse attendance, MembershipStatus membership,
                               String photoVersion) {}

    /** Recepción lee la credencial QR del socio y registra su entrada (una por día). */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('ASISTENCIA_GESTION')")
    public ScanResponse scan(@RequestBody ScanRequest req, @CurrentUser AuthUser me) {
        AttendanceService.ScanResult r = attendanceService.scan(req.code(), req.force(), me);
        return new ScanResponse(r.action(), AttendanceResponse.from(r.attendance()), r.membership(),
                r.attendance().getUser().photoVersion());
    }

    /** Salida opcional: hoy la marca recepción si quiere; después, el socio desde la app móvil. */
    @PostMapping("/check-out")
    public AttendanceResponse checkOut(@RequestBody(required = false) AttendanceRequest req, @CurrentUser AuthUser me) {
        return AttendanceResponse.from(attendanceService.checkOut(req == null ? null : req.userId(), me));
    }

    /** Lista asistencias entre dos fechas (por defecto, los últimos 30 días). Un socio solo ve las suyas. */
    @GetMapping
    public List<AttendanceResponse> search(@RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @CurrentUser AuthUser me) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate end = to == null ? LocalDate.now(zone) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        return attendanceService.search(userId, start.atStartOfDay(zone).toInstant(),
                        end.plusDays(1).atStartOfDay(zone).toInstant(), me)
                .stream().map(AttendanceResponse::from).toList();
    }
}
