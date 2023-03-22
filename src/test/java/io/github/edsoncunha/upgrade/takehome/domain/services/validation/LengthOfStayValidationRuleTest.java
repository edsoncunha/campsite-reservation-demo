package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationMaxDurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class LengthOfStayValidationRuleTest {
    private final LengthOfStayValidationRule rule = new LengthOfStayValidationRule();

    @Test
    public void campsiteCantBeReservedForMoreThan3Days()  {
        Assertions.assertThrows(ReservationMaxDurationException.class, () -> {
            rule.validate("dummy@test.com", LocalDate.now(), 4);
        });
    }

    @Test
    public void campsiteCanBeReservedFor3Days() {
        rule.validate("dummy@test.com", LocalDate.now(), 3);
    }
}