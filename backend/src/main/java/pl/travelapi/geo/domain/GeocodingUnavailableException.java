package pl.travelapi.geo.domain;

import pl.travelapi.shared.exception.ServiceUnavailableException;

/** The geocoding service cannot answer now (error, timeout or our own rate limit). */
public class GeocodingUnavailableException extends ServiceUnavailableException {

	public static final String UNAVAILABLE_MESSAGE =
			"Wyszukiwarka miejsc jest chwilowo niedostępna. Spróbuj ponownie później.";

	public static final String BUSY_MESSAGE =
			"Wyszukiwarka miejsc jest chwilowo zajęta. Spróbuj ponownie za chwilę.";

	public GeocodingUnavailableException(String message) {
		super(message);
	}
}
