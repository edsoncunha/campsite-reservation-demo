package io.github.edsoncunha.upgrade.takehome.domain.repositories;

import io.github.edsoncunha.upgrade.takehome.domain.entities.Reservation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends CrudRepository<Reservation, Long> {
}
