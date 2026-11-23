package pl.travelapi.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	private static final String VALID_SECRET = "a".repeat(JwtProperties.MIN_SECRET_BYTES);

	@Test
	void shouldRequireSecret() {
		assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("JWT_SECRET");
		assertThatThrownBy(() -> new JwtProperties("   ", Duration.ofHours(1)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void shouldRequireAtLeast32Bytes() {
		assertThatThrownBy(() -> new JwtProperties("a".repeat(31), Duration.ofHours(1)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("too short");
		assertThat(new JwtProperties(VALID_SECRET, Duration.ofHours(1)).secret()).isEqualTo(VALID_SECRET);
	}

	@Test
	void shouldDefaultExpirationAndRejectNonPositive() {
		assertThat(new JwtProperties(VALID_SECRET, null).expiration()).isEqualTo(JwtProperties.DEFAULT_EXPIRATION);
		assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, Duration.ZERO))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, Duration.ofMinutes(-5)))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldNotExposeSecretInToString() {
		assertThat(new JwtProperties(VALID_SECRET, Duration.ofHours(1)).toString()).doesNotContain(VALID_SECRET);
	}
}
