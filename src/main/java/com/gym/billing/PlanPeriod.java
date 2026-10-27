package com.gym.billing;

import java.time.LocalDate;

/** Periodo que cubre una membresía. */
public enum PlanPeriod {
    DIA, SEMANA, MES, ANIO;

    /** Último día cubierto al pagar {@code quantity} periodos empezando en {@code start}. */
    public LocalDate coversTo(LocalDate start, int quantity) {
        LocalDate next = switch (this) {
            case DIA -> start.plusDays(quantity);
            case SEMANA -> start.plusWeeks(quantity);
            case MES -> start.plusMonths(quantity);
            case ANIO -> start.plusYears(quantity);
        };
        return next.minusDays(1);
    }
}
