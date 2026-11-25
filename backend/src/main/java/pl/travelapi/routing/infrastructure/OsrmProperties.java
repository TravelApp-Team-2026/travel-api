package pl.travelapi.routing.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSRM settings ({@code app.routing.osrm.*}).
 *
 * @param minRequestInterval minimal time between two requests (demo server: 1 request per second)
 * @param maxQueueWait how long a request may wait for its turn before it is skipped
 * @param maxSnapDistanceMeters how far OSRM may move a point to reach a road; a route that needs a
 *        bigger move is not used
 */
@ConfigurationProperties("app.routing.osrm")
public record OsrmProperties(
		String baseUrl,
		String userAgent,
		Duration minRequestInterval,
		Duration maxQueueWait,
		double maxSnapDistanceMeters) {

	public OsrmProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("app.routing.osrm.base-url must be set.");
		}
		if (userAgent == null || userAgent.isBlank()) {
			throw new IllegalStateException("app.routing.osrm.user-agent must be set (the server requires it).");
		}
		if (minRequestInterval == null || maxQueueWait == null) {
			throw new IllegalStateException(
					"app.routing.osrm.min-request-interval and max-queue-wait must be set.");
		}
		if (maxSnapDistanceMeters <= 0) {
			throw new IllegalStateException("app.routing.osrm.max-snap-distance-meters must be positive.");
		}
	}
}
