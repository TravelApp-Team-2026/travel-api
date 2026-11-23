package pl.travelapi.shared.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues access tokens. The user id is stored in the standard {@code sub} claim; validation of incoming
 * tokens is done by the {@code JwtDecoder} from {@link JwtConfig}.
 */
@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;
	private final Clock clock;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.expiration = properties.expiration();
		this.clock = clock;
	}

	public AccessToken issue(long userId) {
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plus(expiration);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(Long.toString(userId))
			.issuedAt(now)
			.expiresAt(expiresAt)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(value, expiresAt);
	}

	/**
	 * Id of the logged-in user, for controllers: {@code @AuthenticationPrincipal Jwt jwt} then
	 * {@code JwtTokenService.userId(jwt)}.
	 */
	public static long userId(Jwt jwt) {
		return Long.parseLong(jwt.getSubject());
	}
}
