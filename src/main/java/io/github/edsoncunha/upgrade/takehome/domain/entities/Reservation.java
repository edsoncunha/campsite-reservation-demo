package io.github.edsoncunha.upgrade.takehome.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity(name = "reservation")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "email")
    private String email;

    @Column(name = "checkin")
    private LocalDateTime checkin;

    @Column(name = "checkout")
    private LocalDateTime checkout;

    @Column(name = "canceled", nullable = false)
    @Builder.Default
    private Boolean canceled = false;

    public boolean isActiveAt(LocalDate candidateDate) {
        return candidateDate.toEpochDay() >= checkin.toLocalDate().toEpochDay() &&
                candidateDate.toEpochDay() < checkout.toLocalDate().toEpochDay();
    }
}
