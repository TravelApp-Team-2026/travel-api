package pl.travelapi.trip.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import pl.travelapi.routing.application.RouteCalculator;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.trip.api.GeometryPointResponse;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.SegmentRequest;
import pl.travelapi.trip.api.SegmentResponse;
import pl.travelapi.trip.api.TripResponse;
import pl.travelapi.trip.api.TripSummaryResponse;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.api.WaypointResponse;
import pl.travelapi.trip.domain.GeometryJson;
import pl.travelapi.trip.domain.TransportMode;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.TripNotFoundException;
import pl.travelapi.trip.domain.TripSummary;
import pl.travelapi.trip.infrastructure.TripRepository;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

class TripServiceTest {

	private static final long USER_ID = 7L;
	private static final long TRIP_ID = 3L;
	private static final LocalDate START = LocalDate.of(2026, 5, 1);
	private static final double CALCULATED_KM = 100.25;
	private static final GeoPoint KRAKOW = new GeoPoint(50.06, 19.94);
	private static final GeoPoint WARSAW = new GeoPoint(52.23, 21.01);
	private static final GeoPoint BERLIN = new GeoPoint(52.52, 13.40);

	private boolean inTransaction;

	private final TransactionOperations transactions = new TransactionOperations() {
		@Override
		public <T> T execute(TransactionCallback<T> action) {
			inTransaction = true;
			try {
				return action.doInTransaction(new SimpleTransactionStatus());
			}
			finally {
				inTransaction = false;
			}
		}
	};

