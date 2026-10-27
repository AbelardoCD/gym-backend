package com.gym.billing;

import com.gym.billing.BillingDtos.*;
import com.gym.security.AuthUser;
import com.gym.security.CurrentUser;
import com.gym.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Transactional
public class BillingController {
    private final BillingService billing;
    private final UserRepository users;
    private final MembershipReminderJob reminders;
    private final ReceiptService receipts;

    @Value("${app.receipts.auto-email}")
    private boolean autoEmailReceipts;

    // ---- Membresías ----

    @GetMapping("/api/plans")
    @PreAuthorize("hasAnyAuthority('PAGOS','MEMBRESIAS')")
    public List<PlanResponse> plans() {
        return billing.plans().stream().map(p -> PlanResponse.from(p, billing.paymentsOf(p))).toList();
    }

    @GetMapping("/api/plans/{id}")
    @PreAuthorize("hasAnyAuthority('PAGOS','MEMBRESIAS')")
    public PlanResponse plan(@PathVariable Long id) {
        MembershipPlan p = billing.plan(id);
        return PlanResponse.from(p, billing.paymentsOf(p));
    }

    @PostMapping("/api/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MEMBRESIAS')")
    public PlanResponse createPlan(@Valid @RequestBody PlanRequest req) {
        return PlanResponse.from(billing.savePlan(null, req), 0);
    }

    @PutMapping("/api/plans/{id}")
    @PreAuthorize("hasAuthority('MEMBRESIAS')")
    public PlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        MembershipPlan p = billing.savePlan(id, req);
        return PlanResponse.from(p, billing.paymentsOf(p));
    }

    @DeleteMapping("/api/plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MEMBRESIAS')")
    public void deletePlan(@PathVariable Long id) {
        billing.deletePlan(id);
    }

    // ---- Pagos ----

    /** Pagos entre dos fechas de cobro (por defecto, el mes en curso). */
    @GetMapping("/api/payments")
    @PreAuthorize("hasAuthority('PAGOS')")
    public List<PaymentResponse> payments(@RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return billing.search(userId, from == null ? today.withDayOfMonth(1) : from, to == null ? today : to)
                .stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/api/payments/{id}")
    @PreAuthorize("hasAuthority('PAGOS')")
    public PaymentResponse payment(@PathVariable Long id) {
        return PaymentResponse.from(billing.payment(id));
    }

    @GetMapping("/api/payments/preview")
    @PreAuthorize("hasAuthority('PAGOS')")
    public PaymentPreview preview(@RequestParam Long userId, @RequestParam Long planId,
                                  @RequestParam(defaultValue = "1") int quantity,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate coversFrom) {
        return billing.preview(userId, planId, quantity, coversFrom);
    }

    @PostMapping("/api/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAGOS')")
    public PaymentResponse register(@Valid @RequestBody PaymentRequest req, @CurrentUser AuthUser me) {
        Payment p = billing.register(req, me);
        if (autoEmailReceipts) {
            receipts.email(p);
        }
        return PaymentResponse.from(p);
    }

    /** Recibo en PDF. Lo puede bajar quien cobra o el propio socio. */
    @GetMapping("/api/payments/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id, @CurrentUser AuthUser me) {
        ReceiptService.Receipt r = receipts.pdf(id, me);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(r.filename()).build().toString())
                .body(r.pdf());
    }

    /** Reenvía el recibo al correo del socio. */
    @PostMapping("/api/payments/{id}/receipt/email")
    @PreAuthorize("hasAuthority('PAGOS')")
    public Map<String, String> emailReceipt(@PathVariable Long id) {
        return Map.of("sentTo", receipts.email(id));
    }

    @DeleteMapping("/api/payments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PAGOS')")
    public void cancel(@PathVariable Long id, @CurrentUser AuthUser me) {
        billing.cancel(id, me);
    }

    /** Manda ya los recordatorios de vencimiento pendientes (normalmente salen solos cada mañana). */
    @PostMapping("/api/payments/reminders/run")
    @PreAuthorize("hasAuthority('MEMBRESIAS')")
    public Map<String, Integer> runReminders() {
        return Map.of("sent", reminders.run());
    }

    /** Vigencia de todos los usuarios, para la tabla de usuarios. */
    @GetMapping("/api/payments/status")
    @PreAuthorize("hasAuthority('PAGOS')")
    public List<MembershipStatus> statuses() {
        return billing.statuses(users.findAllByOrderByFullName());
    }

    /** Vigencia y pagos del usuario conectado. */
    @GetMapping("/api/payments/mine")
    public MyPayments mine(@CurrentUser AuthUser me) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return new MyPayments(billing.status(me.id()),
                billing.search(me.id(), today.minusYears(2), today).stream().map(PaymentResponse::from).toList());
    }

    public record MyPayments(MembershipStatus status, List<PaymentResponse> payments) {}
}
