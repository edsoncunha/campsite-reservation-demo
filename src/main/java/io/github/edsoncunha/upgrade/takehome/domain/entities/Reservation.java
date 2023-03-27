package io.github.edsoncunha.upgrade.takehome.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity(name = "reservation")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "email")
    private String email;

    @Column(name = "checkin")
    private ZonedDateTime checkin;

    @Column(name = "checkout")
    private ZonedDateTime checkout;

    @Column(name = "canceled")
    private Boolean canceled;

    public boolean isActiveAt(LocalDate candidateDate) {
        return candidateDate.toEpochDay() >= checkin.toLocalDate().toEpochDay() &&
                candidateDate.toEpochDay() < checkout.toLocalDate().toEpochDay();
    }
}