	private final TripRepository tripRepository = mock(TripRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RouteCalculator routeCalculator = mock(RouteCalculator.class);
	private final TripService service = new TripService(tripRepository, userRepository,
			new TripRouteService(routeCalculator), transactions);
	private final User owner = User.create("anna@example.com", "hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));

	@BeforeEach
	void calculateStraightLinesOutsideTransactions() {
		when(routeCalculator.calculate(any(), any(), any())).thenAnswer(invocation -> {
			assertThat(inTransaction).as("route calculated inside a transaction").isFalse();
			GeoPoint from = invocation.getArgument(1);
			GeoPoint to = invocation.getArgument(2);
			return Route.of(List.of(from, to), CALCULATED_KM);
		});
	}

	@Test
	void shouldCreateTripWithOrderedStopsAndCalculatedLegs() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TripResponse response = service.create(USER_ID, request("  Europe  ", "   ", 3));

		assertThat(response.title()).isEqualTo("Europe");
		assertThat(response.description()).isNull();
		assertThat(response.waypoints()).extracting(WaypointResponse::order).containsExactly(0, 1, 2);
		assertThat(response.segments()).extracting(SegmentResponse::order).containsExactly(0, 1);
		assertThat(response.segments().get(1).distanceKm()).isEqualByComparingTo("100.25");
		assertThat(response.segments().get(1).geometry())
			.containsExactly(new GeometryPointResponse(50.06, 19.94), new GeometryPointResponse(50.06, 19.94));
		assertThat(response.totalDistanceKm()).isEqualByComparingTo("200.50");
		verify(routeCalculator, times(2)).calculate(any(), any(), any());
	}

	@Test
	void shouldNotCreateTripWhenAccountNoLongerExists() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(USER_ID, request("Europe", null, 2)))
			.isInstanceOf(ResourceNotFoundException.class);
		verify(tripRepository, never()).save(any());
		verifyNoInteractions(routeCalculator);
	}

	@Test
	void shouldReplaceDetailsStopsAndLegsOnUpdate() {
		Trip trip = Trip.create(owner, "Old", "Old description", START, null);
		trip.addWaypoint(0, "A", 1.0, 1.0, "PL", "Polska");
		trip.addWaypoint(1, "B", 2.0, 2.0, "PL", "Polska");
		trip.addSegment(0, TransportMode.TRAIN, BigDecimal.ZERO, null);
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(trip));

		TripResponse response = service.update(USER_ID, TRIP_ID, request("New", "New description", 3));

		verify(tripRepository).flush();
		assertThat(response.title()).isEqualTo("New");
		assertThat(response.description()).isEqualTo("New description");
		assertThat(response.waypoints()).extracting(WaypointResponse::placeName)
			.containsExactly("Krakow", "Krakow", "Krakow");
		assertThat(response.segments()).hasSize(2);
		assertThat(response.totalDistanceKm()).isEqualByComparingTo("200.50");
	}

	@Test
	void shouldKeepSavedRoutesWhenOnlyDetailsChange() {
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(savedCarTrip()));

		TripResponse response = service.update(USER_ID, TRIP_ID, carRequest("New title", KRAKOW, WARSAW));

		assertThat(response.title()).isEqualTo("New title");
		assertThat(response.segments().get(0).distanceKm()).isEqualByComparingTo("293.41");
		assertThat(response.segments().get(0).geometry()).hasSize(3);
		assertThat(response.totalDistanceKm()).isEqualByComparingTo("293.41");
		verifyNoInteractions(routeCalculator);
	}

	@Test
	void shouldCalculateOnlyTheLegAddedOnUpdate() {
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(savedCarTrip()));

		TripResponse response = service.update(USER_ID, TRIP_ID, carRequest("Longer", KRAKOW, WARSAW, BERLIN));

		assertThat(response.totalDistanceKm()).isEqualByComparingTo("393.66");
		verify(routeCalculator).calculate(TransportMode.CAR, WARSAW, BERLIN);
		verify(routeCalculator, times(1)).calculate(any(), any(), any());
	}

	@Test
	void shouldReturnSummariesOfOwnTrips() {
		TripSummary summary = new TripSummary(TRIP_ID, "Europe", START, null, 3, BigDecimal.ZERO);
		when(tripRepository.findSummariesByUserId(USER_ID)).thenReturn(List.of(summary));

		List<TripSummaryResponse> result = service.list(USER_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).stopCount()).isEqualTo(3);
	}

	@Test
	void shouldTreatTripOfAnotherUserAsNotFound() {
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(USER_ID, TRIP_ID)).isInstanceOf(TripNotFoundException.class);
		assertThatThrownBy(() -> service.update(USER_ID, TRIP_ID, request("New", null, 2)))
			.isInstanceOf(TripNotFoundException.class);
		assertThatThrownBy(() -> service.delete(USER_ID, TRIP_ID)).isInstanceOf(TripNotFoundException.class);
		verify(tripRepository, never()).delete(any());
		verifyNoInteractions(routeCalculator);
	}

	@Test
	void shouldDeleteOwnTrip() {
		Trip trip = Trip.create(owner, "Europe", null, START, null);
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(trip));

		service.delete(USER_ID, TRIP_ID);

		verify(tripRepository).delete(trip);
	}

	/** A saved trip Krakow - Warsaw by car, with a road geometry of 3 points. */
	private Trip savedCarTrip() {
		Trip trip = Trip.create(owner, "Europe", null, START, null);
		trip.addWaypoint(0, "Krakow", KRAKOW.latitude(), KRAKOW.longitude(), "PL", "Polska");
		trip.addWaypoint(1, "Warsaw", WARSAW.latitude(), WARSAW.longitude(), "PL", "Polska");
		trip.addSegment(0, TransportMode.CAR, new BigDecimal("293.41"),
				GeometryJson.write(List.of(KRAKOW, new GeoPoint(51.40, 20.30), WARSAW)));
		return trip;
	}

	private static SaveTripRequest carRequest(String title, GeoPoint... stops) {
		List<WaypointRequest> waypoints = List.of(stops).stream()
			.map(point -> new WaypointRequest("Stop", point.latitude(), point.longitude(), "PL", "Polska"))
			.toList();
		List<SegmentRequest> segments = Collections.nCopies(stops.length - 1, new SegmentRequest(TransportMode.CAR));
		return new SaveTripRequest(title, null, START, null, waypoints, segments);
	}

	private static SaveTripRequest request(String title, String description, int waypointCount) {
		List<WaypointRequest> waypoints = Collections.nCopies(waypointCount,
				new WaypointRequest(" Krakow ", 50.06, 19.94, "PL", "Polska"));
		List<SegmentRequest> segments = Collections.nCopies(waypointCount - 1,
				new SegmentRequest(TransportMode.CAR));
		return new SaveTripRequest(title, description, START, null, waypoints, segments);
	}
}
