package pl.travelapi.routing.application;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.travelapi.routing.domain.CarRouteProvider;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.GreatCircle;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.routing.domain.RouteStrategy;
import pl.travelapi.trip.domain.TransportMode;

/**
 * Car legs: the road route and driving distance. When no road route can be obtained, the leg becomes
 * a straight line with the great-circle distance, so a trip can always be saved.
 */
@Component
public class CarRouteStrategy implements RouteStrategy {

	private final CarRouteProvider carRouteProvider;

	public CarRouteStrategy(CarRouteProvider carRouteProvider) {
		this.carRouteProvider = carRouteProvider;
	}

	@Override
	public Set<TransportMode> supportedModes() {
		return EnumSet.of(TransportMode.CAR);
	}

	@Override
	public Route calculate(GeoPoint from, GeoPoint to) {
		return carRouteProvider.findRoute(from, to).orElseGet(() -> straightLine(from, to));
	}

	private static Route straightLine(GeoPoint from, GeoPoint to) {
		return Route.of(List.of(from, to), GreatCircle.distanceKm(from, to));
	}
}
