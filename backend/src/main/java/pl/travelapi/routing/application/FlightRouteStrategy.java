package pl.travelapi.routing.application;

import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.GreatCircle;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.routing.domain.RouteStrategy;
import pl.travelapi.trip.domain.TransportMode;

/**
 * Plane legs: the great-circle distance and the great-circle arc as geometry, so on a flat map a
 * flight is drawn as a curve, like real flight paths.
 */
@Component
public class FlightRouteStrategy implements RouteStrategy {

	/** One arc point per this many kilometres keeps the curve smooth without a huge geometry. */
	static final double KM_PER_SEGMENT = 100;
	static final int MAX_SEGMENTS = 64;

	@Override
	public Set<TransportMode> supportedModes() {
		return EnumSet.of(TransportMode.PLANE);
	}

	@Override
	public Route calculate(GeoPoint from, GeoPoint to) {
		double distanceKm = GreatCircle.distanceKm(from, to);
		int segments = (int) Math.min(MAX_SEGMENTS, Math.max(1, Math.ceil(distanceKm / KM_PER_SEGMENT)));
		return Route.of(GreatCircle.arc(from, to, segments), distanceKm);
	}
}
