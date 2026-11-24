package pl.travelapi.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.travelapi.trip.domain.Waypoint;

public interface WaypointRepository extends JpaRepository<Waypoint, Long> {
}
