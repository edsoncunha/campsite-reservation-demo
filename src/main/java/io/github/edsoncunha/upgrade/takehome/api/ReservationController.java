package io.github.edsoncunha.upgrade.takehome.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    @GetMapping("/availability")
    @Operation(summary = "Returns a boolean indicating whether a reservation would be available for a given arrival date and length of stay")
    @ApiResponses(
            value = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))})}
    )
    public boolean getAvailability(
            @Parameter(description = "Arrival date")
            @RequestParam LocalDate arrivalDate,

            @Parameter(description = "Length of stay (in days)")
            @RequestParam int lengthOfStay

//            Authentication authentication
            ) {
        return false;
    }
}
