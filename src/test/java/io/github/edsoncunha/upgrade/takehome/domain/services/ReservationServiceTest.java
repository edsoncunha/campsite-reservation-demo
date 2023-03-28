package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationNotFoundException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ReservationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private Clock clockMock;
    @Mock
    private ReservationRepository repositoryMock;
    @Mock
    private LockManager lockManagerMock;

    private ReservationService.ReservationServiceBuilder serviceBuilderFor(List<ReservationRule> reservationRules) {
        return ReservationService.builder()
                .clock(clockMock)
                .reservationRules(reservationRules)
                .repository(repositoryMock)
                .lockManager(lockManagerMock);
    }

    private LocalDateTime january(int day, int year) {
        return LocalDate.of(year, 1, day).atStartOfDay();
    }

    @Nested
    @DisplayName("Reservation")
    class ReservationTests {
        @Test
        @DisplayName("A reservation is successful if there is availability in campsite")
        public void reservationSuccessfulIfThereIsAvailability() {
            List<ReservationRule> noRules = Collections.emptyList();
            // just runs the operation it gets
            LockManager lockManagerMock = (id, timeout, operation) -> operation.run();

            ReservationService service = serviceBuilderFor(noRules)
                    .lockManager(lockManagerMock)
                    .repository(repositoryMock)
                    .campsiteCapacity(1)
                    .build();

            // just returning what it gets
            AtomicReference<Reservation> reservationSentToDatabase = new AtomicReference<>();
            when(repositoryMock.save(any())).thenAnswer((invocation) -> {
                Reservation reservation = invocation.getArgument(0, Reservation.class);
                reservationSentToDatabase.set(reservation);
                return reservation;
            });

//            // mimic clock
//            when(clockMock.asZonedDateTime(any())).thenAnswer((invocation) -> {
//                LocalDate date = invocation.getArgument(0, LocalDate.class);
//                return zonedAtUtc(date);
//            });

            // when
            String email = "dummy@test.com";
            LocalDate arrivalDate = LocalDate.now().plusDays(1);
            int lengthOfStay = 1;

            Reservation returnedReservation = service.reserve(email, arrivalDate, lengthOfStay);

            // then
            Mockito.verify(repositoryMock).save(any());

            assertThat(reservationSentToDatabase.get()).isNotNull();
            assertThat(reservationSentToDatabase.get().getEmail()).isEqualTo(email);
            assertThat(reservationSentToDatabase.get().getCheckin()).isEqualTo(arrivalDate.atStartOfDay());
            assertThat(reservationSentToDatabase.get().getCheckout()).isEqualTo(arrivalDate.plusDays(lengthOfStay).atStartOfDay());

            assertThat(returnedReservation).isNotNull();
        }

        @Test
        @DisplayName("A reservation must fail if campsite is full")
        public void reservationShouldFailIfOccupationIsFull() {
            List<ReservationRule> noRules = Collections.emptyList();
            // just runs the operation it gets
            LockManager lockManagerMock = (id, timeout, operation) -> operation.run();

            ReservationService service = serviceBuilderFor(noRules)
                    .lockManager(lockManagerMock)
                    .repository(repositoryMock)
                    .campsiteCapacity(1)
                    .build();

            String email = "dummy@test.com";
            LocalDate arrivalDate = LocalDate.now().plusDays(1);
            int lengthOfStay = 1;

            // emulate an existing reservation with same parameters
            when(repositoryMock.getReservationsInPeriod(any(), any())).thenReturn(
                Collections.singletonList (
                    Reservation.builder()
                            .id(1)
                            .checkin(arrivalDate.atStartOfDay())
                            .checkout(arrivalDate.plusDays(lengthOfStay).atStartOfDay())
                            .build()
                )
            );

            // when
            assertThrows(NoPlacesAvailableException.class, () -> service.reserve(email, arrivalDate, lengthOfStay));

            // then
            Mockito.verify(repositoryMock, times(0)).save(any());
        }

    }

    @Nested
    @DisplayName("Reservation validation rules")
    class ReservationValidations {
        @Test
        @DisplayName("If none fails, validations are performed one by one")
        public void allValidationsShouldBePerformedWhenAllAreSuccessful() {
            ArrayList<ReservationRule> reservationRules = new ArrayList<>();

            ReservationRule rule1 = mock(ReservationRule.class);
            ReservationRule rule2 = mock(ReservationRule.class);

            reservationRules.add(rule1);
            reservationRules.add(rule2);

            ReservationService service = serviceBuilderFor(reservationRules)
                    .campsiteCapacity(0)
                    .build();

            String email = "dummy@mail.com";
            LocalDate arrivalDate = LocalDate.now();
            int lengthOfStay = 2;

            // skip the process of reserving, for simplicity
            when(repositoryMock.getReservationsInPeriod(any(), any())).thenReturn(Collections.emptyList());
            assertThrows(NoPlacesAvailableException.class, () -> service.reserve(email, arrivalDate, lengthOfStay));

            verify(rule1).validate(email, arrivalDate, lengthOfStay);
            verify(rule2).validate(email, arrivalDate, lengthOfStay);
        }

        @Test
        @DisplayName("If a validation throws an exception, validation chain is interrupted")
        public void validationsAreInterruptedWheneverSomeRuleFail() {
            ArrayList<ReservationRule> reservationRules = new ArrayList<>();

            ReservationRule rule1 = mock(ReservationRule.class);
            ReservationRule rule2 = mock(ReservationRule.class);

            reservationRules.add(rule1);
            reservationRules.add(rule2);

            ReservationService service = serviceBuilderFor(reservationRules).build();

            String email = "dummy@mail.com";
            LocalDate arrivalDate = LocalDate.now();
            int lengthOfStay = 2;

            doThrow(new NoPlacesAvailableException()).when(rule1).validate(email, arrivalDate, lengthOfStay);

            assertThrows(NoPlacesAvailableException.class, () -> service.reserve(email, arrivalDate, lengthOfStay));

            verify(rule1).validate(email, arrivalDate, lengthOfStay);
            verify(rule2, times(0)).validate(email, arrivalDate, lengthOfStay);
        }
    }

    // testar se reserva não é feita se todas acomodações estiverem ocupadas no período

    @Nested
    @DisplayName("Reservation availability")
    class ReservationAvailabilityTests {
        @Test
        @DisplayName("Availability is correctly calculated even with fragmented space allocation")
        public void availableDatesAreCalculatedCorrectly() {
            List<ReservationRule> noReservationRules = Collections.emptyList();

            ReservationService service = serviceBuilderFor(noReservationRules)
                    .campsiteCapacity(3)
                    .build();

            LocalDate searchStartDate = LocalDate.parse("2023-01-01");
            LocalDate searchEndDate = LocalDate.parse("2023-01-04");

            ArrayList<Reservation> reservations = new ArrayList<>();

            reservations.add(Reservation.builder().id(1)
                    .checkin(january(1, 2023))
                    .checkout(january(2, 2023)).build());

            reservations.add(Reservation.builder().id(2)
                    .checkin(january(3, 2023))
                    .checkout(january(4, 2023)).build());

            reservations.add(Reservation.builder().id(3)
                    .checkin(january(2, 2023))
                    .checkout(january(3, 2023)).build());

            reservations.add(Reservation.builder().id(4)
                    .checkin(january(3, 2023))
                    .checkout(january(4, 2023)).build());

            reservations.add(Reservation.builder().id(5)
                    .checkin(january(1, 2023))
                    .checkout(january(4, 2023)).build());

            when(repositoryMock.getReservationsInPeriod(any(), any())).thenReturn(reservations);

            List<LocalDate> availableDates = service.getAvailableDates(searchStartDate, searchEndDate);

            assertThat(availableDates).containsExactlyInAnyOrder(
                    january(1, 2023).toLocalDate(),
                    january(2, 2023).toLocalDate(),
                    january(4, 2023).toLocalDate());
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class CancellationTests {
        @Test
        @DisplayName("A reservation is properly canceled if it is found on database")
        public void reservationIsCancelledIfFoundOnDatabase() {
            List<ReservationRule> noReservationRules = Collections.emptyList();

            ReservationService service = serviceBuilderFor(noReservationRules).build();

            long reservationId = 10;
            Reservation reservationMock = mock(Reservation.class);

            when(repositoryMock.findById(reservationId)).thenReturn(Optional.of(reservationMock));

            service.cancelReservation(reservationId);

            verify(reservationMock).setCanceled(true);
            verify(repositoryMock).save(reservationMock);
        }

        @Test
        @DisplayName("An exception is thrown if reservation is not found by id on database")
        public void exceptionIsThrownIfReservationToBeCancelledIsNotFoundOnDatabase() {
            List<ReservationRule> noReservationRules = Collections.emptyList();

            ReservationService service = serviceBuilderFor(noReservationRules).build();

            long reservationId = 10;

            when(repositoryMock.findById(reservationId)).thenReturn(Optional.empty());

            assertThrows(ReservationNotFoundException.class, () -> service.cancelReservation(reservationId));

            verify(repositoryMock, times(0)).save(any());
        }
    }
}
