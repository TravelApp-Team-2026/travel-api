package pl.travelapi.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.travelapi.trip.domain.TripSegment;

public interface TripSegmentRepository extends JpaRepository<TripSegment, Long> {
}
