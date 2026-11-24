package pl.travelapi.trip.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.travelapi.trip.domain.TransportMode;

class SaveTripRequestValidationTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
	private static final LocalDate START = LocalDate.of(2026, 5, 1);

	@Test
	void shouldAcceptValidTrip() {
		assertThat(violatedFields(trip("Europe", START, null, waypoints(2), segments(1)))).isEmpty();
	}

	@Test
	void shouldAcceptOneDayTripWithEqualDates() {
		assertThat(violatedFields(trip("Europe", START, START, waypoints(2), segments(1)))).isEmpty();
	}

	@Test
	void shouldRejectBlankTitle() {
		assertThat(violatedFields(trip(" ", START, null, waypoints(2), segments(1)))).containsExactly("title");
	}

	@Test
	void shouldRejectMissingStartDate() {
		assertThat(violatedFields(trip("Europe", null, null, waypoints(2), segments(1))))
			.containsExactly("startDate");
	}

	@Test
	void shouldRejectEndDateBeforeStartDate() {
		assertThat(violatedFields(trip("Europe", START, START.minusDays(1), waypoints(2), segments(1))))
			.containsExactly("endDate");
	}

	@Test
	void shouldRejectFewerThanTwoWaypoints() {
		assertThat(violatedFields(trip("Europe", START, null, waypoints(1), segments(0))))
			.containsExactly("waypoints");
	}

	@Test
	void shouldRejectMoreThanFiftyWaypoints() {
		assertThat(violatedFields(trip("Europe", START, null, waypoints(51), segments(50))))
			.containsExactly("waypoints");
	}

	@Test
	void shouldRejectSegmentCountOtherThanWaypointsMinusOne() {
		assertThat(violatedFields(trip("Europe", START, null, waypoints(3), segments(1))))
			.containsExactly("segments");
	}

	@Test
	void shouldRejectNullTransportModeOfSegment() {
		List<SegmentRequest> segments = List.of(new SegmentRequest(null));

		assertThat(violatedFields(trip("Europe", START, null, waypoints(2), segments)))
			.containsExactly("segments[0].transportMode");
	}

	@Test
	void shouldRejectLowercaseCountryCode() {
		List<WaypointRequest> waypoints = List.of(
				new WaypointRequest("Krakow", 50.06, 19.94, "pl", "Polska"), waypoint());

		assertThat(violatedFields(trip("Europe", START, null, waypoints, segments(1))))
			.containsExactly("waypoints[0].countryCode");
	}

	@Test
	void shouldRejectCoordinatesOutOfRange() {
		List<WaypointRequest> waypoints = List.of(
				new WaypointRequest("Nowhere", 91.0, 19.94, "PL", "Polska"),
				new WaypointRequest("Nowhere", 50.0, -181.0, "PL", "Polska"));

		assertThat(violatedFields(trip("Europe", START, null, waypoints, segments(1))))
			.containsExactlyInAnyOrder("waypoints[0].latitude", "waypoints[1].longitude");
	}

	private static List<String> violatedFields(SaveTripRequest request) {
		return VALIDATOR.validate(request).stream().map(violation -> violation.getPropertyPath().toString()).toList();
	}

	private static SaveTripRequest trip(String title, LocalDate startDate, LocalDate endDate,
			List<WaypointRequest> waypoints, List<SegmentRequest> segments) {
		return new SaveTripRequest(title, null, startDate, endDate, waypoints, segments);
	}

	private static WaypointRequest waypoint() {
		return new WaypointRequest("Krakow", 50.06, 19.94, "PL", "Polska");
	}

	private static List<WaypointRequest> waypoints(int count) {
		return Collections.nCopies(count, waypoint());
	}

	private static List<SegmentRequest> segments(int count) {
		return Collections.nCopies(count, new SegmentRequest(TransportMode.CAR));
	}
}
