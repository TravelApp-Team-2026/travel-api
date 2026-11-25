package pl.travelapi.routing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.trip.domain.TransportMode;

class FlightRouteStrategyTest {

	private static final GeoPoint WARSAW = new GeoPoint(52.2297, 21.0122);
	private static final GeoPoint KRAKOW = new GeoPoint(50.0647, 19.9450);
	private static final GeoPoint NEW_YORK = new GeoPoint(40.7128, -74.0060);

	private final FlightRouteStrategy strategy = new FlightRouteStrategy();

	@Test
	void shouldSupportOnlyPlane() {
		assertThat(strategy.supportedModes()).containsExactly(TransportMode.PLANE);
	}

	@Test
	void shouldReturnArcWithOnePointPerHundredKilometres() {
		Route route = strategy.calculate(WARSAW, KRAKOW);

		// about 252 km: 3 segments, 4 points
		assertThat(route.geometry()).hasSize(4);
		assertThat(route.geometry().getFirst()).isEqualTo(WARSAW);
		assertThat(route.distanceKm().doubleValue()).isBetween(251.0, 253.0);
	}

	@Test
	void shouldLimitNumberOfArcPointsOnLongFlights() {
		Route route = strategy.calculate(WARSAW, NEW_YORK);

		assertThat(route.geometry()).hasSize(FlightRouteStrategy.MAX_SEGMENTS + 1);
		assertThat(route.distanceKm().doubleValue()).isBetween(6850.0, 6862.0);
	}

	@Test
	void shouldReturnTwoPointsForSameStart() {
		Route route = strategy.calculate(KRAKOW, KRAKOW);

		assertThat(route.geometry()).containsExactly(KRAKOW, KRAKOW);
		assertThat(route.distanceKm()).isZero();
	}
}
