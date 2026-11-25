package pl.travelapi.routing.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;

/**
 * The part of the OSRM route service response that the application uses. Distances are in metres and
 * GeoJSON coordinates are {@code [longitude, latitude]}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OsrmRouteResponse(String code, List<OsrmRoute> routes, List<SnappedPoint> waypoints) {

	private static final String OK_CODE = "Ok";
	private static final double METERS_PER_KM = 1000;

	@JsonIgnoreProperties(ignoreUnknown = true)
	record OsrmRoute(Double distance, Geometry geometry) {

		private boolean isComplete() {
			return distance != null && distance >= 0 && geometry != null && geometry.isLine();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Geometry(List<List<Double>> coordinates) {

		private boolean isLine() {
			return coordinates != null && coordinates.size() >= 2
					&& coordinates.stream().allMatch(Geometry::isPosition);
		}

		private static boolean isPosition(List<Double> position) {
			return position != null && position.size() >= 2 && position.stream().allMatch(Objects::nonNull);
		}
	}

	/** An input point moved to the nearest road; {@code distance} says how far it was moved. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record SnappedPoint(Double distance) {
	}

	boolean hasRoute() {
		return OK_CODE.equals(code) && routes != null && !routes.isEmpty()
				&& routes.getFirst() != null && routes.getFirst().isComplete();
	}

	boolean isSnappedWithin(double maxMeters) {
		return waypoints != null && !waypoints.isEmpty() && waypoints.stream()
			.allMatch(point -> point != null && point.distance() != null && point.distance() <= maxMeters);
	}

	/** Call only when {@link #hasRoute()} is true. */
	Route toRoute() {
		OsrmRoute route = routes.getFirst();
		List<GeoPoint> points = route.geometry().coordinates().stream()
			.map(position -> new GeoPoint(position.get(1), position.get(0)))
			.toList();
		return Route.of(points, route.distance() / METERS_PER_KM);
	}
}
