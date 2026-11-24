package pl.travelapi.trip.application;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.SegmentRequest;
import pl.travelapi.trip.api.TripResponse;
import pl.travelapi.trip.api.TripSummaryResponse;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.TripNotFoundException;
import pl.travelapi.trip.infrastructure.TripRepository;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Service
public class TripService {

	static final String USER_NOT_FOUND_MESSAGE = "Nie znaleziono użytkownika.";

	/** Distances are calculated later; until then every leg has zero length. */
	private static final BigDecimal NOT_CALCULATED = BigDecimal.ZERO.setScale(2);

	private final TripRepository tripRepository;
	private final UserRepository userRepository;

	public TripService(TripRepository tripRepository, UserRepository userRepository) {
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
	}

	/**
	 * @throws ResourceNotFoundException if the account from the token no longer exists
	 */
	@Transactional
	public TripResponse create(long userId, SaveTripRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE));
		Trip trip = Trip.create(user, request.title().trim(), normalizeDescription(request.description()),
				request.startDate(), request.endDate());
		addRoute(trip, request);
		return TripResponse.from(tripRepository.save(trip));
	}

	@Transactional(readOnly = true)
	public List<TripSummaryResponse> list(long userId) {
		return tripRepository.findSummariesByUserId(userId).stream().map(TripSummaryResponse::from).toList();
	}

	/**
	 * @throws TripNotFoundException if the trip does not exist or belongs to another user
	 */
	@Transactional(readOnly = true)
	public TripResponse get(long userId, long tripId) {
		return TripResponse.from(findOwned(userId, tripId));
	}

	/**
	 * Replaces the details, stops and legs.
	 *
	 * @throws TripNotFoundException if the trip does not exist or belongs to another user
	 */
	@Transactional
	public TripResponse update(long userId, long tripId, SaveTripRequest request) {
		Trip trip = findOwned(userId, tripId);
		trip.updateDetails(request.title().trim(), normalizeDescription(request.description()),
				request.startDate(), request.endDate());
		trip.clearRoute();
		// Hibernate inserts before it deletes; without this flush the new stops would clash with the old
		// ones on the unique (trip_id, stop_order) and (trip_id, segment_order) constraints.
		tripRepository.flush();
		addRoute(trip, request);
		return TripResponse.from(trip);
	}

	/**
	 * @throws TripNotFoundException if the trip does not exist or belongs to another user
	 */
	@Transactional
	public void delete(long userId, long tripId) {
		tripRepository.delete(findOwned(userId, tripId));
	}

	private Trip findOwned(long userId, long tripId) {
		return tripRepository.findByIdAndUserId(tripId, userId).orElseThrow(TripNotFoundException::new);
	}

	private static void addRoute(Trip trip, SaveTripRequest request) {
		List<WaypointRequest> waypoints = request.waypoints();
		for (int order = 0; order < waypoints.size(); order++) {
			WaypointRequest waypoint = waypoints.get(order);
			trip.addWaypoint(order, waypoint.placeName().trim(), waypoint.latitude(), waypoint.longitude(),
					waypoint.countryCode(), waypoint.countryName().trim());
		}
		List<SegmentRequest> segments = request.segments();
		for (int order = 0; order < segments.size(); order++) {
			trip.addSegment(order, segments.get(order).transportMode(), NOT_CALCULATED, null);
		}
	}

	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}
}
