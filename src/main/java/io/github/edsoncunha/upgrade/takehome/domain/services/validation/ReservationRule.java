package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import java.time.LocalDate;

public interface ReservationRule {
    void validate(String userEmail, LocalDate arrivalDate, int lengthOfStay);
}
