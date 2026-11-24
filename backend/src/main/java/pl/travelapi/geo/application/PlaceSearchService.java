package pl.travelapi.geo.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pl.travelapi.geo.domain.GeocodingProvider;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.geo.infrastructure.PlaceCache;
import pl.travelapi.shared.exception.InvalidRequestException;

@Service
public class PlaceSearchService {

	static final int MAX_QUERY_LENGTH = 200;
	static final String QUERY_FIELD = "q";

	private final GeocodingProvider geocodingProvider;
	private final PlaceCache placeCache;

	public PlaceSearchService(GeocodingProvider geocodingProvider, PlaceCache placeCache) {
		this.geocodingProvider = geocodingProvider;
		this.placeCache = placeCache;
	}

	public List<Place> search(String rawQuery) {
		String query = normalize(rawQuery);
		validate(query);
		String cacheKey = query.toLowerCase(Locale.ROOT);
		Optional<List<Place>> cached = placeCache.get(cacheKey);
		if (cached.isPresent()) {
			return cached.get();
		}
		List<Place> places = geocodingProvider.search(query);
		placeCache.put(cacheKey, places);
		return places;
	}

	private static String normalize(String rawQuery) {
		return rawQuery == null ? "" : rawQuery.strip().replaceAll("\\s+", " ");
	}

	private static void validate(String query) {
		if (query.isEmpty()) {
			throw new InvalidRequestException(QUERY_FIELD, "Wpisz nazwę miejsca.");
		}
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new InvalidRequestException(QUERY_FIELD, "Nazwa miejsca może mieć maksymalnie 200 znaków.");
		}
	}
}
