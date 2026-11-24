package pl.travelapi.geo.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.travelapi.geo.domain.Place;

public record PlaceResponse(

		@Schema(description = "Nazwa miejsca (do 200 znaków).", example = "Kraków")
		String placeName,

		@Schema(description = "Szerokość geograficzna.", example = "50.0619474")
		double latitude,

		@Schema(description = "Długość geograficzna.", example = "19.9368564")
		double longitude,

		@Schema(description = "Nazwa kraju po polsku.", example = "Polska")
		String countryName,

		@Schema(description = "Kod kraju ISO 3166-1 alfa-2, wielkimi literami.", example = "PL")
		String countryCode) {

	public static PlaceResponse from(Place place) {
		return new PlaceResponse(place.name(), place.latitude(), place.longitude(), place.countryName(),
				place.countryCode());
	}
}
