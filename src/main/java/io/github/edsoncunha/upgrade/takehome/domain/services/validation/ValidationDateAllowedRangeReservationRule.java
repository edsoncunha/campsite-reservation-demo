package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationDateNotInAllowedRangeException;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
@AllArgsConstructor
public class ValidationDateAllowedRangeReservationRule implements ReservationRule {
    private static final int MIN_ALLOWED_DAYS_IN_ADVANCE = 1;
    private static final int MAX_ALLOWED_DAYS_IN_ADVANCE = 30;

    private Clock clock;

    @Override
    public void validate(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        ZonedDateTime campsiteNow = clock.campsiteDateTime();

        LocalDate campsiteCurrentDate = campsiteNow.toLocalDate();

        long daysInAdvance = ChronoUnit.DAYS.between(campsiteCurrentDate, arrivalDate);

        if (daysInAdvance < MIN_ALLOWED_DAYS_IN_ADVANCE || daysInAdvance > MAX_ALLOWED_DAYS_IN_ADVANCE) {
            LocalDate allowedStart = arrivalDate.plusDays(MIN_ALLOWED_DAYS_IN_ADVANCE);
            LocalDate allowedEnd = arrivalDate.plusDays(MAX_ALLOWED_DAYS_IN_ADVANCE);

            throw new ReservationDateNotInAllowedRangeException(allowedStart, allowedEnd, arrivalDate);
        }
    }


}
