package pl.travelapi.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
			.andExpect(jsonPath("$.fieldErrors.email").exists())
			.andExpect(jsonPath("$.fieldErrors.password").exists());
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
			.andExpect(jsonPath("$.detail").value("Nieprawidłowy adres e-mail lub hasło."));
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

	record TestRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password) {
	}

	@RestController
	@RequestMapping("/test-errors")
	static class ErrorTestController {

		@PostMapping("/validation")
		void validation(@Valid @RequestBody TestRequest request) {
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

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("tajny szczegół");
		}
	}
}
