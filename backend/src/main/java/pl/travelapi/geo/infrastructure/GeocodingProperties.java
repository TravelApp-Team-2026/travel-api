package pl.travelapi.geo.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Geocoding settings ({@code app.geocoding.*}).
 *
 * @param minRequestInterval minimal time between two requests to the provider (Nominatim: 1 request per second)
 * @param maxQueueWait how long a search may wait for its turn before it is rejected
 * @param cacheTtl how long a result of an identical query is reused
 */
@ConfigurationProperties("app.geocoding")
public record GeocodingProperties(
		String baseUrl,
		String userAgent,
		Duration minRequestInterval,
		Duration maxQueueWait,
		Duration cacheTtl,
		int cacheMaxEntries,
		int resultLimit) {

	public GeocodingProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("app.geocoding.base-url must be set.");
		}
		if (userAgent == null || userAgent.isBlank()) {
			throw new IllegalStateException("app.geocoding.user-agent must be set (the provider requires it).");
		}
	}
}
