package pl.travelapi.trip.application;

import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.trip.domain.TransportMode;

/** Everything a route depends on: two legs that are equal have the same route. */
record TripLeg(TransportMode transportMode, GeoPoint from, GeoPoint to) {
}
