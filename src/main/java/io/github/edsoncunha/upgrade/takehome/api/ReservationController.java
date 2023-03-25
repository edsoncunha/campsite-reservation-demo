package io.github.edsoncunha.upgrade.takehome.api;

import io.github.edsoncunha.upgrade.takehome.api.requests.ReservationRequest;
import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.services.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {
    @Autowired
    private ReservationService reservationService;

    @GetMapping("/availability")
    @Operation(summary = "Returns a boolean indicating whether a reservation would be available for a given arrival date and length of stay")
    @ApiResponses(
            value = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))})}
    )
    public List<LocalDate> getAvailability(
            @Parameter(description = "Arrival date")
            @RequestParam LocalDate arrivalDate,

            @Parameter(description = "Length of stay (in days)")
            @RequestParam int lengthOfStay
    ) {
        LocalDate lastDayOfAccommodation = arrivalDate.plusDays(lengthOfStay - 1);
        return reservationService.getAvailableDates(arrivalDate, lastDayOfAccommodation);
    }

    @PostMapping
    public ResponseEntity<Reservation> submitReservation(ReservationRequest request) {
        Reservation reservation = reservationService.reserve(request.email, request.arrivalDate, request.lengthOfStay);

        return ResponseEntity
                .created(URI.create("/reservations/" + reservation.getId()))
                .body(reservation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable @NotNull long reservationId) {
        reservationService.cancelReservation(reservationId);

        return ResponseEntity.ok().build();
    }
}
