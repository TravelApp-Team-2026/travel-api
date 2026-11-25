package pl.travelapi.routing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.GreatCircle;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.trip.domain.TransportMode;

class CarRouteStrategyTest {

	private static final GeoPoint LEGNICA = new GeoPoint(51.2070, 16.1619);
	private static final GeoPoint BERLIN = new GeoPoint(52.5200, 13.4050);

	@Test
	void shouldSupportOnlyCar() {
		CarRouteStrategy strategy = new CarRouteStrategy((from, to) -> Optional.empty());

		assertThat(strategy.supportedModes()).containsExactly(TransportMode.CAR);
	}

	@Test
	void shouldReturnRoadRouteFromProvider() {
		Route road = Route.of(List.of(LEGNICA, new GeoPoint(51.9, 15.2), BERLIN), 310.5);
		CarRouteStrategy strategy = new CarRouteStrategy((from, to) -> Optional.of(road));

		assertThat(strategy.calculate(LEGNICA, BERLIN)).isEqualTo(road);
	}

	@Test
	void shouldFallBackToStraightLineWhenThereIsNoRoadRoute() {
		CarRouteStrategy strategy = new CarRouteStrategy((from, to) -> Optional.empty());

		Route route = strategy.calculate(LEGNICA, BERLIN);

		assertThat(route).isEqualTo(Route.of(List.of(LEGNICA, BERLIN), GreatCircle.distanceKm(LEGNICA, BERLIN)));
	}

	@Test
	void shouldCompleteStrategiesForEveryTransportMode() {
		RouteCalculator calculator = new RouteCalculator(List.of(new StraightLineRouteStrategy(),
				new FlightRouteStrategy(), new CarRouteStrategy((from, to) -> Optional.empty())));

		for (TransportMode mode : TransportMode.values()) {
			assertThatCode(() -> calculator.calculate(mode, LEGNICA, BERLIN)).doesNotThrowAnyException();
		}
	}
}
