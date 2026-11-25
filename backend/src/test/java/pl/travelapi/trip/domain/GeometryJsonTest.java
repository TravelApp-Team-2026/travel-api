package pl.travelapi.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.travelapi.routing.domain.GeoPoint;

class GeometryJsonTest {

	@Test
	void shouldWritePointsWithNamedCoordinates() {
		String json = GeometryJson.write(List.of(new GeoPoint(50.06, 19.94)));

		assertThat(json).isEqualTo("[{\"latitude\":50.06,\"longitude\":19.94}]");
	}

	@Test
	void shouldReadBackTheSamePointsInTheSameOrder() {
		List<GeoPoint> geometry = List.of(new GeoPoint(35.68, 179.5), new GeoPoint(36.0, 181.25));

		assertThat(GeometryJson.read(GeometryJson.write(geometry))).containsExactlyElementsOf(geometry);
	}

	@Test
	void shouldReadMissingGeometryAsEmptyList() {
		assertThat(GeometryJson.read(null)).isEmpty();
	}
}
