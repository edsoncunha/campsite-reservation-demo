package io.github.edsoncunha.upgrade.takehome.domain.exceptions;

import lombok.AllArgsConstructor;

import java.text.MessageFormat;

@AllArgsConstructor
public class ReservationMaxDurationException extends RuntimeException {
    private final int maxAllowedLengthOfStay;

    @Override
    public String getMessage() {
        return MessageFormat.format("The campsite can be reserved for max {0} days", maxAllowedLengthOfStay);
    }
}
