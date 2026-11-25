package pl.travelapi.routing.domain;

/**
 * A point on the Earth in degrees (WGS 84).
 * <p>
 * Longitude is normally in the range -180..180, but points of a route geometry may go beyond it,
 * see {@link GreatCircle#arc(GeoPoint, GeoPoint, int)}.
 */
public record GeoPoint(double latitude, double longitude) {
}
