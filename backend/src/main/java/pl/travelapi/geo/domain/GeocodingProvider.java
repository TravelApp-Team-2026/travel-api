package pl.travelapi.geo.domain;

import java.util.List;

public interface GeocodingProvider {

	/**
	 * Searches places by free text.
	 *
	 * @return found places, best match first; empty when nothing was found
	 * @throws GeocodingUnavailableException when the provider cannot answer
	 */
	List<Place> search(String query);
}
