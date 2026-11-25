package pl.travelapi.trip.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import pl.travelapi.routing.application.RouteCalculator;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.SegmentRequest;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.domain.GeometryJson;
import pl.travelapi.trip.domain.TransportMode;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.user.domain.User;

class TripRouteServiceTest {

	private static final GeoPoint KRAKOW = new GeoPoint(50.06, 19.94);
	private static final GeoPoint WARSAW = new GeoPoint(52.23, 21.01);
	private static final GeoPoint BERLIN = new GeoPoint(52.52, 13.40);

	private final RouteCalculator routeCalculator = mock(RouteCalculator.class);
	private final TripRouteService service = new TripRouteService(routeCalculator);

	@Test
	void shouldCalculateEveryLegInTravelOrder() {
		Route first = route(KRAKOW, WARSAW, 293.41);
		Route second = route(WARSAW, BERLIN, 517.09);
		when(routeCalculator.calculate(TransportMode.CAR, KRAKOW, WARSAW)).thenReturn(first);
		when(routeCalculator.calculate(TransportMode.PLANE, WARSAW, BERLIN)).thenReturn(second);

		List<Route> routes = service.routesFor(
				request(List.of(KRAKOW, WARSAW, BERLIN), TransportMode.CAR, TransportMode.PLANE), Map.of());

		assertThat(routes).containsExactly(first, second);
	}

	@Test
	void shouldReuseStoredRouteOfUnchangedLegAndCalculateOnlyTheNewOne() {
		Route stored = route(KRAKOW, WARSAW, 293.41);
		Route added = route(WARSAW, BERLIN, 517.09);
		when(routeCalculator.calculate(TransportMode.PLANE, WARSAW, BERLIN)).thenReturn(added);

		List<Route> routes = service.routesFor(
				request(List.of(KRAKOW, WARSAW, BERLIN), TransportMode.CAR, TransportMode.PLANE),
				Map.of(new TripLeg(TransportMode.CAR, KRAKOW, WARSAW), stored));

		assertThat(routes).containsExactly(stored, added);
		verify(routeCalculator).calculate(TransportMode.PLANE, WARSAW, BERLIN);
	}

	@Test
	void shouldCalculateAgainWhenTransportModeChanges() {
		Route byTrain = route(KRAKOW, WARSAW, 252.00);
		when(routeCalculator.calculate(TransportMode.TRAIN, KRAKOW, WARSAW)).thenReturn(byTrain);

		List<Route> routes = service.routesFor(request(List.of(KRAKOW, WARSAW), TransportMode.TRAIN),
				Map.of(new TripLeg(TransportMode.CAR, KRAKOW, WARSAW), route(KRAKOW, WARSAW, 293.41)));

		assertThat(routes).containsExactly(byTrain);
	}

	@Test
	void shouldCalculateAgainWhenDirectionOfLegChanges() {
		Route back = route(WARSAW, KRAKOW, 294.00);
		when(routeCalculator.calculate(TransportMode.CAR, WARSAW, KRAKOW)).thenReturn(back);

		List<Route> routes = service.routesFor(request(List.of(WARSAW, KRAKOW), TransportMode.CAR),
				Map.of(new TripLeg(TransportMode.CAR, KRAKOW, WARSAW), route(KRAKOW, WARSAW, 293.41)));

		assertThat(routes).containsExactly(back);
	}

	@Test
	void shouldReadStoredRoutesAndSkipLegsWithoutGeometry() {
		User owner = User.create("anna@example.com", "hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));
		Trip trip = Trip.create(owner, "Europe", null, LocalDate.of(2026, 5, 1), null);
		addStop(trip, 0, KRAKOW);
		addStop(trip, 1, WARSAW);
		addStop(trip, 2, BERLIN);
		Route saved = route(KRAKOW, WARSAW, 293.41);
		trip.addSegment(0, TransportMode.CAR, saved.distanceKm(), GeometryJson.write(saved.geometry()));
		trip.addSegment(1, TransportMode.PLANE, saved.distanceKm(), null);

		Map<TripLeg, Route> routes = service.storedRoutes(trip);

		assertThat(routes).containsOnly(entry(new TripLeg(TransportMode.CAR, KRAKOW, WARSAW), saved));
		verifyNoInteractions(routeCalculator);
	}

	private static Route route(GeoPoint from, GeoPoint to, double distanceKm) {
		return Route.of(List.of(from, to), distanceKm);
	}

	private static void addStop(Trip trip, int order, GeoPoint point) {
		trip.addWaypoint(order, "Stop", point.latitude(), point.longitude(), "PL", "Polska");
	}

	private static SaveTripRequest request(List<GeoPoint> stops, TransportMode... modes) {
		List<WaypointRequest> waypoints = stops.stream()
			.map(point -> new WaypointRequest("Stop", point.latitude(), point.longitude(), "PL", "Polska"))
			.toList();
		List<SegmentRequest> segments = Stream.of(modes).map(SegmentRequest::new).toList();
		return new SaveTripRequest("Europe", null, LocalDate.of(2026, 5, 1), null, waypoints, segments);
	}
}
