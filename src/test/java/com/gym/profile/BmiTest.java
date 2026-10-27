package com.gym.profile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BmiTest {
    @Test
    void calculaElImcConUnDecimal() {
        assertThat(ProfileDtos.bmi(new BigDecimal("72.5"), new BigDecimal("165"))).isEqualByComparingTo("26.6");
    }

    @Test
    void sinPesoOEstaturaNoHayImc() {
        assertThat(ProfileDtos.bmi(null, new BigDecimal("165"))).isNull();
        assertThat(ProfileDtos.bmi(new BigDecimal("70"), null)).isNull();
    }
}
