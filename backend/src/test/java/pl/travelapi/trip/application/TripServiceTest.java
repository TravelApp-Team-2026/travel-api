package pl.travelapi.trip.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.SegmentRequest;
import pl.travelapi.trip.api.TripResponse;
import pl.travelapi.trip.api.TripSummaryResponse;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.api.WaypointResponse;
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

	private final TripRepository tripRepository = mock(TripRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TripService service = new TripService(tripRepository, userRepository);
	private final User owner = User.create("anna@example.com", "hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));

	@Test
	void shouldCreateTripWithOrderedStopsAndZeroDistanceLegs() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TripResponse response = service.create(USER_ID, request("  Europe  ", "   ", 3));

		assertThat(response.title()).isEqualTo("Europe");
		assertThat(response.description()).isNull();
		assertThat(response.waypoints()).extracting(WaypointResponse::order).containsExactly(0, 1, 2);
		assertThat(response.segments()).hasSize(2);
		assertThat(response.segments().get(1).order()).isEqualTo(1);
		assertThat(response.segments().get(1).distanceKm()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(response.totalDistanceKm()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void shouldNotCreateTripWhenAccountNoLongerExists() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(USER_ID, request("Europe", null, 2)))
			.isInstanceOf(ResourceNotFoundException.class);
		verify(tripRepository, never()).save(any());
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
	}

	@Test
	void shouldDeleteOwnTrip() {
		Trip trip = Trip.create(owner, "Europe", null, START, null);
		when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(trip));

		service.delete(USER_ID, TRIP_ID);

		verify(tripRepository).delete(trip);
	}

	private static SaveTripRequest request(String title, String description, int waypointCount) {
		List<WaypointRequest> waypoints = Collections.nCopies(waypointCount,
				new WaypointRequest(" Krakow ", 50.06, 19.94, "PL", "Polska"));
		List<SegmentRequest> segments = Collections.nCopies(waypointCount - 1,
				new SegmentRequest(TransportMode.CAR));
		return new SaveTripRequest(title, description, START, null, waypoints, segments);
	}
}
