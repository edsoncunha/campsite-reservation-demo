package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ValidationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Builder
@Service
public class ReservationService {
    private int campsiteCapacity;
    private final Clock clock;
    private final ReservationRepository repository;
    private final List<ValidationRule> validationRules;
    private final LockManager lockManager;

    public ReservationService(@Value("${campsite.capacity}") int capacity, Clock clock, ReservationRepository repository, List<ValidationRule> validationRules, LockManager lockManager) {
        this.campsiteCapacity = capacity;
        this.clock = clock;
        this.repository = repository;
        this.validationRules = validationRules;
        this.lockManager = lockManager;
    }

    @Transactional
    public Reservation reserve(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        validationRules.forEach(rule -> rule.validate(userEmail, arrivalDate, lengthOfStay));

        AtomicReference<Reservation> savedReservation = new AtomicReference<>();

        int lockId = this.hashCode();
        Duration starvationTimeout = Duration.ofSeconds(3);

        if (isReservable(arrivalDate, lengthOfStay)) {
            lockManager.lock(lockId, starvationTimeout, () -> {
                // double-check locking
                if (isReservable(arrivalDate, lengthOfStay)) {
                    savedReservation.set(repository.save(Reservation.builder()
                            .checkin(clock.asZonedDateTime(arrivalDate))
                            .checkout(clock.asZonedDateTime(arrivalDate.plusDays(lengthOfStay)))
                            .build()));
                }
            });
        }

        if (savedReservation.get() == null) {
            throw new NoPlacesAvailableException();
        }

        return savedReservation.get();
    }

    private Boolean isReservable(LocalDate arrivalDate, int lengthOfStay) {
        ArrayList<LocalDate> accommodationDates = new ArrayList<>();
        for (int i=0; i<lengthOfStay; i++) {
            accommodationDates.add(arrivalDate.plusDays(i));
        }

        List<LocalDate> availableDates = getAvailableDates(arrivalDate, last(accommodationDates));

        return new HashSet<>(availableDates).containsAll(accommodationDates);
    }

    public List<LocalDate> getAvailableDates(LocalDate firstDayOfAccommodation, LocalDate lastDayOfAccommodation) {
        if (firstDayOfAccommodation.toEpochDay() >= lastDayOfAccommodation.toEpochDay()) {
            throw new IllegalArgumentException("Checkout must be greater than check-in date");
        }

        List<Reservation> currentReservations = repository.getReservationsInPeriod(firstDayOfAccommodation, lastDayOfAccommodation);

        // the island capacity is small, so it's fine to count occupation per day on the application side
        long daysSpan = ChronoUnit.DAYS.between(firstDayOfAccommodation, lastDayOfAccommodation);

        ArrayList<LocalDate> availableDates = new ArrayList<>();

        for (int i = 0; i <= daysSpan; i++) {
            LocalDate candidateDate = firstDayOfAccommodation.plusDays(i);

            List<Reservation> activeReservationsAtCandidateDate = currentReservations.stream().filter(reservation -> reservation.isActiveAt(candidateDate)).toList();
            long occupationAtCandidateDate = activeReservationsAtCandidateDate.size();

            if (occupationAtCandidateDate < campsiteCapacity) {
                availableDates.add(candidateDate);
            }
        }

        return availableDates;
    }

    private <T> T last(ArrayList<T> list) {
        return list.get(list.size() - 1);
    }
}
