package io.github.edsoncunha.upgrade.takehome.domain.services.validation;

import java.time.LocalDate;

public interface ValidationRule {
    void validate(String userEmail, LocalDate arrivalDate, int lengthOfStay);
}
