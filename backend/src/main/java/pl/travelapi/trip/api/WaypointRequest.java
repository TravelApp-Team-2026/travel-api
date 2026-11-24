package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WaypointRequest(

		@Schema(description = "Nazwa miejsca (1-200 znaków), np. z wyszukiwarki miejsc.", example = "Kraków")
		@NotBlank(message = "Podaj nazwę miejsca.")
		@Size(max = 200, message = "Nazwa miejsca może mieć maksymalnie 200 znaków.")
		String placeName,

		@Schema(description = "Szerokość geograficzna, od -90 do 90.", example = "50.06")
		@NotNull(message = "Podaj szerokość geograficzną.")
		@DecimalMin(value = "-90", message = "Szerokość geograficzna musi być od -90 do 90.")
		@DecimalMax(value = "90", message = "Szerokość geograficzna musi być od -90 do 90.")
		Double latitude,

		@Schema(description = "Długość geograficzna, od -180 do 180.", example = "19.94")
		@NotNull(message = "Podaj długość geograficzną.")
		@DecimalMin(value = "-180", message = "Długość geograficzna musi być od -180 do 180.")
		@DecimalMax(value = "180", message = "Długość geograficzna musi być od -180 do 180.")
		Double longitude,

		@Schema(description = "Kod kraju ISO 3166-1 alfa-2, wielkimi literami.", example = "PL")
		@NotNull(message = "Podaj kod kraju.")
		@Pattern(regexp = "[A-Z]{2}", message = "Kod kraju to dwie wielkie litery, np. PL.")
		String countryCode,

		@Schema(description = "Nazwa kraju (1-100 znaków).", example = "Polska")
		@NotBlank(message = "Podaj nazwę kraju.")
		@Size(max = 100, message = "Nazwa kraju może mieć maksymalnie 100 znaków.")
		String countryName) {
}
