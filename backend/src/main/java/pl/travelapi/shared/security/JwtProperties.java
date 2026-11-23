package pl.travelapi.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings ({@code app.jwt.*}). The application refuses to start without a secret, so a missing
 * {@code JWT_SECRET} is noticed immediately instead of tokens being signed with an empty or default key.
 *
 * @param secret HMAC key for signing tokens (HS256), at least {@value #MIN_SECRET_BYTES} bytes
 * @param expiration lifetime of an access token
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiration) {

	/** HS256 needs a key of at least 256 bits. */
	public static final int MIN_SECRET_BYTES = 32;

	public static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"JWT secret is not set. Set JWT_SECRET in .env (see .env.example) or as an environment variable.");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT secret is too short. JWT_SECRET must have at least " + MIN_SECRET_BYTES + " bytes.");
		}
		if (expiration == null) {
			expiration = DEFAULT_EXPIRATION;
		}
		if (expiration.isZero() || expiration.isNegative()) {
			throw new IllegalStateException("JWT expiration must be positive.");
		}
	}

	/** Keeps the secret out of logs and error messages. */
	@Override
	public String toString() {
		return "JwtProperties[secret=***, expiration=" + expiration + "]";
	}
}
