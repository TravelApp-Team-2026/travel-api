package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Body of {@code POST /api/v1/trips} and {@code PUT /api/v1/trips/{id}}. Stops and legs are numbered from 0
 * in list order; leg {@code i} connects stop {@code i} with stop {@code i + 1}.
 */
@ValidTripRequest
public record SaveTripRequest(

		@Schema(description = "Nazwa podróży (1-100 znaków).", example = "Europa 2026")
		@NotBlank(message = "Podaj nazwę podróży.")
		@Size(max = 100, message = "Nazwa podróży może mieć maksymalnie 100 znaków.")
		String title,

		@Schema(description = "Opis podróży (maks. 1000 znaków), może być pusty lub null.", nullable = true)
		@Size(max = 1000, message = "Opis podróży może mieć maksymalnie 1000 znaków.")
		String description,

		@Schema(description = "Data rozpoczęcia (RRRR-MM-DD).", example = "2026-05-01")
		@NotNull(message = "Podaj datę rozpoczęcia podróży.")
		LocalDate startDate,

		@Schema(description = "Data zakończenia (RRRR-MM-DD), nie wcześniejsza niż rozpoczęcia. Null: podróż jednodniowa.",
				nullable = true)
		LocalDate endDate,

		@Schema(description = "Przystanki w kolejności podróży: od 2 do 50.")
		@NotNull(message = "Dodaj co najmniej 2 przystanki.")
		@Size(min = 2, max = 50, message = "Podróż musi mieć co najmniej 2 przystanki i maksymalnie 50.")
		@Valid
		List<@NotNull(message = "Przystanek nie może być pusty.") WaypointRequest> waypoints,

		@Schema(description = "Odcinki w kolejności podróży; dokładnie o 1 mniej niż przystanków.")
		@NotNull(message = "Wybierz środek transportu dla każdego odcinka.")
		@Valid
		List<@NotNull(message = "Odcinek nie może być pusty.") SegmentRequest> segments) {
}
