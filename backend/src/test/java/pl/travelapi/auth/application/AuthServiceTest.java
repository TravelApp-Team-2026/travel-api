package pl.travelapi.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.travelapi.shared.exception.ConflictException;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

class AuthServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final AuthService authService = new AuthService(userRepository, passwordEncoder, clock);

	@Test
	void shouldHashPasswordAndNormalizeEmailAndName() {
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authService.register("  Anna@Example.COM ", "secret-password", "  Anna  ");

		assertThat(result.getEmail()).isEqualTo("anna@example.com");
		assertThat(result.getDisplayName()).isEqualTo("Anna");
		assertThat(result.getPasswordHash()).isNotEqualTo("secret-password");
		assertThat(passwordEncoder.matches("secret-password", result.getPasswordHash())).isTrue();
		assertThat(result.getCreatedAt()).isEqualTo(NOW);
		assertThat(result.isPublicProfile()).isFalse();
	}

	@Test
	void shouldRejectEmailThatIsAlreadyRegisteredRegardlessOfCase() {
		when(userRepository.existsByEmail("anna@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register("Anna@Example.com", "secret-password", "Anna"))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Konto z tym adresem e-mail już istnieje.");
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	void shouldMapUniqueConstraintViolationToConflict() {
		when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> authService.register("anna@example.com", "secret-password", "Anna"))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Konto z tym adresem e-mail już istnieje.");
	}
}
