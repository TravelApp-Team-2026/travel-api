package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import pl.travelapi.trip.domain.Trip;

public record TripResponse(
		Long id,
		String title,
		@Schema(nullable = true) String description,
		LocalDate startDate,
		@Schema(nullable = true) LocalDate endDate,
		@Schema(description = "Suma odległości odcinków w km; do czasu obliczania tras zawsze 0.00")
		BigDecimal totalDistanceKm,
		List<WaypointResponse> waypoints,
		List<SegmentResponse> segments) {

	/** Reads the stops and legs, so it has to be called inside the transaction that loaded the trip. */
	public static TripResponse from(Trip trip) {
		return new TripResponse(trip.getId(), trip.getTitle(), trip.getDescription(), trip.getStartDate(),
				trip.getEndDate(), trip.getTotalDistanceKm(),
				trip.getWaypoints().stream().map(WaypointResponse::from).toList(),
				trip.getSegments().stream().map(SegmentResponse::from).toList());
	}
}
