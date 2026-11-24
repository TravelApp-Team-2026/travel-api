package pl.travelapi.geo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.geo.application.PlaceSearchService;
import pl.travelapi.shared.config.OpenApiConfig;

@Tag(name = "Miejsca", description = "Wyszukiwanie miejsc (geokodowanie)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(
		responseCode = "401",
		description = "Brak tokena, token nieprawidłowy lub wygasł",
		content = @Content(mediaType = "application/problem+json",
				schema = @Schema(implementation = ProblemDetail.class)))
@RestController
@RequestMapping("/api/v1/geo")
public class GeoController {

	private final PlaceSearchService placeSearchService;

	public GeoController(PlaceSearchService placeSearchService) {
		this.placeSearchService = placeSearchService;
	}

	@Operation(
			summary = "Wyszukiwanie miejsca",
			description = "Szuka miejsca po nazwie (dane OpenStreetMap, usługa Nominatim). Wywołuj dopiero po "
					+ "zatwierdzeniu frazy przez użytkownika, nie przy każdym wpisanym znaku (zasady Nominatim). "
					+ "Zwraca do 5 wyników, najlepszy pierwszy; pusta lista oznacza brak wyników. Pola odpowiadają "
					+ "przystankowi z `POST /api/v1/trips`. Powtórzone zapytanie jest brane z pamięci podręcznej "
					+ "(do 24 godzin). Dane © współtwórcy OpenStreetMap (licencja ODbL).")
	@ApiResponse(responseCode = "200", description = "Lista znalezionych miejsc (może być pusta)")
	@ApiResponse(
			responseCode = "400",
			description = "Pusta lub za długa fraza (maks. 200 znaków); komunikat w `fieldErrors.q`",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(
			responseCode = "503",
			description = "Usługa wyszukiwania niedostępna, przekroczono czas oczekiwania albo jest zbyt zajęta",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/search")
	public List<PlaceResponse> search(
			@Parameter(description = "Szukana fraza, np. nazwa miasta (1-200 znaków)", example = "Kraków")
			@RequestParam(name = "q", defaultValue = "") String q) {
		return placeSearchService.search(q).stream().map(PlaceResponse::from).toList();
	}
}
