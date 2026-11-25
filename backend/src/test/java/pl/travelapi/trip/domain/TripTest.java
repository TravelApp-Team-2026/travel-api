package pl.travelapi.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pl.travelapi.user.domain.User;

class TripTest {

	private final User owner = User.create("anna@example.com", "hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));

	@Test
	void shouldKeepTotalDistanceEqualToSumOfLegs() {
		Trip trip = newTrip();

		trip.addSegment(0, TransportMode.CAR, new BigDecimal("293.41"), null);
		trip.addSegment(1, TransportMode.PLANE, new BigDecimal("517.09"), null);

		assertThat(trip.getTotalDistanceKm()).isEqualByComparingTo("810.50");
	}

	@Test
	void shouldResetTotalDistanceWhenRouteIsCleared() {
		Trip trip = newTrip();
		trip.addSegment(0, TransportMode.CAR, new BigDecimal("293.41"), null);

		trip.clearRoute();

		assertThat(trip.getTotalDistanceKm()).isEqualByComparingTo("0");
		assertThat(trip.getSegments()).isEmpty();
	}

	private Trip newTrip() {
		return Trip.create(owner, "Europe", null, LocalDate.of(2026, 5, 1), null);
	}
}
