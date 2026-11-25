package pl.travelapi.country.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.country.application.VisitedCountryService;
import pl.travelapi.shared.config.OpenApiConfig;
import pl.travelapi.shared.security.JwtTokenService;
import pl.travelapi.shared.validation.YearParameter;

@Tag(name = "Kraje", description = "Kraje odwiedzone przez zalogowanego użytkownika")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(
		responseCode = "401",
		description = "Brak tokena, token nieprawidłowy lub wygasł",
		content = @Content(mediaType = "application/problem+json",
				schema = @Schema(implementation = ProblemDetail.class)))
@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

	private final VisitedCountryService visitedCountryService;

	public CountryController(VisitedCountryService visitedCountryService) {
		this.visitedCountryService = visitedCountryService;
	}

	@Operation(
			summary = "Odwiedzone kraje",
			description = "Lista wyliczana automatycznie z przystanków podróży: kraj jest odwiedzony, gdy podróż ma "
					+ "w nim co najmniej jeden przystanek. `tripCount` to liczba różnych podróży z przystankiem w tym "
					+ "kraju (kilka przystanków w jednym kraju w tej samej podróży liczy się raz). Kolejność: najwięcej "
					+ "podróży na początku, przy remisie nazwa alfabetycznie (polski alfabet). Z parametrem `year` "
					+ "liczą się tylko podróże rozpoczęte w tym roku (według daty rozpoczęcia).")
	@ApiResponse(responseCode = "200", description = "Lista krajów (pusta, gdy brak podróży)")
	@ApiResponse(
			responseCode = "400",
			description = "Nieprawidłowy rok; komunikat w `fieldErrors.year`",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/visited")
	public List<VisitedCountryResponse> visited(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Rok rozpoczęcia podróży (" + YearParameter.MIN + "-" + YearParameter.MAX
					+ "); bez parametru cała historia",
					schema = @Schema(type = "integer", minimum = "1900", maximum = "2100"), example = "2026")
			@RequestParam(name = YearParameter.NAME, required = false) String year) {
		return visitedCountryService.findVisited(JwtTokenService.userId(jwt), YearParameter.parse(year))
			.stream()
			.map(VisitedCountryResponse::from)
			.toList();
	}
}
