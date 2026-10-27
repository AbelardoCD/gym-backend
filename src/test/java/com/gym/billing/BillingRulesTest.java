package com.gym.billing;

import com.gym.routine.Discipline;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BillingRulesTest {
    private static final LocalDate START = LocalDate.of(2026, 3, 10);

    @Test
    void cadaPeriodoCubreHastaElDiaAnteriorAlSiguiente() {
        assertThat(PlanPeriod.DIA.coversTo(START, 1)).isEqualTo(START);
        assertThat(PlanPeriod.DIA.coversTo(START, 3)).isEqualTo(LocalDate.of(2026, 3, 12));
        assertThat(PlanPeriod.SEMANA.coversTo(START, 2)).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(PlanPeriod.MES.coversTo(START, 1)).isEqualTo(LocalDate.of(2026, 4, 9));
        assertThat(PlanPeriod.ANIO.coversTo(START, 1)).isEqualTo(LocalDate.of(2027, 3, 9));
    }

    @Test
    void laMembresiaDebeCorresponderALosSubrolesDelSocio() {
        Set<Discipline> gym = Set.of(Discipline.GYM);
        Set<Discipline> ambos = Set.of(Discipline.GYM, Discipline.CROSSFIT);
        assertThat(PlanAccess.GYM.fits(gym)).isTrue();
        assertThat(PlanAccess.CROSSFIT.fits(gym)).isFalse();
        assertThat(PlanAccess.AMBOS.fits(gym)).isFalse();
        assertThat(PlanAccess.AMBOS.fits(ambos)).isTrue();
        assertThat(PlanAccess.CROSSFIT.fits(ambos)).isTrue();
    }

    @Test
    void estadoDeLaMembresiaSegunElUltimoDiaPagado() {
        LocalDate today = LocalDate.of(2026, 10, 24);
        assertThat(MembershipStatus.of(1L, today, null, today).state()).isEqualTo(MembershipStatus.State.SIN_PAGOS);
        assertThat(MembershipStatus.of(1L, today, today.plusDays(20), today).state()).isEqualTo(MembershipStatus.State.VIGENTE);
        assertThat(MembershipStatus.of(1L, today, today.plusDays(5), today).state()).isEqualTo(MembershipStatus.State.POR_VENCER);
        assertThat(MembershipStatus.of(1L, today, today, today).state()).isEqualTo(MembershipStatus.State.POR_VENCER);
        MembershipStatus vencida = MembershipStatus.of(1L, today, today.minusDays(1), today);
        assertThat(vencida.state()).isEqualTo(MembershipStatus.State.VENCIDA);
        assertThat(vencida.daysLeft()).isEqualTo(-1);
    }
}
