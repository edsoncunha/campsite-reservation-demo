package io.github.edsoncunha.upgrade.takehome.api;

import io.github.edsoncunha.upgrade.takehome.api.responses.ApiCallError;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationConstraintException;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

@ControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiCallError<String>> handleInternalServerError(HttpServletRequest request, Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiCallError<>("Internal server error", Collections.singletonList(ex.getMessage())));
    }

    @ExceptionHandler(ReservationConstraintException.class)
    public ResponseEntity<ApiCallError<String>> handleReservationConstraintException(HttpServletRequest request, Exception ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiCallError<>("Invalid request", Collections.singletonList(ex.getMessage())));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiCallError<String>> handleEntityNotFoundException(HttpServletRequest request, Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiCallError<>("Reservation not found", Collections.emptyList()));
    }
}
