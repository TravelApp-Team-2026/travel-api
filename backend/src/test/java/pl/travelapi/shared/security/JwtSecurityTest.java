package pl.travelapi.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.exception.GlobalExceptionHandler;

@WebMvcTest(JwtSecurityTest.JwtTestController.class)
@Import({ SecurityConfig.class, RestSecurityExceptionHandler.class, GlobalExceptionHandler.class,
		JwtTokenService.class, JwtSecurityTest.JwtTestController.class })
class JwtSecurityTest {

	private static final String SECURED = "/api/v1/test-jwt/me";
	private static final String PUBLIC = "/api/v1/auth/test-public";

	private final MockMvc mockMvc;
	private final JwtTokenService tokenService;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;

	@Autowired
	JwtSecurityTest(MockMvc mockMvc, JwtTokenService tokenService, JwtEncoder jwtEncoder, JwtProperties properties) {
		this.mockMvc = mockMvc;
		this.tokenService = tokenService;
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	@Test
	void shouldAuthenticateWithValidTokenAndExposeUserId() throws Exception {
		String token = tokenService.issue(42L).value();

		mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(content().string("42"))
			// stateless: no session cookie is created
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@Test
	void shouldReturn401ProblemWithoutToken() throws Exception {
		mockMvc.perform(get(SECURED))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"));
	}

	@Test
	void shouldReturn401ProblemForMalformedToken() throws Exception {
		mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"));
	}

	@Test
	void shouldReturn401ForExpiredToken() throws Exception {
		Clock twoDaysAgo = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
		String expired = new JwtTokenService(jwtEncoder, properties, twoDaysAgo).issue(42L).value();

		mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void shouldReturn401ForTokenSignedWithAnotherSecret() throws Exception {
		JwtProperties other = new JwtProperties("another-secret-also-at-least-32-bytes!!", Duration.ofHours(1));
		String foreign = new JwtTokenService(new JwtConfig().jwtEncoder(other), other, Clock.systemUTC())
			.issue(42L)
			.value();

		mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(foreign)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldIgnoreInvalidTokenOnPublicPaths() throws Exception {
		mockMvc.perform(get(PUBLIC).header(HttpHeaders.AUTHORIZATION, bearer("expired-or-garbage")))
			.andExpect(status().isOk())
			.andExpect(content().string("public"));
	}

	@RestController
	static class JwtTestController {

		@GetMapping(SECURED)
		String me(@AuthenticationPrincipal Jwt jwt) {
			return Long.toString(JwtTokenService.userId(jwt));
		}

		@GetMapping(PUBLIC)
		String publicEndpoint() {
			return "public";
		}
	}
}
