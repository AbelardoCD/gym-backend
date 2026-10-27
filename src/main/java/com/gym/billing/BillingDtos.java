package com.gym.billing;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BillingDtos {
    private BillingDtos() {}

    public record PlanRequest(@NotBlank @Size(max = 80) String name,
                              @NotNull PlanAccess access,
                              @NotNull PlanPeriod period,
                              @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price,
                              boolean active) {}

    public record PlanResponse(Long id, String name, PlanAccess access, PlanPeriod period, BigDecimal price, boolean active,
                               long payments) {
        static PlanResponse from(MembershipPlan p, long payments) {
            return new PlanResponse(p.getId(), p.getName(), p.getAccess(), p.getPeriod(), p.getPrice(), p.isActive(), payments);
        }
    }

    /** coversFrom y amount son opcionales: si no vienen se calculan. */
    public record PaymentRequest(@NotNull Long userId,
                                 @NotNull Long planId,
                                 @Min(1) @Max(366) int quantity,
                                 LocalDate coversFrom,
                                 @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal amount,
                                 @NotNull PaymentMethod method,
                                 @Size(max = 255) String notes) {}

    public record PaymentPreview(LocalDate coversFrom, LocalDate coversTo, BigDecimal amount, MembershipStatus status) {}

    public record PaymentResponse(Long id, Long userId, String userName, Long planId, String planName, PlanPeriod period,
                                  int quantity, BigDecimal amount, PaymentMethod method, LocalDate coversFrom,
                                  LocalDate coversTo, String notes, String registeredBy, Instant paidAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getUser().getId(), p.getUser().getFullName(),
                    p.getPlan() == null ? null : p.getPlan().getId(), p.getPlanName(), p.getPeriod(), p.getQuantity(),
                    p.getAmount(), p.getMethod(), p.getCoversFrom(), p.getCoversTo(), p.getNotes(),
                    p.getRegisteredBy().getFullName(), p.getPaidAt());
        }
    }
}
