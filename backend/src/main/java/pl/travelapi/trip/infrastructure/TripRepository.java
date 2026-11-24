package pl.travelapi.trip.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.TripSummary;

public interface TripRepository extends JpaRepository<Trip, Long> {

	Optional<Trip> findByIdAndUserId(Long id, Long userId);

	/** Newest first; the id breaks ties between trips that start on the same day. */
	@Query("""
			select new pl.travelapi.trip.domain.TripSummary(
				t.id, t.title, t.startDate, t.endDate, size(t.waypoints), t.totalDistanceKm)
			from Trip t
			where t.user.id = :userId
			order by t.startDate desc, t.id desc
			""")
	List<TripSummary> findSummariesByUserId(@Param("userId") Long userId);
}
