package pl.travelapi.trip.api;

import pl.travelapi.trip.domain.Waypoint;

public record WaypointResponse(
		int order,
		String placeName,
		double latitude,
		double longitude,
		String countryCode,
		String countryName) {

	public static WaypointResponse from(Waypoint waypoint) {
		return new WaypointResponse(waypoint.getStopOrder(), waypoint.getPlaceName(), waypoint.getLatitude(),
				waypoint.getLongitude(), waypoint.getCountryCode(), waypoint.getCountryName());
	}
}
