package pl.travelapi.country.infrastructure;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import pl.travelapi.country.domain.VisitedCountry;
import pl.travelapi.trip.domain.Waypoint;

/**
 * Visited countries aggregated in the database. A country counts when a trip has a stop in it; several stops
 * in the same country count as one trip. The country name comes from the stops; if one code had different
 * names, the query picks one of them deterministically.
 */
public interface VisitedCountryRepository extends Repository<Waypoint, Long> {

	@Query("""
			select new pl.travelapi.country.domain.VisitedCountry(w.countryCode, max(w.countryName), count(distinct t.id))
			from Waypoint w join w.trip t
			where t.user.id = :userId
			group by w.countryCode
			""")
	List<VisitedCountry> findByUserId(@Param("userId") Long userId);

	/** Only trips with {@code from <= startDate < to}; a range (not a year function) lets the database use its index. */
	@Query("""
			select new pl.travelapi.country.domain.VisitedCountry(w.countryCode, max(w.countryName), count(distinct t.id))
			from Waypoint w join w.trip t
			where t.user.id = :userId and t.startDate >= :from and t.startDate < :to
			group by w.countryCode
			""")
	List<VisitedCountry> findByUserIdAndTripStartBetween(@Param("userId") Long userId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);
}
