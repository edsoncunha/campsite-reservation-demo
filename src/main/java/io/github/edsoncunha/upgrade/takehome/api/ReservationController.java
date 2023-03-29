package io.github.edsoncunha.upgrade.takehome.api;

import io.github.edsoncunha.upgrade.takehome.api.requests.ReservationRequest;
import io.github.edsoncunha.upgrade.takehome.api.requests.UpdateReservationRequest;
import io.github.edsoncunha.upgrade.takehome.api.responses.ApiCallError;
import io.github.edsoncunha.upgrade.takehome.api.swagger.types.ListOfLocalDate;
import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.services.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
                    schema = @Schema(implementation = ListOfLocalDate.class))})}
    )
    public ResponseEntity<List<LocalDate>> getAvailability(
            @Parameter(description = "First day of availability search")
            @RequestParam LocalDate startDate,

            @Parameter(description = "Last day of availability search")
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reservationService.getAvailableDates(startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "Submits a reservation")
    @ApiResponses(
            value = {@ApiResponse(responseCode = "201", description = "Created successfully", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))}),
                    @ApiResponse(responseCode = "409", description = "Reservation was not successful due to a rule violation", content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiCallError.class))})
            }
    )
    public ResponseEntity<Reservation> submitReservation(ReservationRequest request) {
        Reservation reservation = reservationService.reserve(request.email, request.arrivalDate, request.lengthOfStay);

        return ResponseEntity
                .created(URI.create("/reservations/" + reservation.getId()))
                .body(reservation);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a reservation")
    @ApiResponses(
            value = {@ApiResponse(responseCode = "200", description = "Updated successfully", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Reservation.class), examples = @ExampleObject(value= """
                    {
                      "id": 29,
                      "email": "string",
                      "checkin": "2023-03-29T02:00:00.000",
                      "checkout": "2023-03-31T00:00:00.000",
                      "canceled": false
                    }"""))}),
                    @ApiResponse(responseCode = "409", description = "Reservation could not be updated due to a rule violation",
                            content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiCallError.class))})
            }
    )
    public ResponseEntity<Reservation> updateReservation(@Parameter(description = "Reservation id", required = true) @PathVariable("id") @NotNull long id,  @RequestBody UpdateReservationRequest request) {
        Reservation updatedReservation = reservationService.updateReservation(id, request.arrivalDate, request.lengthOfStay);

        return ResponseEntity.ok(updatedReservation);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancels a reservation")
    @ApiResponses(
            value = {@ApiResponse(responseCode = "200", description = "Canceled successfully", content = {@Content(mediaType = "application/json")})}
    )
    public ResponseEntity<Void> cancelReservation(@Parameter(description = "Reservation id", required = true) @PathVariable("id") @NotNull long id) {
        reservationService.cancelReservation(id);

        return ResponseEntity.ok().build();
    }
}
