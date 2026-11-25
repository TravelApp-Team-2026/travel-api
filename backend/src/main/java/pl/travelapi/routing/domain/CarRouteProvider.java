package pl.travelapi.routing.domain;

import java.util.Optional;

/** Source of road routes for car legs. */
public interface CarRouteProvider {

	/**
	 * Finds a driving route between two points.
	 *
	 * @return the road route, or empty when there is no usable route or the routing service is
	 *         unavailable
	 */
	Optional<Route> findRoute(GeoPoint from, GeoPoint to);
}
