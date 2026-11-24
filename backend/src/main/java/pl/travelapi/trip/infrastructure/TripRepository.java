package pl.travelapi.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.travelapi.trip.domain.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
