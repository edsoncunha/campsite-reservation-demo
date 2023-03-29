package io.github.edsoncunha.upgrade.takehome.api.requests;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReservationRequest {
    @Parameter(description = "Arrival date", required = true, example = "2023-10-10")
    public LocalDate arrivalDate;
    @Parameter(description = "Length of stay (in days)", required = true, example = "1")
    public int lengthOfStay;
}
