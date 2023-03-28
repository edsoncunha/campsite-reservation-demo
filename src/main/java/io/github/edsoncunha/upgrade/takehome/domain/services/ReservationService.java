package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.NoPlacesAvailableException;
import io.github.edsoncunha.upgrade.takehome.domain.exceptions.ReservationNotFoundException;
import io.github.edsoncunha.upgrade.takehome.domain.repositories.ReservationRepository;
import io.github.edsoncunha.upgrade.takehome.domain.services.validation.ReservationRule;
import io.github.edsoncunha.upgrade.takehome.etc.Clock;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Builder
@Service
public class ReservationService {
    public static final String AVAILABILITY_SEARCH_CACHE_NAME = "availability";

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

        Duration timeout = Duration.ofSeconds(3);

        if (isReservable(arrivalDate, lengthOfStay)) {
            return lockManager.lock(13, timeout, () -> {
                // double-check locking
                if (isReservable(arrivalDate, lengthOfStay)) {
                    return doSaveReservation(userEmail, arrivalDate, lengthOfStay);
                }

                throw new NoPlacesAvailableException();
            });
        }

        throw new NoPlacesAvailableException();
    }

    @CacheEvict(value = AVAILABILITY_SEARCH_CACHE_NAME, allEntries = true) /* evicts all keys for the sake of simplicity.
     In a more realistic scenario, it would be required to evict availability information only for {start, end} cache keys that have some overlapping
     with the reservation we have just created */
    private Reservation doSaveReservation(String userEmail, LocalDate arrivalDate, int lengthOfStay) {
        return repository.save(
                Reservation.builder()
                        .email(userEmail)
                        .checkin(arrivalDate.atStartOfDay())
                        .checkout(arrivalDate.plusDays(lengthOfStay).atStartOfDay())
                        .build()
        );
    }

    @Cacheable(value = AVAILABILITY_SEARCH_CACHE_NAME, key = "'#startDate__#endDate'")
    public List<LocalDate> getAvailableDates(LocalDate firstDayOfAccommodation, LocalDate lastDayOfAccommodation) {
        firstDayOfAccommodation = ensureFutureDate(firstDayOfAccommodation);

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

    private Boolean isReservable(LocalDate arrivalDate, int lengthOfStay) {
        ArrayList<LocalDate> accommodationDates = new ArrayList<>();
        for (int i = 0; i < lengthOfStay; i++) {
            accommodationDates.add(arrivalDate.plusDays(i));
        }

        List<LocalDate> availableDates = getAvailableDates(arrivalDate, last(accommodationDates));

        return new HashSet<>(availableDates).containsAll(accommodationDates);
    }

    private LocalDate ensureFutureDate(LocalDate firstDayOfAccommodation) {
        if (firstDayOfAccommodation.toEpochDay() <= clock.campsiteDateTime().toLocalDate().toEpochDay()) {
            firstDayOfAccommodation = clock.campsiteDateTime().toLocalDate();
        }
        return firstDayOfAccommodation;
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
