package pl.travelapi.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokenServiceTest {

	private static final String SECRET = "unit-test-secret-at-least-32-bytes-long";
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, Duration.ofHours(24));

	private final JwtConfig config = new JwtConfig();
	private final JwtTokenService service = new JwtTokenService(config.jwtEncoder(PROPERTIES), PROPERTIES, at(NOW));

	private static Clock at(Instant instant) {
		return Clock.fixed(instant, ZoneOffset.UTC);
	}

	private JwtDecoder decoderAt(Instant instant) {
		return config.jwtDecoder(PROPERTIES, at(instant));
	}

	@Test
	void shouldIssueSignedTokenWithUserIdAndExpiration() {
		AccessToken token = service.issue(42L);

		assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		Jwt jwt = decoderAt(NOW.plusSeconds(60)).decode(token.value());
		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(JwtTokenService.userId(jwt)).isEqualTo(42L);
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
	}

	@Test
	void shouldRejectExpiredToken() {
		AccessToken token = service.issue(42L);

		// default allowed clock skew is 60 seconds
		assertThatThrownBy(() -> decoderAt(token.expiresAt().plusSeconds(120)).decode(token.value()))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRejectTokenSignedWithAnotherSecret() {
		JwtProperties other = new JwtProperties("another-secret-also-at-least-32-bytes!!", Duration.ofHours(24));
		AccessToken foreign = new JwtTokenService(config.jwtEncoder(other), other, at(NOW)).issue(42L);

		assertThatThrownBy(() -> decoderAt(NOW).decode(foreign.value())).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRejectTamperedToken() {
		String[] parts = service.issue(42L).value().split("\\.");
		// payload of a token issued for another user, glued to the original signature
		String otherPayload = service.issue(1L).value().split("\\.")[1];
		String tampered = parts[0] + "." + otherPayload + "." + parts[2];

		assertThatThrownBy(() -> decoderAt(NOW).decode(tampered)).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldRejectGarbage() {
		assertThatThrownBy(() -> decoderAt(NOW).decode("not-a-jwt")).isInstanceOf(JwtException.class);
	}

	@Test
	void shouldNotExposeTokenInToString() {
		AccessToken token = service.issue(42L);

		assertThat(token.toString()).doesNotContain(token.value());
	}
}
