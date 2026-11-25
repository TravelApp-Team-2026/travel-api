package pl.travelapi.routing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.routing.domain.RouteStrategy;
import pl.travelapi.trip.domain.TransportMode;

class RouteCalculatorTest {

	private static final GeoPoint KRAKOW = new GeoPoint(50.0647, 19.9450);
	private static final GeoPoint BERLIN = new GeoPoint(52.5200, 13.4050);

	private final RouteCalculator calculator =
			new RouteCalculator(List.of(new StraightLineRouteStrategy(), new FlightRouteStrategy()));

	@Test
	void shouldUseStraightLineForTrainAndOther() {
		assertThat(calculator.calculate(TransportMode.TRAIN, KRAKOW, BERLIN).geometry()).hasSize(2);
		assertThat(calculator.calculate(TransportMode.OTHER, KRAKOW, BERLIN).geometry()).hasSize(2);
	}

	@Test
	void shouldUseArcForPlane() {
		Route flight = calculator.calculate(TransportMode.PLANE, KRAKOW, BERLIN);
		Route train = calculator.calculate(TransportMode.TRAIN, KRAKOW, BERLIN);

		assertThat(flight.geometry()).hasSizeGreaterThan(2);
		assertThat(flight.distanceKm()).isEqualByComparingTo(train.distanceKm());
	}

	@Test
	void shouldFailForModeWithoutStrategy() {
		assertThatThrownBy(() -> calculator.calculate(TransportMode.CAR, KRAKOW, BERLIN))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("CAR");
	}

	@Test
	void shouldRejectTwoStrategiesForSameMode() {
		RouteStrategy anotherTrain = new RouteStrategy() {

			@Override
			public Set<TransportMode> supportedModes() {
				return Set.of(TransportMode.TRAIN);
			}

			@Override
			public Route calculate(GeoPoint from, GeoPoint to) {
				return Route.of(List.of(from, to), 0);
			}
		};

		assertThatThrownBy(() -> new RouteCalculator(List.of(new StraightLineRouteStrategy(), anotherTrain)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("TRAIN");
	}
}
