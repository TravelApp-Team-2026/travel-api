package pl.travelapi.shared.validation;

import java.time.Year;
import pl.travelapi.shared.exception.InvalidRequestException;

/** The optional {@code ?year=YYYY} filter of statistics endpoints. */
public final class YearParameter {

	public static final String NAME = "year";
	public static final int MIN = 1900;
	public static final int MAX = 2100;
	public static final String INVALID_MESSAGE = "Rok musi być liczbą od " + MIN + " do " + MAX + ".";

	private YearParameter() {
	}

	/**
	 * @return the year, or {@code null} when the parameter is missing or blank (no filter)
	 * @throws InvalidRequestException when the value is not a whole number from {@value #MIN} to {@value #MAX}
	 */
	public static Year parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		int value;
		try {
			value = Integer.parseInt(raw.strip());
		}
		catch (NumberFormatException e) {
			throw new InvalidRequestException(NAME, INVALID_MESSAGE);
		}
		if (value < MIN || value > MAX) {
			throw new InvalidRequestException(NAME, INVALID_MESSAGE);
		}
		return Year.of(value);
	}
}
