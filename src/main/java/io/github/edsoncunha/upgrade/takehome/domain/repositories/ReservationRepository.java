package io.github.edsoncunha.upgrade.takehome.domain.repositories;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends CrudRepository<Reservation, Long> {
    @Query(value = "select * from reservation where checkin >= :firstDayOfAccommodation and checkout < :lastDayOfAccommodation and canceled = false", nativeQuery = true)
    List<Reservation> getReservationsInPeriod(@Param("firstDayOfAccommodation") LocalDate firstDayOfAccommodation, @Param("lastDayOfAccommodation") LocalDate lastDayOfAccommodation);
}
