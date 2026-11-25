package pl.travelapi.country.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.travelapi.country.domain.VisitedCountry;

public record VisitedCountryResponse(
		@Schema(description = "Kod kraju ISO 3166-1 alpha-2", example = "PL") String code,
		@Schema(description = "Nazwa kraju po polsku", example = "Polska") String name,
		@Schema(description = "Liczba różnych podróży z przystankiem w tym kraju", example = "3") long tripCount) {

	public static VisitedCountryResponse from(VisitedCountry country) {
		return new VisitedCountryResponse(country.code(), country.name(), country.tripCount());
	}
}
