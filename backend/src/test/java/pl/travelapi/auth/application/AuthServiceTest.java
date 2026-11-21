package pl.travelapi.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	private final AuthService authService = new AuthService(userRepository, passwordEncoder);

	@Test
	void shouldHashPasswordAndNormalizeEmailAndName() {
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authService.register("  Anna@Example.COM ", "secret-password", "  Anna  ");

		assertThat(result.getEmail()).isEqualTo("anna@example.com");
		assertThat(result.getDisplayName()).isEqualTo("Anna");
		assertThat(result.getPasswordHash()).isNotEqualTo("secret-password");
		assertThat(passwordEncoder.matches("secret-password", result.getPasswordHash())).isTrue();
		assertThat(result.getCreatedAt()).isNotNull();
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
