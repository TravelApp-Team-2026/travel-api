package pl.travelapi.routing.domain;

import java.util.Set;
import pl.travelapi.trip.domain.TransportMode;

/** Calculates the geometry and distance of a leg for the transport modes it supports. */
public interface RouteStrategy {

	/** Transport modes handled by this strategy; each mode has exactly one strategy. */
	Set<TransportMode> supportedModes();

	Route calculate(GeoPoint from, GeoPoint to);
}
