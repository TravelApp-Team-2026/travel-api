package pl.travelapi.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pl.travelapi.auth.application.AuthService.LoginResult;
import pl.travelapi.shared.exception.ConflictException;
import pl.travelapi.shared.security.AccessToken;
import pl.travelapi.shared.security.JwtTokenService;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

class AuthServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	// low cost keeps the tests fast; spy to check that a hash comparison always happens
	private final PasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder(4));
	private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtTokenService, clock);

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

	private User existingUser(String email, String rawPassword) {
		User user = User.create(email, passwordEncoder.encode(rawPassword), "Anna", NOW);
		ReflectionTestUtils.setField(user, "id", 7L);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		return user;
	}

	@Test
	void shouldLogInWithCorrectCredentialsAndIssueTokenForUserId() {
		User user = existingUser("anna@example.com", "secret-password");
		AccessToken token = new AccessToken("jwt-value", NOW.plusSeconds(3600));
		when(jwtTokenService.issue(7L)).thenReturn(token);

		LoginResult result = authService.login("  Anna@Example.COM ", "secret-password");

		assertThat(result.user()).isSameAs(user);
		assertThat(result.token()).isSameAs(token);
	}

	@Test
	void shouldGiveTheSameErrorForWrongPasswordAndUnknownEmail() {
		existingUser("anna@example.com", "secret-password");
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		Throwable wrongPassword = catchThrowable(
				() -> authService.login("anna@example.com", "wrong-password"));
		Throwable unknownEmail = catchThrowable(
				() -> authService.login("nobody@example.com", "secret-password"));

		assertThat(wrongPassword).isInstanceOf(BadCredentialsException.class);
		assertThat(unknownEmail).isInstanceOf(BadCredentialsException.class);
		assertThat(unknownEmail).hasMessage(wrongPassword.getMessage());
		verify(jwtTokenService, never()).issue(anyLong());
	}

	@Test
	void shouldCompareAHashEvenForUnknownEmail() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nobody@example.com", "secret-password"))
			.isInstanceOf(BadCredentialsException.class);
		verify(passwordEncoder, times(1)).matches(eq("secret-password"),
				any(String.class));
	}

	@Test
	void shouldRejectPasswordLongerThanBcryptLimitAsBadCredentials() {
		existingUser("anna@example.com", "secret-password");

		assertThatThrownBy(() -> authService.login("anna@example.com", "ą".repeat(37)))
			.isInstanceOf(BadCredentialsException.class);
	}
}
