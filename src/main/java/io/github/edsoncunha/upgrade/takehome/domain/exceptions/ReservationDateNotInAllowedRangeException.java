package io.github.edsoncunha.upgrade.takehome.domain.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.MessageFormat;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ReservationDateNotInAllowedRangeException extends ReservationConstraintException {
    private final LocalDate allowedStart;
    private final LocalDate allowedEnd;
    private final LocalDate requestedArrivalDate;

    @Override
    public String getMessage() {
        return MessageFormat.format("Selected date ({0}) is out of allowed range. Your reservation should start between {1} and {2}.", requestedArrivalDate, allowedStart, allowedEnd);
    }
}
