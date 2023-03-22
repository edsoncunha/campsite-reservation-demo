package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationMaxDurationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LengthOfStayValidationRule implements ValidationRule {

    private static final int MAX_ALLOWED_LENGTH_OF_STAY = 3;

    @Override
    public void validate(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        if (lengthOfStay > MAX_ALLOWED_LENGTH_OF_STAY) {
            throw new ReservationMaxDurationException(MAX_ALLOWED_LENGTH_OF_STAY);
        }
    }
}
