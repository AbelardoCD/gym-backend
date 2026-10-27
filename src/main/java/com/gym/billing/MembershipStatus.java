package com.gym.billing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Estado de la membresía de un usuario a partir de su último día pagado.
 * daysLeft es negativo cuando ya venció.
 */
public record MembershipStatus(Long userId, LocalDate enrolledOn, LocalDate paidUntil, State state, Long daysLeft) {
    public enum State { VIGENTE, POR_VENCER, VENCIDA, SIN_PAGOS }

    /** Días antes del vencimiento en los que se avisa. */
    static final int WARNING_DAYS = 5;

    public static MembershipStatus of(Long userId, LocalDate enrolledOn, LocalDate paidUntil, LocalDate today) {
        if (paidUntil == null) {
            return new MembershipStatus(userId, enrolledOn, null, State.SIN_PAGOS, null);
        }
        long left = ChronoUnit.DAYS.between(today, paidUntil);
        State state = left < 0 ? State.VENCIDA : left <= WARNING_DAYS ? State.POR_VENCER : State.VIGENTE;
        return new MembershipStatus(userId, enrolledOn, paidUntil, state, left);
    }
}
