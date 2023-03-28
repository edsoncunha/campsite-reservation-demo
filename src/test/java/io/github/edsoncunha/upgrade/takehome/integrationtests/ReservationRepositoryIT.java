package io.github.edsoncunha.upgrade.takehome.integrationtests;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.support.PostgresContainerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(PostgresContainerExtension.class)
@ActiveProfiles("it")
@DirtiesContext
class ReservationRepositoryIT {

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    public void setup() {
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("It should correctly retrieve a saved reservation")
    public void itShouldRetrieveASavedReservation() {
        LocalDate beginningOfStay = LocalDate.of(2001, 1, 1);
        LocalDate checkoutDate = beginningOfStay.plusDays(1);

        Reservation persisted = reservationRepository.save(
                Reservation.builder()
                        .email("some@mail.com")
                        .checkin(beginningOfStay.atStartOfDay())
                        .checkout(checkoutDate.atStartOfDay())
                        .build()
        );

        List<Reservation> retrieved = reservationRepository.getReservationsInPeriod(beginningOfStay, beginningOfStay);
        assertThat(retrieved).contains(persisted);
    }

    @Test
    @DisplayName("It should not retrieve a reservation when searching for availability on its checkout day")
    public void itShouldNotRetrieveReservationOnItsCheckoutDay() {
        LocalDate beginningOfStay = LocalDate.of(2001, 1, 1);
        LocalDate checkoutDate = beginningOfStay.plusDays(1);

        reservationRepository.save(
                Reservation.builder()
                        .email("some@mail.com")
                        .checkin(beginningOfStay.atStartOfDay())
                        .checkout(checkoutDate.atStartOfDay())
                        .build()
        );

        List<Reservation> retrieved = reservationRepository.getReservationsInPeriod(checkoutDate, checkoutDate);
        assertThat(retrieved).isEmpty();
    }
}