package pl.travelapi.routing.domain;

import java.util.ArrayList;
import java.util.List;

/** Calculations on the shortest path over the surface of the Earth, treated as a sphere. */
public final class GreatCircle {

	/** Mean Earth radius (IUGG). */
	static final double EARTH_RADIUS_KM = 6371.0088;

	/** Below this value the sine of the central angle is treated as zero (same or opposite points). */
	private static final double MIN_SIN_ANGLE = 1e-9;

	private GreatCircle() {
	}

	/** Great-circle distance in kilometres (Haversine formula). */
	public static double distanceKm(GeoPoint from, GeoPoint to) {
		return EARTH_RADIUS_KM * centralAngle(from, to);
	}

	/**
	 * Points along the great circle from {@code from} to {@code to}, both included.
	 * <p>
	 * Longitudes are continuous: an arc that crosses the 180th meridian continues past 180 (or below
	 * -180) instead of jumping to the other side of the map, so the line can be drawn without a break.
	 * For the same or exactly opposite points, where the arc is not defined, the result is just the
	 * two points.
	 *
	 * @param segments number of straight pieces the arc is divided into, at least 1
	 */
	public static List<GeoPoint> arc(GeoPoint from, GeoPoint to, int segments) {
		if (segments < 1) {
			throw new IllegalArgumentException("An arc needs at least 1 segment");
		}
		double angle = centralAngle(from, to);
		double sinAngle = Math.sin(angle);
		if (sinAngle < MIN_SIN_ANGLE) {
			return List.of(from, to);
		}

		double lat1 = Math.toRadians(from.latitude());
		double lon1 = Math.toRadians(from.longitude());
		double lat2 = Math.toRadians(to.latitude());
		double lon2 = Math.toRadians(to.longitude());

		List<GeoPoint> points = new ArrayList<>(segments + 1);
		points.add(from);
		double previousLongitude = from.longitude();
		for (int i = 1; i < segments; i++) {
			double fraction = (double) i / segments;
			double a = Math.sin((1 - fraction) * angle) / sinAngle;
			double b = Math.sin(fraction * angle) / sinAngle;
			double x = a * Math.cos(lat1) * Math.cos(lon1) + b * Math.cos(lat2) * Math.cos(lon2);
			double y = a * Math.cos(lat1) * Math.sin(lon1) + b * Math.cos(lat2) * Math.sin(lon2);
			double z = a * Math.sin(lat1) + b * Math.sin(lat2);
			double latitude = Math.toDegrees(Math.atan2(z, Math.hypot(x, y)));
			double longitude = unwrap(Math.toDegrees(Math.atan2(y, x)), previousLongitude);
			points.add(new GeoPoint(latitude, longitude));
			previousLongitude = longitude;
		}
		points.add(new GeoPoint(to.latitude(), unwrap(to.longitude(), previousLongitude)));
		return points;
	}

	private static double centralAngle(GeoPoint from, GeoPoint to) {
		double lat1 = Math.toRadians(from.latitude());
		double lat2 = Math.toRadians(to.latitude());
		double sinHalfLat = Math.sin((lat2 - lat1) / 2);
		double sinHalfLon = Math.sin(Math.toRadians(to.longitude() - from.longitude()) / 2);
		double h = sinHalfLat * sinHalfLat + Math.cos(lat1) * Math.cos(lat2) * sinHalfLon * sinHalfLon;
		return 2 * Math.asin(Math.min(1, Math.sqrt(h)));
	}

	/** Shifts the longitude by whole turns so it is at most 180 degrees away from the previous one. */
	private static double unwrap(double longitude, double previousLongitude) {
		return longitude + 360 * Math.rint((previousLongitude - longitude) / 360);
	}
}
