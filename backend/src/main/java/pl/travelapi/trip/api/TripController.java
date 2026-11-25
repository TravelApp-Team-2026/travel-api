package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.travelapi.shared.config.OpenApiConfig;
import pl.travelapi.shared.security.JwtTokenService;
import pl.travelapi.trip.application.TripService;

@Tag(name = "Podróże", description = "Podróże zalogowanego użytkownika")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(
		responseCode = "401",
		description = "Brak tokena, token nieprawidłowy lub wygasł",
		content = @Content(mediaType = "application/problem+json",
				schema = @Schema(implementation = ProblemDetail.class)))
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

	private static final String VALIDATION_ERRORS = "Błędy walidacji; komunikaty w `fieldErrors` (klucz to nazwa pola, "
			+ "np. `title`, `endDate`, `segments` albo `waypoints[0].placeName`)";

	private final TripService tripService;

	public TripController(TripService tripService) {
		this.tripService = tripService;
	}

	@Operation(
			summary = "Dodanie podróży",
			description = "Przystanki i odcinki są numerowane od 0 w kolejności z listy; odcinek `i` łączy przystanek `i` "
					+ "z `i + 1`. Podróż ma od 2 do 50 przystanków i dokładnie o 1 mniej odcinków. "
					+ "Serwer liczy długość i geometrię każdego odcinka. Odcinki CAR są liczone po kolei, najwyżej "
					+ "jeden na ok. 1,1 s, więc odpowiedź może przyjść po kilku sekundach, a przy wielu odcinkach "
					+ "CAR lub niedostępnej usłudze tras nawet po kilku minutach. Klient powinien ustawić długi "
					+ "limit czasu i nie wysyłać żądania ponownie, bo powstanie druga podróż. Niedostępna usługa "
					+ "tras nie blokuje zapisu: odcinek dostaje linię prostą.")
	@ApiResponse(responseCode = "201", description = "Podróż utworzona; nagłówek `Location` wskazuje jej adres")
	@ApiResponse(
			responseCode = "400",
			description = VALIDATION_ERRORS,
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping
	public ResponseEntity<TripResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody SaveTripRequest request) {
		TripResponse created = tripService.create(JwtTokenService.userId(jwt), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@Operation(
			summary = "Lista własnych podróży",
			description = "Od najnowszej (po dacie rozpoczęcia malejąco). Każdy element ma liczbę przystanków "
					+ "i całkowity dystans.")
	@ApiResponse(responseCode = "200", description = "Lista podróży (pusta, gdy brak)")
	@GetMapping
	public List<TripSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return tripService.list(JwtTokenService.userId(jwt));
	}

	@Operation(
			summary = "Szczegóły podróży",
			description = "Zwraca przystanki i odcinki w kolejności podróży; każdy odcinek ma długość `distanceKm` "
					+ "i linię `geometry` (lista punktów z polami `latitude` i `longitude`).")
	@ApiResponse(responseCode = "200", description = "Podróż")
	@ApiResponse(
			responseCode = "404",
			description = "Podróż nie istnieje albo należy do innego użytkownika",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/{id}")
	public TripResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
		return tripService.get(JwtTokenService.userId(jwt), id);
	}

	@Operation(
			summary = "Zastąpienie podróży",
			description = "Zastępuje szczegóły, przystanki i odcinki treścią żądania (jak przy dodawaniu). Odcinek "
					+ "o tych samych współrzędnych początku i końca oraz tym samym środku transportu zachowuje "
					+ "zapisaną trasę (także linię prostą); pozostałe są liczone od nowa, z tymi samymi czasami "
					+ "odpowiedzi co przy dodawaniu.")
	@ApiResponse(responseCode = "200", description = "Podróż po zmianie")
	@ApiResponse(
			responseCode = "400",
			description = VALIDATION_ERRORS,
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Podróż nie istnieje albo należy do innego użytkownika",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@PutMapping("/{id}")
	public TripResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
			@Valid @RequestBody SaveTripRequest request) {
		return tripService.update(JwtTokenService.userId(jwt), id, request);
	}

	@Operation(summary = "Usunięcie podróży", description = "Usuwa podróż razem z przystankami i odcinkami.")
	@ApiResponse(responseCode = "204", description = "Podróż usunięta")
	@ApiResponse(
			responseCode = "404",
			description = "Podróż nie istnieje albo należy do innego użytkownika",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
		tripService.delete(JwtTokenService.userId(jwt), id);
		return ResponseEntity.noContent().build();
	}
}
