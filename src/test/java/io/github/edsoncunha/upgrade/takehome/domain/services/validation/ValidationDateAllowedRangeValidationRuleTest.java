package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationDateNotInAllowedRangeException;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationDateAllowedRangeValidationRuleTest {
    @Mock
    private Clock clock;

    @InjectMocks
    private ValidationDateAllowedRangeValidationRule rule;

    @Test
    public void whenReservationIsLessThan1DayAheadAnExceptionMustBeThrown() {
        ZonedDateTime campsiteDateTime = ZonedDateTime.now().plusHours(3);

        when(clock.campsiteDateTime()).thenReturn(campsiteDateTime);

        assertThrows(ReservationDateNotInAllowedRangeException.class, () -> {
            rule.validate("dummy", LocalDate.now(), 1);
        });
    }

    @Test
    public void whenReservationIs1DayAheadNoExceptionMustBeThrown() {
        ZonedDateTime now = ZonedDateTime.of(LocalDate.now(), LocalTime.NOON, ZoneId.of("UTC"));

        when(clock.campsiteDateTime()).thenReturn(now);

        rule.validate("dummy", now.plusHours(24).toLocalDate(), 1);
    }

    @Test
    public void whenReservationIsMoreThan30DaysAheadAnExceptionMustBeThrown() {
        ZonedDateTime campsiteDateTime = ZonedDateTime.now().plusHours(3);

        when(clock.campsiteDateTime()).thenReturn(campsiteDateTime);

        assertThrows(ReservationDateNotInAllowedRangeException.class, () -> {
            rule.validate("dummy", LocalDate.now(), 31);
        });
    }

    @Test
    public void exceptionDateRangeIsCalculatedCorrectly() {
        ZonedDateTime campsiteDateTime = ZonedDateTime.of(
                LocalDate.of(2001,1,1),
                LocalTime.MIDNIGHT,
                ZoneId.of("UTC"));

        when(clock.campsiteDateTime()).thenReturn(campsiteDateTime);

        try {
            rule.validate("dummy", campsiteDateTime.toLocalDate(), 31);
        } catch (ReservationDateNotInAllowedRangeException e) {
            assertThat(e.getAllowedStart().toString()).isEqualTo(LocalDate.of(2001,1,2).toString());
            assertThat(e.getAllowedEnd().toString()).isEqualTo(LocalDate.of(2001,1,31).toString());

            return; // successful
        }

        fail("ReservationDateNotInAllowedRangeException was expected, but not thrown");
    }

    @Test
    public void whenReservation30DaysAheadNoExceptionMustBeThrown() {
        ZonedDateTime campsiteDateTime = LocalDate.of(2001, 1, 1)
                .atStartOfDay(ZoneId.of("UTC")).plusHours(-1);

        when(clock.campsiteDateTime()).thenReturn(campsiteDateTime);

        rule.validate("dummy", LocalDate.of(2001, 1, 1), 30);
    }


}