package io.github.edsoncunha.upgrade.takehome.integrationtests;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.ReservationService;
import io.github.edsoncunha.upgrade.takehome.support.PostgresContainerExtension;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
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
//    static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:12.5-alpine")
//            .withUsername("test-user")
//            .withPassword("test-pwd")
//            .withDatabaseName("campingsite-demo")
//            .waitingFor(Wait.forListeningPort());
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ReservationService reservationService;

//    @DynamicPropertySource
//    static void properties(DynamicPropertyRegistry registry) {
//        container.start();
//
//        registry.add("spring.datasource.url", container::getJdbcUrl);
//        registry.add("spring.datasource.password=", container::getPassword);
//        registry.add("spring.datasource.username=", container::getUsername);
//    }

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
        doConcurrenctly(s -> reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(1), 1));

        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    public void reservationsRespectLimitsOfCampsite() {
        reservationService.setCapacity(1);

        Reservation r1 = reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(3), 1);

        assertThat(r1.getId()).isGreaterThan(0L);

        assertThrows(NoPlacesAvailableException.class, () -> {
            reservationService.reserve("simple@mail.com", LocalDate.now().plusDays(3), 1);
        }) ;
    }

    protected void doConcurrenctly(Consumer<String> operation) throws InterruptedException {
        int threads = 20;

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
