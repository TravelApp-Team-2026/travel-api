package pl.travelapi.trip.domain;

import java.util.List;
import pl.travelapi.routing.domain.GeoPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Format of {@code trip_segments.geometry_json}: an array of points in travel order, each with named
 * {@code latitude} and {@code longitude}.
 */
public final class GeometryJson {

	// Own mapper on purpose: the stored format must not change with the JSON settings of the web layer.
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private GeometryJson() {
	}

	public static String write(List<GeoPoint> geometry) {
		return JSON.writeValueAsString(geometry);
	}

	/** Returns an empty list for a leg that was saved without geometry. */
	public static List<GeoPoint> read(String json) {
		if (json == null) {
			return List.of();
		}
		return List.of(JSON.readValue(json, GeoPoint[].class));
	}
}
