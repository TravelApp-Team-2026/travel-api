package pl.travelapi.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.shared.config.OpenApiConfig;
import pl.travelapi.shared.security.JwtTokenService;
import pl.travelapi.user.application.UserProfileService;

@Tag(name = "Profil", description = "Profil zalogowanego użytkownika")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(
		responseCode = "401",
		description = "Brak tokena, token nieprawidłowy lub wygasł",
		content = @Content(
				mediaType = "application/problem+json",
				schema = @Schema(implementation = ProblemDetail.class),
				examples = @ExampleObject(value = """
						{
						  "type": "about:blank",
						  "title": "Brak autoryzacji",
						  "status": 401,
						  "detail": "Wymagane jest zalogowanie. Zaloguj się ponownie.",
						  "instance": "/api/v1/users/me"
						}
						""")))
@ApiResponse(
		responseCode = "404",
		description = "Konto z tokena już nie istnieje",
		content = @Content(
				mediaType = "application/problem+json",
				schema = @Schema(implementation = ProblemDetail.class),
				examples = @ExampleObject(value = """
						{
						  "type": "about:blank",
						  "title": "Nie znaleziono",
						  "status": 404,
						  "detail": "Nie znaleziono użytkownika.",
						  "instance": "/api/v1/users/me"
						}
						""")))
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

	private final UserProfileService userProfileService;

	public UserController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@Operation(
			summary = "Profil zalogowanego użytkownika",
			description = "Zwraca e-mail, nazwę wyświetlaną, opis „O mnie” (`bio`, może być null) "
					+ "i widoczność profilu (`isPublic`).")
	@ApiResponse(responseCode = "200", description = "Profil")
	@GetMapping
	public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
		return ProfileResponse.from(userProfileService.getProfile(JwtTokenService.userId(jwt)));
	}

	@Operation(
			summary = "Zmiana profilu",
			description = "Częściowa zmiana: pole pominięte lub `null` pozostaje bez zmian, więc można wysłać "
					+ "np. samo `isPublic`. Nazwa i opis są przycinane ze spacji; pusty `bio` usuwa opis. "
					+ "E-maila nie można tu zmienić. Zwraca profil po zmianie.")
	@ApiResponse(responseCode = "200", description = "Profil po zmianie")
	@ApiResponse(
			responseCode = "400",
			description = "Błędy walidacji; komunikaty dla poszczególnych pól w `fieldErrors`",
			content = @Content(
					mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = """
							{
							  "type": "about:blank",
							  "title": "Nieprawidłowe żądanie",
							  "status": 400,
							  "detail": "Przesłane dane są nieprawidłowe.",
							  "instance": "/api/v1/users/me",
							  "fieldErrors": { "displayName": "Nazwa wyświetlana nie może być pusta." }
							}
							""")))
	@PatchMapping
	public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ProfileResponse.from(userProfileService.updateProfile(JwtTokenService.userId(jwt),
				request.displayName(), request.bio(), request.isPublic()));
	}
}
