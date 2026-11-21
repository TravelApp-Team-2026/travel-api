package pl.travelapi.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.auth.application.AuthService;

@Tag(name = "Auth", description = "Rejestracja i logowanie")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(
			summary = "Rejestracja nowego użytkownika",
			description = "Tworzy konto. E-mail jest zapisywany małymi literami, hasło jako hash BCrypt. "
					+ "Hasło: 8-64 znaki (i maksymalnie 72 bajty w UTF-8).")
	@ApiResponse(responseCode = "201", description = "Konto utworzone (odpowiedź nie zawiera hasła ani hasha)")
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
							  "instance": "/api/v1/auth/register",
							  "fieldErrors": { "email": "Podaj poprawny adres e-mail." }
							}
							""")))
	@ApiResponse(
			responseCode = "409",
			description = "Konto z tym adresem e-mail już istnieje",
			content = @Content(
					mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = """
							{
							  "type": "about:blank",
							  "title": "Konflikt",
							  "status": 409,
							  "detail": "Konto z tym adresem e-mail już istnieje.",
							  "instance": "/api/v1/auth/register"
							}
							""")))
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		return RegisterResponse.from(authService.register(request.email(), request.password(), request.displayName()));
	}
}
