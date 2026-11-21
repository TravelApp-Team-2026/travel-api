package pl.travelapi.auth.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pl.travelapi.auth.application.AuthService;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.exception.ConflictException;
import pl.travelapi.shared.exception.GlobalExceptionHandler;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;
import pl.travelapi.user.domain.User;

@WebMvcTest(AuthController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, RestSecurityExceptionHandler.class })
class AuthControllerTest {

	private static final String REGISTER_URL = "/api/v1/auth/register";

	private final MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Autowired
	AuthControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	private static User savedUser() {
		User user = User.create("anna@example.com", "$2a$10$hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));
		ReflectionTestUtils.setField(user, "id", 1L);
		return user;
	}

	private static String body(String email, String password, String displayName) {
		return "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\"}".formatted(email, password,
				displayName);
	}

	@Test
	void shouldReturn201WithoutPasswordOrHash() throws Exception {
		when(authService.register("anna@example.com", "secret-password", "Anna")).thenReturn(savedUser());

		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "secret-password", "Anna")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.email").value("anna@example.com"))
			.andExpect(jsonPath("$.displayName").value("Anna"))
			.andExpect(jsonPath("$.createdAt").exists())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		verify(authService).register("anna@example.com", "secret-password", "Anna");
	}

	@Test
	void shouldReturn409WhenEmailIsTaken() throws Exception {
		when(authService.register(anyString(), anyString(), anyString()))
			.thenThrow(new ConflictException("Konto z tym adresem e-mail już istnieje."));

		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "secret-password", "Anna")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Konto z tym adresem e-mail już istnieje."));
	}

	@Test
	void shouldReturn400WithPolishFieldErrorsForInvalidData() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("nie-email", "krotkie", " ")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.email").value("Podaj poprawny adres e-mail."))
			.andExpect(jsonPath("$.fieldErrors.password").value("Hasło musi mieć od 8 do 64 znaków."))
			.andExpect(jsonPath("$.fieldErrors.displayName").value("Nazwa wyświetlana jest wymagana."));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldReturn400ForEmptyBody() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.email").exists())
			.andExpect(jsonPath("$.fieldErrors.password").exists())
			.andExpect(jsonPath("$.fieldErrors.displayName").exists());

		verifyNoInteractions(authService);
	}

	@Test
	void shouldRejectTooLongDisplayName() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "secret-password", "n".repeat(51))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.displayName")
				.value("Nazwa wyświetlana może mieć maksymalnie 50 znaków."));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldAcceptPasswordOfExactly64Characters() throws Exception {
		when(authService.register(anyString(), anyString(), anyString())).thenReturn(savedUser());

		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "a".repeat(64), "Anna")))
			.andExpect(status().isCreated());
	}

	@Test
	void shouldRejectPasswordLongerThan64Characters() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "a".repeat(65), "Anna")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password").value("Hasło musi mieć od 8 do 64 znaków."));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldAcceptPasswordOfExactly72BytesEvenWithPolishLetters() throws Exception {
		when(authService.register(anyString(), anyString(), anyString())).thenReturn(savedUser());

		// 36 characters x 2 bytes = 72 bytes in UTF-8
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "ą".repeat(36), "Anna")))
			.andExpect(status().isCreated());
	}

	@Test
	void shouldRejectPasswordOver72BytesInsteadOfFailingInBcrypt() throws Exception {
		// 37 characters (within the 64 limit) but 74 bytes in UTF-8
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "ą".repeat(37), "Anna")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password")
				.value("Hasło jest zbyt długie. Skróć je lub użyj mniej znaków specjalnych i polskich liter."));

		verifyNoInteractions(authService);
	}
}
