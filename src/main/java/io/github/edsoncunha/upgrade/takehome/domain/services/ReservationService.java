package io.github.edsoncunha.upgrade.takehome.domain.services;


import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationNotFoundException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ReservationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static  io.github.edsoncunha.upgrade.takehome.support.ListOperations.last;

@Builder
@Service
@Slf4j
public class ReservationService {
    public static final String AVAILABILITY_SEARCH_CACHE_NAME = "availability";
    public static final int LOCK_ID = 13;

    private int campsiteCapacity;
    private final Clock clock;
    private final ReservationRepository repository;
    private final List<ReservationRule> reservationRules;
    private final LockManager lockManager;

    private final CacheManager cacheManager;


    public ReservationService(@Value("${campsite.capacity}") int capacity, Clock clock, ReservationRepository repository, List<ReservationRule> reservationRules, LockManager lockManager, CacheManager cacheManager) {
        this.campsiteCapacity = capacity;
        this.clock = clock;
        this.repository = repository;
        this.reservationRules = reservationRules;
        this.lockManager = lockManager;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public Reservation reserve(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        reservationRules.forEach(rule -> rule.validate(userEmail, arrivalDate, lengthOfStay));

        Duration timeout = Duration.ofSeconds(3);

        if (isReservable(arrivalDate, lengthOfStay)) {
            return lockManager.lock(LOCK_ID, timeout, () -> {

                clearAvailabilityCache();

                // double-check locking
                if (isReservable(arrivalDate, lengthOfStay)) {
                    return doSaveReservation(userEmail, arrivalDate, lengthOfStay);
                }

                throw new NoPlacesAvailableException();
            });
        }

        throw new NoPlacesAvailableException();
    }

    public Reservation updateReservation(long id, LocalDate newArrivalDate, int lengthOfStay) {
        Reservation reservationToBeUpdated = repository.findById(id)
                .orElseThrow(ReservationNotFoundException::new);

        reservationRules.forEach(rule -> rule.validate(reservationToBeUpdated.getEmail(), newArrivalDate, lengthOfStay));

        Duration timeout = Duration.ofSeconds(3);

        if (isReservable(newArrivalDate, lengthOfStay, reservationToBeUpdated)) {
            return lockManager.lock(LOCK_ID, timeout, () -> {

                clearAvailabilityCache();

                // double-check locking
                if (isReservable(newArrivalDate, lengthOfStay, reservationToBeUpdated)) {
                    return doUpdateReservation(reservationToBeUpdated, newArrivalDate, lengthOfStay);
                }

                throw new NoPlacesAvailableException();
            });
        }

        throw new NoPlacesAvailableException();
    }

    public void cancelReservation(long reservationId) {
        Reservation reservation = repository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);

        reservation.setCanceled(true);

        repository.save(reservation);
    }

    @Cacheable(value = AVAILABILITY_SEARCH_CACHE_NAME)
    public List<LocalDate> getAvailableDates(LocalDate firstDayOfAccommodation, LocalDate lastDayOfAccommodation) {
        return getAvailableDates(firstDayOfAccommodation, lastDayOfAccommodation, null);
    }

    public List<LocalDate> getAvailableDates(LocalDate firstDayOfAccommodation, LocalDate lastDayOfAccommodation, Reservation reservationToBeUpdated) {
        log.info("Searching availability between  {} and {}", firstDayOfAccommodation, lastDayOfAccommodation);

        firstDayOfAccommodation = ensureFutureDate(firstDayOfAccommodation);

        List<Reservation> currentReservations = repository.getReservationsInPeriod(firstDayOfAccommodation, lastDayOfAccommodation);

        if (reservationToBeUpdated != null) {
            currentReservations = currentReservations.stream().filter( reservation -> reservation != reservationToBeUpdated).toList();
        }

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

    private Reservation doSaveReservation(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        return repository.save(
                Reservation.builder()
                        .email(userEmail)
                        .checkin(arrivalDate.atStartOfDay())
                        .checkout(arrivalDate.plusDays(lengthOfStay).atStartOfDay())
                        .build()
        );
    }

    private Reservation doUpdateReservation(Reservation reservationToBeUpdated, LocalDate newArrivalDate, int lengthOfStay) {
        reservationToBeUpdated.setCheckin(newArrivalDate.atStartOfDay());
        reservationToBeUpdated.setCheckout(newArrivalDate.plusDays(lengthOfStay).atStartOfDay());

        return repository.save(reservationToBeUpdated);
    }

    private void clearAvailabilityCache() {
        // Clears cache synchronously to avoid eventual consistency during double-checking inside a critical section
        // It evicts all keys for the sake of simplicity.
        // In a more realistic scenario, it would be required to evict availability information only for {start, end} cache keys
        // that overlap the reservation we have just created
        Cache availabilityCache = cacheManager.getCache(AVAILABILITY_SEARCH_CACHE_NAME);
        if (availabilityCache != null) {
            availabilityCache.invalidate();
        }
    }

    private Boolean isReservable(LocalDate arrivalDate, int lengthOfStay) {
        return isReservable(arrivalDate, lengthOfStay, null);
    }

    private Boolean isReservable(LocalDate arrivalDate, int lengthOfStay, Reservation reservationToBeUpdated) {
        ArrayList<LocalDate> accommodationDates = new ArrayList<>();
        for (int i = 0; i < lengthOfStay; i++) {
            accommodationDates.add(arrivalDate.plusDays(i));
        }

        List<LocalDate> availableDates = getAvailableDates(arrivalDate, last(accommodationDates), reservationToBeUpdated);

        return new HashSet<>(availableDates).containsAll(accommodationDates);
    }

    private LocalDate ensureFutureDate(LocalDate firstDayOfAccommodation) {
        if (firstDayOfAccommodation.toEpochDay() <= clock.now().toLocalDate().toEpochDay()) {
            firstDayOfAccommodation = clock.now().toLocalDate();
        }
        return firstDayOfAccommodation;
    }

    // just a convenience for integration tests :)
    public void setCapacity(int newCapacity) {
        this.campsiteCapacity = newCapacity;
    }
}
