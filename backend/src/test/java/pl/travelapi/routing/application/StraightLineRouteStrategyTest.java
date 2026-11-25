package pl.travelapi.routing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.GreatCircle;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.trip.domain.TransportMode;

class StraightLineRouteStrategyTest {

	private static final GeoPoint KRAKOW = new GeoPoint(50.0647, 19.9450);
	private static final GeoPoint BERLIN = new GeoPoint(52.5200, 13.4050);

	private final StraightLineRouteStrategy strategy = new StraightLineRouteStrategy();

	@Test
	void shouldSupportTrainAndOther() {
		assertThat(strategy.supportedModes()).containsExactlyInAnyOrder(TransportMode.TRAIN, TransportMode.OTHER);
	}

	@Test
	void shouldReturnStraightLineWithGreatCircleDistance() {
		Route route = strategy.calculate(KRAKOW, BERLIN);

		assertThat(route.geometry()).containsExactly(KRAKOW, BERLIN);
		assertThat(route.distanceKm()).isEqualByComparingTo(
				BigDecimal.valueOf(GreatCircle.distanceKm(KRAKOW, BERLIN)).setScale(2, RoundingMode.HALF_UP));
		assertThat(route.distanceKm().doubleValue()).isBetween(525.0, 535.0);
	}
}
