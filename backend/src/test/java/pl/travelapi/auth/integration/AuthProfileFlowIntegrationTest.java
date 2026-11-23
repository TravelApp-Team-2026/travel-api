package pl.travelapi.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

/**
 * The whole authentication flow as the mobile app uses it, step by step: registration, login,
 * access to a protected endpoint and a profile change that survives a new login.
 */
@IntegrationTest
class AuthProfileFlowIntegrationTest {

	private static final String REGISTER_URL = "/api/v1/auth/register";
	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String PROFILE_URL = "/api/v1/users/me";
	private static final String UNAUTHORIZED_DETAIL = "Wymagane jest zalogowanie. Zaloguj się ponownie.";

	private final MockMvc mockMvc;
	private final UserRepository userRepository;
	private final JwtEncoder jwtEncoder;
	private final Clock clock;

	@Autowired
	AuthProfileFlowIntegrationTest(MockMvc mockMvc, UserRepository userRepository, JwtEncoder jwtEncoder,
			Clock clock) {
		this.mockMvc = mockMvc;
		this.userRepository = userRepository;
		this.jwtEncoder = jwtEncoder;
		this.clock = clock;
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	@Test
	void shouldRegisterLogInAndKeepProfileChanges() throws Exception {
		// registration: invalid data 400, valid data 201, the same e-mail again 409
		register("{\"email\":\"nie-email\",\"password\":\"secret-password\",\"displayName\":\"Anna\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.email").value("Podaj poprawny adres e-mail."));
		register("{\"email\":\"anna@example.com\",\"password\":\"secret-password\",\"displayName\":\"Anna\"}")
			.andExpect(status().isCreated());
		register("{\"email\":\"anna@example.com\",\"password\":\"other-password\",\"displayName\":\"Ania\"}")
			.andExpect(status().isConflict());
		assertThat(userRepository.count()).isEqualTo(1);

		// login: wrong password and unknown e-mail 401, correct credentials give a token
		logIn("anna@example.com", "wrong-password")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło"));
		logIn("nobody@example.com", "secret-password")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło"));
		String token = token(logIn("anna@example.com", "secret-password").andExpect(status().isOk()));

		// protected endpoint: 401 without a token, 200 with it
		mockMvc.perform(get(PROFILE_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(UNAUTHORIZED_DETAIL));
		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("anna@example.com"))
			.andExpect(jsonPath("$.isPublic").value(false));

		// profile change, persisted: visible in the database and after a new login
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Anna Nowak\",\"bio\":\"Podróżuję pociągiem.\",\"isPublic\":true}"))
			.andExpect(status().isOk());

		User saved = userRepository.findByEmail("anna@example.com").orElseThrow();
		assertThat(saved.getDisplayName()).isEqualTo("Anna Nowak");
		assertThat(saved.getBio()).isEqualTo("Podróżuję pociągiem.");
		assertThat(saved.isPublicProfile()).isTrue();

		String newToken = token(logIn("anna@example.com", "secret-password")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.displayName").value("Anna Nowak")));
		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Anna Nowak"))
			.andExpect(jsonPath("$.bio").value("Podróżuję pociągiem."))
			.andExpect(jsonPath("$.isPublic").value(true));
	}

	@Test
	void shouldRejectExpiredForgedAndMalformedTokens() throws Exception {
		register("{\"email\":\"anna@example.com\",\"password\":\"secret-password\",\"displayName\":\"Anna\"}")
			.andExpect(status().isCreated());
		long userId = userRepository.findByEmail("anna@example.com").orElseThrow().getId();
		Instant now = Instant.now(clock);

		// correctly signed, but expired an hour ago
		String expired = sign(jwtEncoder, userId, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)));
		// valid times, but signed with a key the application does not know
		JwtEncoder otherKey = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
				"some-other-secret-that-is-long-enough-0123".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		String forged = sign(otherKey, userId, now, now.plus(Duration.ofHours(1)));
		// sanity check: the same claims signed with the application's key are accepted
		String valid = sign(jwtEncoder, userId, now, now.plus(Duration.ofHours(1)));

		for (String token : new String[] { expired, forged, "not-a-jwt" }) {
			mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Brak autoryzacji"))
				.andExpect(jsonPath("$.detail").value(UNAUTHORIZED_DETAIL));
		}
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Hacker\"}"))
			.andExpect(status().isUnauthorized());
		assertThat(userRepository.findByEmail("anna@example.com").orElseThrow().getDisplayName()).isEqualTo("Anna");

		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + valid))
			.andExpect(status().isOk());
	}

	private ResultActions register(String json) throws Exception {
		return mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions logIn(String email, String password) throws Exception {
		return mockMvc.perform(post(LOGIN_URL)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
	}

	private static String token(ResultActions loginResult) throws Exception {
		return JsonPath.read(loginResult.andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

	private static String sign(JwtEncoder encoder, long userId, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(Long.toString(userId))
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}
}
