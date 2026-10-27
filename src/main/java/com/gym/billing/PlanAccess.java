package com.gym.billing;

import com.gym.routine.Discipline;

import java.util.Set;

/** A qué da acceso una membresía. */
public enum PlanAccess {
    GYM, CROSSFIT, AMBOS;

    /** Si un socio con esos subroles puede contratar esta membresía. */
    public boolean fits(Set<Discipline> disciplines) {
        return switch (this) {
            case GYM -> disciplines.contains(Discipline.GYM);
            case CROSSFIT -> disciplines.contains(Discipline.CROSSFIT);
            case AMBOS -> disciplines.containsAll(Set.of(Discipline.GYM, Discipline.CROSSFIT));
        };
    }
}
