package pl.travelapi.routing.infrastructure;

import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.travelapi.routing.domain.CarRouteProvider;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.shared.http.RequestSpacer;

/**
 * Car routes from an OSRM server. The public demo server requires an identifying {@code User-Agent}
 * and at most 1 request per second. Every failure gives an empty result, never an exception.
 */
public class OsrmCarRouteProvider implements CarRouteProvider {

	private static final Logger log = LoggerFactory.getLogger(OsrmCarRouteProvider.class);

	/** OSRM takes longitude before latitude. */
	private static final String ROUTE_PATH = "/route/v1/driving/{fromLon},{fromLat};{toLon},{toLat}";

	private final RestClient restClient;
	private final RequestSpacer requestSpacer;
	private final double maxSnapDistanceMeters;

	public OsrmCarRouteProvider(RestClient.Builder builder, OsrmProperties properties,
			RequestSpacer requestSpacer) {
		this.restClient = builder
			.baseUrl(properties.baseUrl())
			.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
			// The JDK client asks for "gzip, deflate" by default. OSRM then answers "Content-Encoding: deflate"
			// with a gzip body, which cannot be read. The response is small, so it is requested uncompressed.
			.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
			.build();
		this.requestSpacer = requestSpacer;
		this.maxSnapDistanceMeters = properties.maxSnapDistanceMeters();
	}

	@Override
	public Optional<Route> findRoute(GeoPoint from, GeoPoint to) {
		if (!waitForTurn()) {
			return Optional.empty();
		}
		return fetch(from, to).flatMap(this::toUsableRoute);
	}

	private boolean waitForTurn() {
		try {
			if (requestSpacer.acquire()) {
				return true;
			}
			log.warn("OSRM request skipped, too many requests are waiting; using a straight line");
			return false;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Interrupted while waiting to call OSRM; using a straight line");
			return false;
		}
	}

	// OSRM answers "no route" and "no road near the point" with HTTP 400, so they end up in the catch.
	private Optional<OsrmRouteResponse> fetch(GeoPoint from, GeoPoint to) {
		try {
			OsrmRouteResponse response = restClient.get()
				.uri(uri -> uri.path(ROUTE_PATH)
					.queryParam("overview", "simplified")
					.queryParam("geometries", "geojson")
					.build(coordinate(from.longitude()), coordinate(from.latitude()),
							coordinate(to.longitude()), coordinate(to.latitude())))
				.retrieve()
				.body(OsrmRouteResponse.class);
			if (response == null) {
				log.warn("OSRM returned an empty response; using a straight line");
			}
			return Optional.ofNullable(response);
		}
		catch (RestClientException e) {
			log.warn("OSRM request failed; using a straight line: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<Route> toUsableRoute(OsrmRouteResponse response) {
		if (!response.hasRoute()) {
			log.warn("OSRM returned no usable route (code {}); using a straight line", response.code());
			return Optional.empty();
		}
		// OSRM moves a point to the nearest road however far it is, for example from the open sea.
		if (!response.isSnappedWithin(maxSnapDistanceMeters)) {
			log.warn("OSRM found no road within {} m of a stop; using a straight line", maxSnapDistanceMeters);
			return Optional.empty();
		}
		return Optional.of(response.toRoute());
	}

	/** Plain decimal notation: very small values must not be sent as, for example, 1.0E-5. */
	private static String coordinate(double degrees) {
		return BigDecimal.valueOf(degrees).toPlainString();
	}
}
