package pl.travelapi.trip.application;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.trip.api.SaveTripRequest;
import pl.travelapi.trip.api.SegmentRequest;
import pl.travelapi.trip.api.TripResponse;
import pl.travelapi.trip.api.TripSummaryResponse;
import pl.travelapi.trip.api.WaypointRequest;
import pl.travelapi.trip.domain.GeometryJson;
import pl.travelapi.trip.domain.Trip;
import pl.travelapi.trip.domain.TripNotFoundException;
import pl.travelapi.trip.infrastructure.TripRepository;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Service
public class TripService {

	static final String USER_NOT_FOUND_MESSAGE = "Nie znaleziono użytkownika.";

	private final TripRepository tripRepository;
	private final UserRepository userRepository;
	private final TripRouteService tripRouteService;
	private final TransactionOperations transactions;

	public TripService(TripRepository tripRepository, UserRepository userRepository,
			TripRouteService tripRouteService, TransactionOperations transactions) {
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
		this.tripRouteService = tripRouteService;
		this.transactions = transactions;
	}

	/**
	 * Not transactional as a whole: calculating routes may take seconds, and no database connection should
	 * be held meanwhile. Saving the new trip with its stops and legs is one transaction.
	 *
	 * @throws ResourceNotFoundException if the account from the token no longer exists
	 */
	public TripResponse create(long userId, SaveTripRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE));
		List<Route> routes = tripRouteService.routesFor(request, Map.of());
		Trip trip = Trip.create(user, request.title().trim(), normalizeDescription(request.description()),
				request.startDate(), request.endDate());
		addRoute(trip, request, routes);
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
	 * Replaces the details, stops and legs. Legs with unchanged ends and transport mode keep their saved
	 * route. Routes are calculated between two transactions: one reads the saved routes, one writes.
	 *
	 * @throws TripNotFoundException if the trip does not exist or belongs to another user
	 */
	public TripResponse update(long userId, long tripId, SaveTripRequest request) {
		Map<TripLeg, Route> storedRoutes = transactions
			.execute(status -> tripRouteService.storedRoutes(findOwned(userId, tripId)));
		List<Route> routes = tripRouteService.routesFor(request, storedRoutes);
		return transactions.execute(status -> replace(userId, tripId, request, routes));
	}

	/**
	 * @throws TripNotFoundException if the trip does not exist or belongs to another user
	 */
	@Transactional
	public void delete(long userId, long tripId) {
		tripRepository.delete(findOwned(userId, tripId));
	}

	// The trip is loaded again because it may have been deleted while the routes were being calculated.
	private TripResponse replace(long userId, long tripId, SaveTripRequest request, List<Route> routes) {
		Trip trip = findOwned(userId, tripId);
		trip.updateDetails(request.title().trim(), normalizeDescription(request.description()),
				request.startDate(), request.endDate());
		trip.clearRoute();
		// Hibernate inserts before it deletes; without this flush the new stops would clash with the old
		// ones on the unique (trip_id, stop_order) and (trip_id, segment_order) constraints.
		tripRepository.flush();
		addRoute(trip, request, routes);
		return TripResponse.from(trip);
	}

	private Trip findOwned(long userId, long tripId) {
		return tripRepository.findByIdAndUserId(tripId, userId).orElseThrow(TripNotFoundException::new);
	}

	private static void addRoute(Trip trip, SaveTripRequest request, List<Route> routes) {
		List<WaypointRequest> waypoints = request.waypoints();
		for (int order = 0; order < waypoints.size(); order++) {
			WaypointRequest waypoint = waypoints.get(order);
			trip.addWaypoint(order, waypoint.placeName().trim(), waypoint.latitude(), waypoint.longitude(),
					waypoint.countryCode(), waypoint.countryName().trim());
		}
		List<SegmentRequest> segments = request.segments();
		for (int order = 0; order < segments.size(); order++) {
			Route route = routes.get(order);
			trip.addSegment(order, segments.get(order).transportMode(), route.distanceKm(),
					GeometryJson.write(route.geometry()));
		}
	}

	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}
}
