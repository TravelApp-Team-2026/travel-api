package pl.travelapi.routing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculated shape and length of one trip leg.
 *
 * @param geometry points of the line from the start to the end of the leg, at least two
 * @param distanceKm length in kilometres, rounded to 2 decimal places like {@code distance_km} in the
 *        database, so the total distance of a trip is exactly the sum of its legs
 */
public record Route(List<GeoPoint> geometry, BigDecimal distanceKm) {

	public Route {
		geometry = List.copyOf(geometry);
		if (geometry.size() < 2) {
			throw new IllegalArgumentException("Route geometry needs at least 2 points");
		}
		if (distanceKm.signum() < 0) {
			throw new IllegalArgumentException("Route distance cannot be negative");
		}
		distanceKm = distanceKm.setScale(2, RoundingMode.HALF_UP);
	}

	public static Route of(List<GeoPoint> geometry, double distanceKm) {
		return new Route(geometry, BigDecimal.valueOf(distanceKm));
	}
}
