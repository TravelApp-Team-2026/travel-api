package pl.travelapi.auth.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.travelapi.shared.exception.ConflictException;
import pl.travelapi.shared.security.AccessToken;
import pl.travelapi.shared.security.JwtTokenService;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Service
public class AuthService {

	static final String EMAIL_TAKEN_MESSAGE = "Konto z tym adresem e-mail już istnieje.";

	/** BCrypt compares at most 72 bytes; no account can have a longer password (see RegisterRequest). */
	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final Clock clock;

	/** Compared against when the e-mail is unknown, so that the response takes as long as for a wrong password. */
	private final String dummyPasswordHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService, Clock clock) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.clock = clock;
		this.dummyPasswordHash = passwordEncoder.encode("dummy-password-used-only-for-timing");
	}

	/**
	 * Creates an account. The e-mail is stored trimmed and lower-cased, so addresses differing
	 * only in letter case are the same account.
	 *
	 * @throws ConflictException if the e-mail is already registered
	 */
	public User register(String email, String rawPassword, String displayName) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ConflictException(EMAIL_TAKEN_MESSAGE);
		}

		User user = User.create(normalizedEmail, passwordEncoder.encode(rawPassword), displayName.trim(),
				Instant.now(clock));
		try {
			return userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException e) {
			// two concurrent registrations passed the check above; the unique constraint caught the second
			throw new ConflictException(EMAIL_TAKEN_MESSAGE);
		}
	}

	/**
	 * Checks the credentials and issues an access token. An unknown e-mail and a wrong password end in the
	 * same {@link BadCredentialsException} (rendered as the same 401), so the response does not reveal
	 * which e-mails have an account.
	 *
	 * @throws BadCredentialsException if the e-mail or the password is wrong
	 */
	public LoginResult login(String email, String rawPassword) {
		User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
		boolean passwordMatches = matches(rawPassword, user != null ? user.getPasswordHash() : dummyPasswordHash);
		if (user == null || !passwordMatches) {
			throw new BadCredentialsException("Bad credentials");
		}
		AccessToken token = jwtTokenService.issue(user.getId());
		return new LoginResult(user, token);
	}

	private boolean matches(String rawPassword, String passwordHash) {
		if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			// BCrypt would reject it; still spend the same time as a normal check
			passwordEncoder.matches("", dummyPasswordHash);
			return false;
		}
		return passwordEncoder.matches(rawPassword, passwordHash);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public record LoginResult(User user, AccessToken token) {
	}
}
