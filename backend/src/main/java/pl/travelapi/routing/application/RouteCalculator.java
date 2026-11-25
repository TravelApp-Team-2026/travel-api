package pl.travelapi.routing.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.routing.domain.RouteStrategy;
import pl.travelapi.trip.domain.TransportMode;

/** Calculates a leg with the strategy registered for its transport mode. */
@Service
public class RouteCalculator {

	private final Map<TransportMode, RouteStrategy> strategies = new EnumMap<>(TransportMode.class);

	public RouteCalculator(List<RouteStrategy> strategies) {
		for (RouteStrategy strategy : strategies) {
			for (TransportMode mode : strategy.supportedModes()) {
				RouteStrategy previous = this.strategies.put(mode, strategy);
				if (previous != null) {
					throw new IllegalStateException("More than one route strategy for " + mode + ": "
							+ previous.getClass().getSimpleName() + " and " + strategy.getClass().getSimpleName());
				}
			}
		}
	}

	public Route calculate(TransportMode mode, GeoPoint from, GeoPoint to) {
		RouteStrategy strategy = strategies.get(mode);
		if (strategy == null) {
			throw new IllegalStateException("No route strategy for " + mode);
		}
		return strategy.calculate(from, to);
	}
}
