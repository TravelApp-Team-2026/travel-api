package pl.travelapi.trip.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.trip.domain.TransportMode;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.Waypoint;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@IntegrationTest
class TripRepositoryIntegrationTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
	private static final LocalDate START_DATE = LocalDate.of(2026, 5, 1);
	private static final String GEOMETRY = "{\"type\":\"LineString\",\"coordinates\":[[19.94,50.06],[21.01,52.23]]}";

	private final TripRepository tripRepository;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	private User owner;

	@Autowired
	TripRepositoryIntegrationTest(TripRepository tripRepository, UserRepository userRepository,
			JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
	}

	@BeforeEach
	void createOwner() {
		owner = userRepository.saveAndFlush(User.create("trip-owner@example.com", "hash", "Anna", CREATED_AT));
	}

	@AfterEach
	void cleanUp() {
		tripRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldSaveTripWithDefaultsAndReadStopsAndLegsInOrder() {
		Trip trip = Trip.create(owner, "Weekend trip", null, START_DATE, null);
		trip.addWaypoint(1, "Warsaw", 52.23, 21.01, "PL", "Poland");
		trip.addWaypoint(0, "Krakow", 50.06, 19.94, "PL", "Poland");
		trip.addSegment(0, TransportMode.TRAIN, new BigDecimal("250.50"), null);
		Long id = tripRepository.saveAndFlush(trip).getId();

		transactionTemplate.executeWithoutResult(status -> {
			Trip found = tripRepository.findById(id).orElseThrow();
			assertThat(found.getWaypoints()).extracting(Waypoint::getPlaceName).containsExactly("Krakow", "Warsaw");
			assertThat(found.getSegments()).hasSize(1);
			assertThat(found.getSegments().get(0).getTransportMode()).isEqualTo(TransportMode.TRAIN);
			assertThat(found.getSegments().get(0).getDistanceKm()).isEqualByComparingTo("250.50");
			assertThat(found.getSegments().get(0).getGeometryJson()).isNull();
			assertThat(found.getTotalDistanceKm()).isEqualByComparingTo("250.50");
			assertThat(found.getDescription()).isNull();
			assertThat(found.getEndDate()).isNull();
		});
	}

	@Test
	void shouldStoreGeometryAsJsonbObject() {
		Trip trip = tripWithTwoStopsAndOneLeg();
		Long id = tripRepository.saveAndFlush(trip).getId();

		String storedType = jdbcTemplate.queryForObject(
			"SELECT jsonb_typeof(geometry_json) FROM trip_segments WHERE trip_id = ?", String.class, id);
		String loaded = transactionTemplate.execute(status ->
			tripRepository.findById(id).orElseThrow().getSegments().get(0).getGeometryJson());

		assertThat(storedType).isEqualTo("object");
		assertThat(loaded).contains("LineString");
	}

	@Test
	void shouldDeleteWaypointsAndSegmentsWhenTripIsDeletedThroughJpa() {
		Long id = tripRepository.saveAndFlush(tripWithTwoStopsAndOneLeg()).getId();

		tripRepository.deleteById(id);

		assertThat(countWaypoints(id)).isZero();
		assertThat(countSegments(id)).isZero();
	}

	@Test
	void shouldDeleteWaypointsAndSegmentsWhenTripRowIsDeletedInDatabase() {
		Long id = tripRepository.saveAndFlush(tripWithTwoStopsAndOneLeg()).getId();

		jdbcTemplate.update("DELETE FROM trips WHERE id = ?", id);

		assertThat(countWaypoints(id)).isZero();
		assertThat(countSegments(id)).isZero();
	}

	@Test
	void shouldDeleteTripsWhenUserIsDeletedInDatabase() {
		Long id = tripRepository.saveAndFlush(tripWithTwoStopsAndOneLeg()).getId();

		jdbcTemplate.update("DELETE FROM users WHERE id = ?", owner.getId());

		assertThat(tripRepository.existsById(id)).isFalse();
	}

	@Test
	void shouldAcceptSingleDateTripAndOneDayTrip() {
		tripRepository.saveAndFlush(Trip.create(owner, "No end date", null, START_DATE, null));
		tripRepository.saveAndFlush(Trip.create(owner, "Same day", null, START_DATE, START_DATE));

		assertThat(tripRepository.count()).isEqualTo(2);
	}

	@Test
	void shouldRejectEndDateBeforeStartDate() {
		Trip trip = Trip.create(owner, "Wrong dates", null, START_DATE, START_DATE.minusDays(1));

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectDuplicateStopOrder() {
		Trip trip = Trip.create(owner, "Duplicate stops", null, START_DATE, null);
		trip.addWaypoint(0, "Krakow", 50.06, 19.94, "PL", "Poland");
		trip.addWaypoint(0, "Warsaw", 52.23, 21.01, "PL", "Poland");

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectDuplicateSegmentOrder() {
		Trip trip = Trip.create(owner, "Duplicate legs", null, START_DATE, null);
		trip.addSegment(0, TransportMode.CAR, BigDecimal.ZERO, null);
		trip.addSegment(0, TransportMode.PLANE, BigDecimal.ZERO, null);

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectLatitudeOutOfRange() {
		Trip trip = Trip.create(owner, "Bad latitude", null, START_DATE, null);
		trip.addWaypoint(0, "Nowhere", 91.0, 19.94, "PL", "Poland");

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectLowercaseCountryCode() {
		Trip trip = Trip.create(owner, "Bad country code", null, START_DATE, null);
		trip.addWaypoint(0, "Krakow", 50.06, 19.94, "pl", "Poland");

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectNegativeDistance() {
		Trip trip = Trip.create(owner, "Negative distance", null, START_DATE, null);
		trip.addSegment(0, TransportMode.CAR, new BigDecimal("-1.00"), null);

		assertThatThrownBy(() -> tripRepository.saveAndFlush(trip))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldRejectUnknownTransportMode() {
		Long id = tripRepository.saveAndFlush(tripWithTwoStopsAndOneLeg()).getId();

		assertThatThrownBy(() -> jdbcTemplate.update(
			"INSERT INTO trip_segments (trip_id, segment_order, transport_mode) VALUES (?, 5, 'BOAT')", id))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Trip tripWithTwoStopsAndOneLeg() {
		Trip trip = Trip.create(owner, "Weekend trip", "Short trip", START_DATE, START_DATE.plusDays(2));
		trip.addWaypoint(0, "Krakow", 50.06, 19.94, "PL", "Poland");
		trip.addWaypoint(1, "Warsaw", 52.23, 21.01, "PL", "Poland");
		trip.addSegment(0, TransportMode.TRAIN, new BigDecimal("250.50"), GEOMETRY);
		return trip;
	}

	private Long countWaypoints(Long tripId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM waypoints WHERE trip_id = ?", Long.class, tripId);
	}

	private Long countSegments(Long tripId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip_segments WHERE trip_id = ?", Long.class, tripId);
	}
}
