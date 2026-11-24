package pl.travelapi.trip.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only row of the trip list; filled directly by a query, so the stops are only counted, not loaded. */
public record TripSummary(
		Long id,
		String title,
		LocalDate startDate,
		LocalDate endDate,
		int stopCount,
		BigDecimal totalDistanceKm) {
}
