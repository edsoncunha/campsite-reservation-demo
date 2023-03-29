package io.github.edsoncunha.upgrade.takehome.integrationtests;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.ReservationService;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@ExtendWith(PostgresContainerExtension.class)
@ActiveProfiles("it")
@DirtiesContext
public class ReservationIT {
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("A single reservation should take place gracefully")
    public void singleReservationScenario() {
        Reservation reserved = reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(1), 1);

        Optional<Reservation> fromDatabase = reservationRepository.findById(reserved.getId());

        assertThat(fromDatabase.isPresent()).isTrue();
        assertThat(fromDatabase.get().getId()).isEqualTo(reserved.getId());
    }

    @Test
    @DisplayName("It should perform a reservation concurrently")
    public void concurrentReservationTest() throws InterruptedException {
        reservationService.setCapacity(1);

        int threads = 200;

        doConcurrenctly(threads, s -> reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(1), 1));

        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    public void reservationsRespectLimitsOfCampsite() {
        reservationService.setCapacity(1);

        reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(3), 1);

        assertThrows(NoPlacesAvailableException.class, () -> {
            reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(3), 1);
        });
    }

    @Test
    public void reservationCanBePostponed() {
        reservationService.setCapacity(1);

        LocalDate arrivalDate = LocalDate.now().plusDays(3);
        int lengthOfStay = 1;

        Reservation reservation = reservationService.reserve("simple@mail.com", arrivalDate, lengthOfStay);

        LocalDate newArrivalDate = arrivalDate.plusDays(1);

        Reservation updated = reservationService.updateReservation(reservation.getId(), newArrivalDate, lengthOfStay);

        assertThat(updated.getId()).isEqualTo(reservation.getId());
        assertThat(updated.getCheckin().toLocalDate().toEpochDay()).isEqualTo(newArrivalDate.toEpochDay());
        assertThat(updated.getCheckout().toLocalDate().toEpochDay()).isEqualTo(newArrivalDate.plusDays(lengthOfStay).toEpochDay());
    }

    @Test
    public void reservationCanBeProlonged() {
        reservationService.setCapacity(1);

        LocalDate arrivalDate = LocalDate.now().plusDays(3);
        int lengthOfStay = 1;

        Reservation reservation = reservationService.reserve("simple@mail.com", arrivalDate, lengthOfStay);

        int newLengthOfStay = 3;

        Reservation updated = reservationService.updateReservation(reservation.getId(), arrivalDate, newLengthOfStay);

        assertThat(updated.getId()).isEqualTo(reservation.getId());
        assertThat(updated.getCheckin().toLocalDate().toEpochDay()).isEqualTo(arrivalDate.toEpochDay());
        assertThat(updated.getCheckout().toLocalDate().toEpochDay()).isEqualTo(arrivalDate.plusDays(newLengthOfStay).toEpochDay());
    }

    protected void doConcurrenctly(int threads, Consumer<String> operation) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            String threadName = "Thread-" + i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    operation.accept(threadName);
                } catch (Exception e) {
                    System.out.println(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await();
    }
}
