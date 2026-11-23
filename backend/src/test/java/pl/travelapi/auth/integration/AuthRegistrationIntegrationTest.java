package pl.travelapi.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@IntegrationTest
class AuthRegistrationIntegrationTest {

	private static final String REGISTER_URL = "/api/v1/auth/register";

	private final MockMvc mockMvc;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	AuthRegistrationIntegrationTest(MockMvc mockMvc, UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.mockMvc = mockMvc;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	private static String body(String email) {
		return "{\"email\":\"%s\",\"password\":\"secret-password\",\"displayName\":\"Anna\"}".formatted(email);
	}

	@Test
	void shouldRegisterUserAndStoreOnlyBcryptHash() throws Exception {
		mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body("Anna@Example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.email").value("anna@example.com"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		User saved = userRepository.findByEmail("anna@example.com").orElseThrow();
		assertThat(saved.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo("secret-password");
		assertThat(passwordEncoder.matches("secret-password", saved.getPasswordHash())).isTrue();
		assertThat(saved.isPublicProfile()).isFalse();
	}

	@Test
	void shouldReturn409ForExistingEmailRegardlessOfCase() throws Exception {
		mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body("anna@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body("ANNA@example.com")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Konto z tym adresem e-mail już istnieje."));

		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldReturn400WithPolishMessagesAndSaveNothingForInvalidData() throws Exception {
		mockMvc.perform(post(REGISTER_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nie-email\",\"password\":\"krotkie\",\"displayName\":\" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Nieprawidłowe żądanie"))
			.andExpect(jsonPath("$.fieldErrors.email").value("Podaj poprawny adres e-mail."))
			.andExpect(jsonPath("$.fieldErrors.password").value("Hasło musi mieć od 8 do 64 znaków."))
			.andExpect(jsonPath("$.fieldErrors.displayName").value("Nazwa wyświetlana jest wymagana."));

		assertThat(userRepository.count()).isZero();
	}

	@Test
	void shouldReturn400ForEmptyOrUnreadableBody() throws Exception {
		mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.email").value("E-mail jest wymagany."))
			.andExpect(jsonPath("$.fieldErrors.password").value("Hasło jest wymagane."));
		mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Nie można odczytać przesłanych danych. Sprawdź format żądania."));

		assertThat(userRepository.count()).isZero();
	}

	@Test
	void shouldDocumentRegisterEndpointInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.summary").exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['201']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['409']").exists());
	}
}
