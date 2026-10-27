package com.gym.billing;

import com.gym.billing.BillingDtos.PaymentPreview;
import com.gym.billing.BillingDtos.PaymentRequest;
import com.gym.billing.BillingDtos.PlanRequest;
import com.gym.common.ApiException;
import com.gym.security.AuthUser;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingService {
    private final MembershipPlanRepository plans;
    private final PaymentRepository payments;
    private final UserRepository users;

    private static LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    // ---- Membresías ----

    @Transactional(readOnly = true)
    public List<MembershipPlan> plans() {
        return plans.findAll().stream()
                .sorted(java.util.Comparator.comparing(MembershipPlan::getAccess).thenComparing(MembershipPlan::getPeriod))
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipPlan plan(Long id) {
        return plans.findById(id).orElseThrow(() -> ApiException.notFound("Membresía no encontrada"));
    }

    public long paymentsOf(MembershipPlan p) {
        return payments.countByPlanId(p.getId());
    }

    public MembershipPlan savePlan(Long id, PlanRequest req) {
        String name = req.name().trim();
        boolean taken = id == null ? plans.existsByNameIgnoreCase(name) : plans.existsByNameIgnoreCaseAndIdNot(name, id);
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una membresía con ese nombre");
        }
        MembershipPlan p = id == null ? new MembershipPlan() : plan(id);
        p.setName(name);
        p.setAccess(req.access());
        p.setPeriod(req.period());
        p.setPrice(req.price());
        p.setActive(req.active());
        return plans.save(p);
    }

    public void deletePlan(Long id) {
        MembershipPlan p = plan(id);
        if (payments.countByPlanId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta membresía ya tiene pagos; desactívala en lugar de borrarla");
        }
        plans.delete(p);
    }

    // ---- Pagos ----

    @Transactional(readOnly = true)
    public List<Payment> search(Long userId, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        return payments.search(userId, start, end);
    }

    @Transactional(readOnly = true)
    public Payment payment(Long id) {
        return payments.findById(id).orElseThrow(() -> ApiException.notFound("Pago no encontrado"));
    }

    @Transactional(readOnly = true)
    public MembershipStatus status(Long userId) {
        User u = user(userId);
        return MembershipStatus.of(u.getId(), u.getEnrolledOn(), payments.paidUntil(userId).orElse(null), today());
    }

    @Transactional(readOnly = true)
    public List<MembershipStatus> statuses(Collection<User> list) {
        Map<Long, LocalDate> paid = new HashMap<>();
        for (Object[] row : payments.paidUntilByUser(list.stream().map(User::getId).toList())) {
            paid.put((Long) row[0], (LocalDate) row[1]);
        }
        LocalDate today = today();
        return list.stream().map(u -> MembershipStatus.of(u.getId(), u.getEnrolledOn(), paid.get(u.getId()), today)).toList();
    }

    /**
     * Calcula el periodo que cubre un pago. Si el socio ya pagó antes, sigue desde el día siguiente a lo pagado,
     * aunque haya vencido, para respetar el ciclo desde su inscripción. Si es su primer pago, empieza el día de inscripción.
     */
    @Transactional(readOnly = true)
    public PaymentPreview preview(Long userId, Long planId, int quantity, LocalDate coversFrom) {
        User u = user(userId);
        MembershipPlan p = plan(planId);
        MembershipStatus st = status(userId);
        LocalDate start = coversFrom != null ? coversFrom
                : st.paidUntil() != null ? st.paidUntil().plusDays(1) : u.getEnrolledOn();
        int qty = Math.max(quantity, 1);
        return new PaymentPreview(start, p.getPeriod().coversTo(start, qty), p.getPrice().multiply(BigDecimal.valueOf(qty)), st);
    }

    public Payment register(PaymentRequest req, AuthUser me) {
        User u = user(req.userId());
        MembershipPlan p = plan(req.planId());
        if (!u.isActive()) {
            throw ApiException.badRequest("El usuario está inactivo");
        }
        if (!u.hasRole(Role.SOCIO)) {
            throw ApiException.badRequest(u.getFullName() + " no es socio");
        }
        if (!p.isActive()) {
            throw ApiException.badRequest("La membresía " + p.getName() + " está desactivada");
        }
        if (!p.getAccess().fits(u.getDisciplines())) {
            throw ApiException.badRequest("La membresía " + p.getName() + " no corresponde a los subroles de " + u.getFullName());
        }
        PaymentPreview calc = preview(u.getId(), p.getId(), req.quantity(), req.coversFrom());

        Payment pay = new Payment();
        pay.setUser(u);
        pay.setPlan(p);
        pay.setPlanName(p.getName());
        pay.setPeriod(p.getPeriod());
        pay.setQuantity(req.quantity());
        pay.setAmount(req.amount() != null ? req.amount() : calc.amount());
        pay.setMethod(req.method());
        pay.setCoversFrom(calc.coversFrom());
        pay.setCoversTo(calc.coversTo());
        pay.setNotes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim());
        pay.setRegisteredBy(user(me.id()));
        return payments.save(pay);
    }

    /** Solo el administrador puede anular un pago (por ejemplo, si se capturó mal). */
    public void cancel(Long id, AuthUser me) {
        if (!me.isAdmin()) {
            throw ApiException.forbidden("Solo un administrador puede anular pagos");
        }
        payments.delete(payment(id));
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }
}
