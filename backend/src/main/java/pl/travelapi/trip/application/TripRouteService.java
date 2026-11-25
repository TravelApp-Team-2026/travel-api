package pl.travelapi.trip.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import pl.travelapi.routing.application.RouteCalculator;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.domain.GeometryJson;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.TripSegment;
import pl.travelapi.trip.domain.Waypoint;

/** Routes of the legs of a trip. */
@Service
public class TripRouteService {

	private final RouteCalculator routeCalculator;

	public TripRouteService(RouteCalculator routeCalculator) {
		this.routeCalculator = routeCalculator;
	}

	/**
	 * Routes already saved with the trip. Reads the stops and legs, so it has to be called inside the
	 * transaction that loaded the trip. Legs saved without geometry are left out.
	 */
	Map<TripLeg, Route> storedRoutes(Trip trip) {
		List<Waypoint> stops = trip.getWaypoints();
		Map<TripLeg, Route> routes = new HashMap<>();
		for (TripSegment segment : trip.getSegments()) {
			List<GeoPoint> geometry = GeometryJson.read(segment.getGeometryJson());
			if (geometry.isEmpty()) {
				continue;
			}
			int start = segment.getSegmentOrder();
			TripLeg leg = new TripLeg(segment.getTransportMode(), point(stops.get(start)), point(stops.get(start + 1)));
			routes.put(leg, new Route(geometry, segment.getDistanceKm()));
		}
		return routes;
	}

	/**
	 * One route per leg, in travel order. A leg found in {@code storedRoutes} keeps its route; the others
	 * are calculated one by one. This may call an external service, so it must not run in a database
	 * transaction.
	 */
	List<Route> routesFor(SaveTripRequest request, Map<TripLeg, Route> storedRoutes) {
		return IntStream.range(0, request.segments().size())
			.mapToObj(index -> route(leg(request, index), storedRoutes))
			.toList();
	}

	private Route route(TripLeg leg, Map<TripLeg, Route> storedRoutes) {
		Route stored = storedRoutes.get(leg);
		if (stored != null) {
			return stored;
		}
		return routeCalculator.calculate(leg.transportMode(), leg.from(), leg.to());
	}

	private static TripLeg leg(SaveTripRequest request, int index) {
		return new TripLeg(request.segments().get(index).transportMode(),
				point(request.waypoints().get(index)), point(request.waypoints().get(index + 1)));
	}

	private static GeoPoint point(WaypointRequest waypoint) {
		return new GeoPoint(waypoint.latitude(), waypoint.longitude());
	}

	private static GeoPoint point(Waypoint waypoint) {
		return new GeoPoint(waypoint.getLatitude(), waypoint.getLongitude());
	}
}
