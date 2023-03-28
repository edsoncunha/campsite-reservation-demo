package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationNotFoundException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ReservationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    private final List<ReservationRule> reservationRules;
    private final LockManager lockManager;


    public ReservationService(@Value("${campsite.capacity}") int capacity, Clock clock, ReservationRepository repository, List<ReservationRule> reservationRules, LockManager lockManager) {
        this.campsiteCapacity = capacity;
        this.clock = clock;
        this.repository = repository;
        this.reservationRules = reservationRules;
        this.lockManager = lockManager;
    }

    @Transactional
    public Reservation reserve(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        reservationRules.forEach(rule -> rule.validate(userEmail, arrivalDate, lengthOfStay));

        // double-check locking
        // TODO: evict cache or force uncached read before persisting to database

        Duration timeout = Duration.ofSeconds(3);

        return lockManager.lock(13, timeout, () -> {
            if (isReservable(arrivalDate, lengthOfStay)) {

                return repository.save(
                        Reservation.builder()
                                .email(userEmail)
                                .checkin(arrivalDate.atStartOfDay())
                                .checkout(arrivalDate.plusDays(lengthOfStay).atStartOfDay())
                                .build()
                );

            }

            throw new NoPlacesAvailableException();
        });
    }

    private Boolean isReservable(LocalDate arrivalDate, int lengthOfStay) {
        ArrayList<LocalDate> accommodationDates = new ArrayList<>();
        for (int i = 0; i < lengthOfStay; i++) {
            accommodationDates.add(arrivalDate.plusDays(i));
        }

        List<LocalDate> availableDates = getAvailableDates(arrivalDate, last(accommodationDates));

        return new HashSet<>(availableDates).containsAll(accommodationDates);
    }

    //TODO: enable caching to speed up reading.
    public List<LocalDate> getAvailableDates(LocalDate firstDayOfAccommodation, LocalDate lastDayOfAccommodation) {
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

    public void cancelReservation(long reservationId) {
        Reservation reservation = repository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        reservation.setCanceled(true);

        repository.save(reservation);
    }

    private <T> T last(ArrayList<T> list) {
        return list.get(list.size() - 1);
    }


    public void setCapacity(int newCapacity) {
        this.campsiteCapacity = newCapacity;
    }
}
