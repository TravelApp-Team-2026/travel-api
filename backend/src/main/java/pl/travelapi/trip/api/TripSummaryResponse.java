package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import pl.travelapi.trip.domain.TripSummary;

public record TripSummaryResponse(
		Long id,
		String title,
		LocalDate startDate,
		@Schema(nullable = true) LocalDate endDate,
		int stopCount,
		BigDecimal totalDistanceKm) {

	public static TripSummaryResponse from(TripSummary summary) {
		return new TripSummaryResponse(summary.id(), summary.title(), summary.startDate(), summary.endDate(),
				summary.stopCount(), summary.totalDistanceKm());
	}
}
