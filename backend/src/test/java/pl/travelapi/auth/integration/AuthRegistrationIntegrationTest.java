package pl.travelapi.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistrationIntegrationTest {

	private static final String REGISTER_URL = "/api/v1/auth/register";

	@SuppressWarnings("resource")
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

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
		assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo("secret-password");
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
}
