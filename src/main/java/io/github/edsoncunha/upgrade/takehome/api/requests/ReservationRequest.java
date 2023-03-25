package io.github.edsoncunha.upgrade.takehome.api.requests;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationRequest {
    public String email;
    public LocalDate arrivalDate;
    public int lengthOfStay;
}
