package pl.travelapi.trip.api;

import java.math.BigDecimal;
import pl.travelapi.trip.domain.TransportMode;
import pl.travelapi.trip.domain.TripSegment;

public record SegmentResponse(int order, TransportMode transportMode, BigDecimal distanceKm) {

	public static SegmentResponse from(TripSegment segment) {
		return new SegmentResponse(segment.getSegmentOrder(), segment.getTransportMode(), segment.getDistanceKm());
	}
}
