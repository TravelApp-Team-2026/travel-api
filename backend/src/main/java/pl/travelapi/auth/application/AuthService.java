package pl.travelapi.auth.application;

import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.travelapi.shared.exception.ConflictException;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Service
public class AuthService {

	static final String EMAIL_TAKEN_MESSAGE = "Konto z tym adresem e-mail już istnieje.";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Creates an account. The e-mail is stored trimmed and lower-cased, so addresses differing
	 * only in letter case are the same account.
	 *
	 * @throws ConflictException if the e-mail is already registered
	 */
	public User register(String email, String rawPassword, String displayName) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ConflictException(EMAIL_TAKEN_MESSAGE);
		}

		User user = User.create(normalizedEmail, passwordEncoder.encode(rawPassword), displayName.trim(),
				Instant.now());
		try {
			return userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException e) {
			// two concurrent registrations passed the check above; the unique constraint caught the second
			throw new ConflictException(EMAIL_TAKEN_MESSAGE);
		}
	}
}
