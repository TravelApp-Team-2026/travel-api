package pl.travelapi.geo.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import pl.travelapi.geo.domain.Place;

/** One element of the Nominatim {@code jsonv2} search response (only the fields we use). */
@JsonIgnoreProperties(ignoreUnknown = true)
record NominatimPlace(
		String name,
		@JsonProperty("display_name") String displayName,
		String lat,
		String lon,
		Address address) {

	private static final int MAX_PLACE_NAME_LENGTH = 200;
	private static final int MAX_COUNTRY_NAME_LENGTH = 100;
	private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Address(String country, @JsonProperty("country_code") String countryCode) {
	}

	/** Results without coordinates or country (e.g. open sea) are useless for a trip, so they are dropped. */
	Optional<Place> toPlace() {
		if (address == null || isBlank(address.country()) || isBlank(address.countryCode())) {
			return Optional.empty();
		}
		String countryCode = address.countryCode().strip().toUpperCase(Locale.ROOT);
		String placeName = placeName();
		Double latitude = parseCoordinate(lat);
		Double longitude = parseCoordinate(lon);
		if (!COUNTRY_CODE.matcher(countryCode).matches() || placeName == null
				|| latitude == null || longitude == null) {
			return Optional.empty();
		}
		String countryName = truncate(address.country().strip(), MAX_COUNTRY_NAME_LENGTH);
		return Optional.of(new Place(placeName, latitude, longitude, countryName, countryCode));
	}

	private String placeName() {
		String candidate = !isBlank(name) ? name : firstSegment(displayName);
		return isBlank(candidate) ? null : truncate(candidate.strip(), MAX_PLACE_NAME_LENGTH);
	}

	private static String firstSegment(String displayName) {
		return displayName == null ? null : displayName.split(",")[0];
	}

	private static Double parseCoordinate(String value) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return Double.valueOf(value.strip());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	private static String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
