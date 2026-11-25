package pl.travelapi.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteTest {

	private static final GeoPoint A = new GeoPoint(50.0, 20.0);
	private static final GeoPoint B = new GeoPoint(51.0, 21.0);

	@Test
	void shouldRoundDistanceToTwoDecimalPlaces() {
		assertThat(Route.of(List.of(A, B), 12.345).distanceKm()).isEqualTo(new BigDecimal("12.35"));
		assertThat(Route.of(List.of(A, B), 12.344).distanceKm()).isEqualTo(new BigDecimal("12.34"));
		assertThat(Route.of(List.of(A, B), 0).distanceKm()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	void shouldCopyGeometry() {
		List<GeoPoint> points = new ArrayList<>(List.of(A, B));
		Route route = Route.of(points, 1);

		points.add(A);

		assertThat(route.geometry()).containsExactly(A, B);
	}

	@Test
	void shouldRejectGeometryWithOnePoint() {
		assertThatThrownBy(() -> Route.of(List.of(A), 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNegativeDistance() {
		assertThatThrownBy(() -> Route.of(List.of(A, B), -1)).isInstanceOf(IllegalArgumentException.class);
	}
}
