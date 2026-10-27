package com.gym.report;

import com.gym.attendance.Attendance;
import com.gym.attendance.AttendanceRepository;
import com.gym.billing.BillingService;
import com.gym.billing.MembershipStatus;
import com.gym.billing.Payment;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cifras para la pantalla de reportes. Todo se agrupa en la zona horaria del servidor. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final BillingService billing;
    private final AttendanceRepository attendances;
    private final UserRepository users;

    public record Point(String label, BigDecimal value) {}

    public record Count(String label, long value) {}

    public record PlanTotal(String plan, long payments, BigDecimal total) {}

    public record Members(long vigentes, long porVencer, long vencidas, long sinPagos) {}

    public record Summary(BigDecimal income, long payments, long visits, long newMembers, Members members,
                          List<Point> incomeByMonth, List<Point> incomeByMethod, List<PlanTotal> incomeByPlan,
                          List<Count> visitsByHour, List<Count> visitsByWeekday, List<Count> enrollmentsByMonth) {}

    private static final String[] WEEKDAYS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    public Summary summary(LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        List<Payment> pays = billing.search(null, from, to);
        List<Attendance> visits = attendances.search(null, from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
        List<User> socios = users.findDistinctByRoles_NameOrderByFullName(Role.SOCIO).stream().filter(User::isActive).toList();

        BigDecimal income = sum(pays, p -> true);

        // Ingresos de los últimos 12 meses hasta "to", sin importar el rango elegido.
        YearMonth last = YearMonth.from(to);
        YearMonth first = last.minusMonths(11);
        List<Payment> year = billing.search(null, first.atDay(1), to);
        List<Point> byMonth = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            YearMonth month = m;
            byMonth.add(new Point(month.toString(), sum(year, p -> YearMonth.from(p.getPaidAt().atZone(zone)).equals(month))));
        }

        List<Point> byMethod = Arrays.stream(com.gym.billing.PaymentMethod.values())
                .map(m -> new Point(m.name(), sum(pays, p -> p.getMethod() == m))).toList();

        List<PlanTotal> byPlan = pays.stream().collect(Collectors.groupingBy(Payment::getPlanName)).entrySet().stream()
                .map(e -> new PlanTotal(e.getKey(), e.getValue().size(), sum(e.getValue(), p -> true)))
                .sorted(Comparator.comparing(PlanTotal::total).reversed()).toList();

        long[] hours = new long[24];
        long[] days = new long[7];
        for (Attendance a : visits) {
            ZonedDateTime t = a.getCheckInAt().atZone(zone);
            hours[t.getHour()]++;
            days[t.getDayOfWeek().getValue() - 1]++;
        }
        List<Count> byHour = new ArrayList<>();
        for (int h = 5; h <= 23; h++) {
            byHour.add(new Count(h + ":00", hours[h]));
        }
        List<Count> byWeekday = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            byWeekday.add(new Count(WEEKDAYS[d], days[d]));
        }

        Map<YearMonth, Long> enrolled = socios.stream()
                .collect(Collectors.groupingBy(u -> YearMonth.from(u.getEnrolledOn()), Collectors.counting()));
        List<Count> enrollments = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            enrollments.add(new Count(m.toString(), enrolled.getOrDefault(m, 0L)));
        }
        long newMembers = socios.stream().filter(u -> !u.getEnrolledOn().isBefore(from) && !u.getEnrolledOn().isAfter(to)).count();

        Map<MembershipStatus.State, Long> states = billing.statuses(socios).stream()
                .collect(Collectors.groupingBy(MembershipStatus::state, Collectors.counting()));
        Function<MembershipStatus.State, Long> st = s -> states.getOrDefault(s, 0L);
        Members members = new Members(st.apply(MembershipStatus.State.VIGENTE), st.apply(MembershipStatus.State.POR_VENCER),
                st.apply(MembershipStatus.State.VENCIDA), st.apply(MembershipStatus.State.SIN_PAGOS));

        return new Summary(income, pays.size(), visits.size(), newMembers, members, byMonth, byMethod, byPlan,
                byHour, byWeekday, enrollments);
    }

    private static BigDecimal sum(List<Payment> list, java.util.function.Predicate<Payment> filter) {
        return list.stream().filter(filter).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
