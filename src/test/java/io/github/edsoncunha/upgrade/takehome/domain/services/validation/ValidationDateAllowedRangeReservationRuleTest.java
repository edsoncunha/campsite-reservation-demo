package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationDateNotInAllowedRangeException;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationDateAllowedRangeReservationRuleTest {
    @Mock
    private Clock clock;

    @InjectMocks
    private ValidationDateAllowedRangeReservationRule rule;

    @Test
    public void whenReservationIsLessThan1DayAheadAnExceptionMustBeThrown() {
        LocalDateTime campsiteDateTime = LocalDateTime.now().plusHours(3);

        when(clock.now()).thenReturn(campsiteDateTime);

        assertThrows(ReservationDateNotInAllowedRangeException.class, () -> {
            rule.validate("dummy", LocalDate.now(), 1);
        });
    }

    @Test
    public void whenReservationIs1DayAheadNoExceptionMustBeThrown() {
        LocalDateTime now = LocalDateTime.of(LocalDate.now(), LocalTime.NOON);

        when(clock.now()).thenReturn(now);

        rule.validate("dummy", now.plusHours(24).toLocalDate(), 1);
    }

    @Test
    public void whenReservationIsMoreThan30DaysAheadAnExceptionMustBeThrown() {
        LocalDateTime campsiteDateTime = LocalDateTime.now().plusHours(3);

        when(clock.now()).thenReturn(campsiteDateTime);

        assertThrows(ReservationDateNotInAllowedRangeException.class, () -> {
            rule.validate("dummy", LocalDate.now(), 31);
        });
    }

    @Test
    public void exceptionDateRangeIsCalculatedCorrectly() {
        LocalDateTime campsiteDateTime = LocalDateTime.of(
                LocalDate.of(2001,1,1),
                LocalTime.MIDNIGHT);

        when(clock.now()).thenReturn(campsiteDateTime);

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
        LocalDate dt = LocalDate.of(2001, 1, 1);
        LocalDateTime campsiteDateTime = LocalDateTime.of(dt, LocalTime.MIDNIGHT).plusHours(-1);

        when(clock.now()).thenReturn(campsiteDateTime);

        rule.validate("dummy", LocalDate.of(2001, 1, 1), 30);
    }
}