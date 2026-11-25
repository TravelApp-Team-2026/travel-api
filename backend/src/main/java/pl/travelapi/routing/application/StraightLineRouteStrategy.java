package pl.travelapi.routing.application;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.GreatCircle;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.routing.domain.RouteStrategy;
import pl.travelapi.trip.domain.TransportMode;

/** Train and other legs: a straight line between the stops and the great-circle distance. */
@Component
public class StraightLineRouteStrategy implements RouteStrategy {

	@Override
	public Set<TransportMode> supportedModes() {
		return EnumSet.of(TransportMode.TRAIN, TransportMode.OTHER);
	}

	@Override
	public Route calculate(GeoPoint from, GeoPoint to) {
		return Route.of(List.of(from, to), GreatCircle.distanceKm(from, to));
	}
}
