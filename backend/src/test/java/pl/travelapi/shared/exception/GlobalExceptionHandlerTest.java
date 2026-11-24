package pl.travelapi.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;

@WebMvcTest(GlobalExceptionHandlerTest.ErrorTestController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, RestSecurityExceptionHandler.class,
		GlobalExceptionHandlerTest.ErrorTestController.class })
class GlobalExceptionHandlerTest {

	private final MockMvc mockMvc;

	@Autowired
	GlobalExceptionHandlerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldReturn400WithFieldErrorsForInvalidBody() throws Exception {
		mockMvc.perform(post("/test-errors/validation")
				.with(user("tester"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Nieprawidłowe żądanie"))
			.andExpect(jsonPath("$.detail").value("Przesłane dane są nieprawidłowe."))
			.andExpect(jsonPath("$.fieldErrors.email").value("Podaj poprawny adres e-mail."))
			.andExpect(jsonPath("$.fieldErrors.password").value("Hasło musi mieć co najmniej 8 znaków."));
	}

	@Test
	void shouldReturn400ForMalformedJson() throws Exception {
		mockMvc.perform(post("/test-errors/validation")
				.with(user("tester"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("Nie można odczytać przesłanych danych. Sprawdź format żądania."));
	}

	@Test
	void shouldReturn401ForMissingToken() throws Exception {
		mockMvc.perform(get("/test-errors/secured"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"))
			.andExpect(jsonPath("$.detail").exists());
	}

	@Test
	void shouldReturn401WithCredentialsMessageForBadCredentials() throws Exception {
		mockMvc.perform(get("/test-errors/bad-credentials").with(user("tester")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło"));
	}

	@Test
	void shouldReturn403ForAccessDenied() throws Exception {
		mockMvc.perform(get("/test-errors/forbidden").with(user("tester")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Brak dostępu"));
	}

	@Test
	void shouldReturn404ForResourceNotFound() throws Exception {
		mockMvc.perform(get("/test-errors/not-found").with(user("tester")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Nie znaleziono"))
			.andExpect(jsonPath("$.detail").value("Podróż nie istnieje."));
	}

	@Test
	void shouldReturn409ForConflict() throws Exception {
		mockMvc.perform(get("/test-errors/conflict").with(user("tester")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.title").value("Konflikt"))
			.andExpect(jsonPath("$.detail").value("Ten adres e-mail jest już zajęty."));
	}

	@Test
	void shouldReturn400WithFieldErrorForInvalidRequestException() throws Exception {
		mockMvc.perform(get("/test-errors/invalid-request").with(user("tester")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Nieprawidłowe żądanie"))
			.andExpect(jsonPath("$.detail").value("Przesłane dane są nieprawidłowe."))
			.andExpect(jsonPath("$.fieldErrors.q").value("Wpisz nazwę miejsca."));
	}

	@Test
	void shouldReturn503ForServiceUnavailable() throws Exception {
		mockMvc.perform(get("/test-errors/unavailable").with(user("tester")))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.status").value(503))
			.andExpect(jsonPath("$.title").value("Usługa niedostępna"))
			.andExpect(jsonPath("$.detail").value("Usługa chwilowo niedostępna."));
	}

	@Test
	void shouldReturn500WithoutLeakingInternals() throws Exception {
		MvcResult result = mockMvc.perform(get("/test-errors/unexpected").with(user("tester")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.title").value("Błąd serwera"))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body)
			.doesNotContain("tajny szczegół")
			.doesNotContain("IllegalStateException")
			.doesNotContain("at pl.travelapi")
			.doesNotContain("stackTrace");
	}

	@Test
	void shouldUseMessageOfObjectLevelConstraintAsDetail() throws Exception {
		mockMvc.perform(post("/test-errors/range")
				.with(user("tester"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"from\":5,\"to\":1}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Data końca nie może być wcześniejsza niż data początku."))
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void shouldReturnPolishProblemForUnknownPath() throws Exception {
		mockMvc.perform(get("/test-errors/nonexistent").with(user("tester")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Nie znaleziono"))
			.andExpect(jsonPath("$.detail").value("Nie znaleziono żądanego zasobu."));
	}

	@Test
	void shouldReturnPolishProblemForWrongHttpMethod() throws Exception {
		mockMvc.perform(post("/test-errors/secured").with(user("tester")))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value(405))
			.andExpect(jsonPath("$.title").value("Metoda niedozwolona"))
			.andExpect(jsonPath("$.detail").value("Ta metoda HTTP nie jest obsługiwana dla tego adresu."));
	}

	@Test
	void shouldReturnPolishProblemForUnsupportedMediaType() throws Exception {
		mockMvc.perform(post("/test-errors/validation")
				.with(user("tester"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("tekst"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.status").value(415))
			.andExpect(jsonPath("$.title").value("Nieobsługiwany typ danych"));
	}

	record TestRequest(
			@NotBlank(message = "E-mail jest wymagany.") @Email(message = "Podaj poprawny adres e-mail.") String email,
			@NotBlank(message = "Hasło jest wymagane.")
			@Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków.") String password) {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = ValidRangeValidator.class)
	@interface ValidRange {

		String message() default "Data końca nie może być wcześniejsza niż data początku.";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}

	public static class ValidRangeValidator implements ConstraintValidator<ValidRange, RangeRequest> {

		@Override
		public boolean isValid(RangeRequest value, ConstraintValidatorContext context) {
			return value == null || value.from() <= value.to();
		}
	}

	@ValidRange
	record RangeRequest(int from, int to) {
	}

	@RestController
	@RequestMapping("/test-errors")
	static class ErrorTestController {

		@PostMapping("/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@PostMapping("/range")
		void range(@Valid @RequestBody RangeRequest request) {
		}

		@GetMapping("/secured")
		String secured() {
			return "ok";
		}

		@GetMapping("/bad-credentials")
		void badCredentials() {
			throw new BadCredentialsException("bad");
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new AccessDeniedException("denied");
		}

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Podróż nie istnieje.");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Ten adres e-mail jest już zajęty.");
		}

		@GetMapping("/invalid-request")
		void invalidRequest() {
			throw new InvalidRequestException("q", "Wpisz nazwę miejsca.");
		}

		@GetMapping("/unavailable")
		void unavailable() {
			throw new ServiceUnavailableException("Usługa chwilowo niedostępna.");
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("tajny szczegół");
		}
	}
}
