package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ValidationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Test
    public void allValidationsShouldBePerformedWhenAllAreSuccessful() {
        ArrayList<ValidationRule> validationRules = new ArrayList<>();

        ValidationRule rule1 = mock(ValidationRule.class);
        ValidationRule rule2 = mock(ValidationRule.class);

        validationRules.add(rule1);
        validationRules.add(rule2);

        ReservationService service = ReservationService.builder()
                .clock(clockMock)
                .validationRules(validationRules)
                .repository(repositoryMock)
                .lockManager(lockManagerMock)
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
    public void validationsAreInterruptedWheneverSomeRuleFail() {
        ArrayList<ValidationRule> validationRules = new ArrayList<>();

        ValidationRule rule1 = mock(ValidationRule.class);
        ValidationRule rule2 = mock(ValidationRule.class);

        validationRules.add(rule1);
        validationRules.add(rule2);

        ReservationService service = ReservationService.builder()
                .clock(clockMock)
                .validationRules(validationRules)
                .repository(repositoryMock)
                .lockManager(lockManagerMock)
                .build();

        String email = "dummy@mail.com";
        LocalDate arrivalDate = LocalDate.now();
        int lengthOfStay = 2;

        doThrow(new NoPlacesAvailableException()).when(rule1).validate(email, arrivalDate, lengthOfStay);

        assertThrows(NoPlacesAvailableException.class, () -> service.reserve(email, arrivalDate, lengthOfStay));

        verify(rule1).validate(email, arrivalDate, lengthOfStay);
        verify(rule2, times(0)).validate(email, arrivalDate, lengthOfStay);
    }

    @Test
    public void availableDatesAreCalculatedCorrectly() {
        ReservationService service = ReservationService.builder()
                .clock(clockMock)
                .validationRules(Collections.emptyList())
                .repository(repositoryMock)
                .lockManager(lockManagerMock)
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

        when(repositoryMock.getReservationsInPeriod(searchStartDate, searchEndDate)).thenReturn(reservations);

        List<LocalDate> availableDates = service.getAvailableDates(searchStartDate, searchEndDate);

        assertThat(availableDates).containsExactlyInAnyOrder(
                january(1, 2023).toLocalDate(),
                january(2, 2023).toLocalDate(),
                january(4, 2023).toLocalDate());
    }

    private ZonedDateTime january(int day, int year) {
        return ZonedDateTime.of(LocalDate.of(year, 1, day), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
    }
}