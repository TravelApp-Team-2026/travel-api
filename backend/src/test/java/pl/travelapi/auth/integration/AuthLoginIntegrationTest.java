package pl.travelapi.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.user.infrastructure.UserRepository;

@IntegrationTest
class AuthLoginIntegrationTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	/** Any path outside /api/v1/auth needs a token. */
	private static final String PROTECTED_URL = "/api/v1/users/me";

	private final MockMvc mockMvc;
	private final UserRepository userRepository;
	private final JwtDecoder jwtDecoder;

	@Autowired
	AuthLoginIntegrationTest(MockMvc mockMvc, UserRepository userRepository, JwtDecoder jwtDecoder) {
		this.mockMvc = mockMvc;
		this.userRepository = userRepository;
		this.jwtDecoder = jwtDecoder;
	}

	@BeforeEach
	void registerUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"anna@example.com\",\"password\":\"secret-password\",\"displayName\":\"Anna\"}"))
			.andExpect(status().isCreated());
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	private static String body(String email, String password) {
		return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
	}

	@Test
	void shouldLogInAndAccessProtectedPathWithReturnedToken() throws Exception {
		String response = mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("Anna@Example.com", "secret-password")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.email").value("anna@example.com"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(response, "$.accessToken");

		// a JWT signed by this application, issued for this user, valid until the returned expiresAt
		long userId = userRepository.findByEmail("anna@example.com").orElseThrow().getId();
		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(Long.toString(userId));
		// the exp claim has whole seconds only
		assertThat(jwt.getExpiresAt())
			.isEqualTo(Instant.parse(JsonPath.<String>read(response, "$.expiresAt")).truncatedTo(ChronoUnit.SECONDS));
		assertThat(((Number) JsonPath.read(response, "$.user.id")).longValue()).isEqualTo(userId);

		// without a token: 401; with it: the profile of the logged-in user
		mockMvc.perform(get(PROTECTED_URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("anna@example.com"));
	}

	@Test
	void shouldReturnIdenticalErrorForWrongPasswordAndUnknownEmail() throws Exception {
		String wrongPassword = mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "wrong-password")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String unknownEmail = mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("nobody@example.com", "secret-password")))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(unknownEmail).isEqualTo(wrongPassword);
	}

	@Test
	void shouldLogInEvenWhenAppSendsAnOldInvalidToken() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "secret-password")))
			.andExpect(status().isOk());
	}
}
