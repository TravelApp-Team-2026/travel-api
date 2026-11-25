package pl.travelapi.geo.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.travelapi.geo.domain.GeocodingProvider;
import pl.travelapi.geo.domain.GeocodingUnavailableException;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.shared.http.RequestSpacer;

/**
 * Geocoding through the public Nominatim server. Its usage policy requires an identifying
 * {@code User-Agent}, at most 1 request per second and no search-as-you-type.
 */
@Component
public class NominatimGeocodingProvider implements GeocodingProvider {

	private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingProvider.class);

	private static final ParameterizedTypeReference<List<NominatimPlace>> RESPONSE_TYPE =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;
	private final RequestSpacer requestSpacer;
	private final int resultLimit;

	public NominatimGeocodingProvider(RestClient.Builder builder, GeocodingProperties properties,
			RequestSpacer requestSpacer) {
		this.restClient = builder
			.baseUrl(properties.baseUrl())
			.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
			.build();
		this.requestSpacer = requestSpacer;
		this.resultLimit = properties.resultLimit();
	}

	@Override
	public List<Place> search(String query) {
		waitForTurn();
		return fetch(query).stream()
			.map(NominatimPlace::toPlace)
			.flatMap(Optional::stream)
			.toList();
	}

	private void waitForTurn() {
		try {
			if (!requestSpacer.acquire()) {
				log.warn("Geocoding request rejected: too many searches waiting for the provider");
				throw new GeocodingUnavailableException(GeocodingUnavailableException.BUSY_MESSAGE);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GeocodingUnavailableException(GeocodingUnavailableException.UNAVAILABLE_MESSAGE);
		}
	}

	private List<NominatimPlace> fetch(String query) {
		try {
			List<NominatimPlace> found = restClient.get()
				.uri(uri -> uri.path("/search")
					.queryParam("q", "{query}")
					.queryParam("format", "jsonv2")
					.queryParam("addressdetails", 1)
					.queryParam("accept-language", "pl")
					.queryParam("limit", resultLimit)
					.build(Map.of("query", query)))
				.retrieve()
				.body(RESPONSE_TYPE);
			return found == null ? List.of() : found;
		}
		catch (RestClientException e) {
			log.warn("Geocoding request failed: {}", e.getMessage());
			throw new GeocodingUnavailableException(GeocodingUnavailableException.UNAVAILABLE_MESSAGE);
		}
	}
}
