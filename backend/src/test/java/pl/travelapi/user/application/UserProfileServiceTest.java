package pl.travelapi.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

class UserProfileServiceTest {

	private static final long USER_ID = 1L;

	private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserProfileService service = new UserProfileService(userRepository);

	private User user;

	@BeforeEach
	void setUp() {
		user = User.create("anna@example.com", "$2a$10$hash", "Anna", Instant.now(clock));
		ReflectionTestUtils.setField(user, "id", USER_ID);
		user.updateProfile("Anna", "Lubię góry.", false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
	}

	@Test
	void shouldReturnProfileOfExistingUser() {
		assertThat(service.getProfile(USER_ID)).isSameAs(user);
	}

	@Test
	void shouldThrowNotFoundWhenUserNoLongerExists() {
		when(userRepository.findById(2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getProfile(2L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Nie znaleziono użytkownika.");
		assertThatThrownBy(() -> service.updateProfile(2L, "Ola", null, null))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldUpdateAllFieldsAndTrimTexts() {
		User result = service.updateProfile(USER_ID, "  Anna Nowak ", "  Podróżuję pociągiem.  ", true);

		assertThat(result.getDisplayName()).isEqualTo("Anna Nowak");
		assertThat(result.getBio()).isEqualTo("Podróżuję pociągiem.");
		assertThat(result.isPublicProfile()).isTrue();
	}

	@Test
	void shouldLeaveNullFieldsUnchanged() {
		User result = service.updateProfile(USER_ID, null, null, true);

		assertThat(result.getDisplayName()).isEqualTo("Anna");
		assertThat(result.getBio()).isEqualTo("Lubię góry.");
		assertThat(result.isPublicProfile()).isTrue();
	}

	@Test
	void shouldChangeNothingWhenAllFieldsAreNull() {
		User result = service.updateProfile(USER_ID, null, null, null);

		assertThat(result.getDisplayName()).isEqualTo("Anna");
		assertThat(result.getBio()).isEqualTo("Lubię góry.");
		assertThat(result.isPublicProfile()).isFalse();
	}

	@Test
	void shouldClearBioWhenBlank() {
		User result = service.updateProfile(USER_ID, null, "   ", null);

		assertThat(result.getBio()).isNull();
	}
}
