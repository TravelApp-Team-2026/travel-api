package pl.travelapi.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.travelapi.user.domain.User;

@Testcontainers
@SpringBootTest
class UserRepositoryIntegrationTest {

	private static final String EMAIL = "anna@example.com";
	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@SuppressWarnings("resource")
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	private final UserRepository userRepository;

	@Autowired
	UserRepositoryIntegrationTest(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	@Test
	void shouldSaveUserAndFindItByEmail() {
		userRepository.saveAndFlush(User.create(EMAIL, "hash", "Anna", CREATED_AT));

		Optional<User> found = userRepository.findByEmail(EMAIL);

		assertThat(found).isPresent();
		assertThat(found.get().getId()).isNotNull();
		assertThat(found.get().getDisplayName()).isEqualTo("Anna");
		assertThat(found.get().getPasswordHash()).isEqualTo("hash");
		assertThat(found.get().getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldCreateUserWithPrivateProfileAndNoBioByDefault() {
		userRepository.saveAndFlush(User.create(EMAIL, "hash", "Anna", CREATED_AT));

		User found = userRepository.findByEmail(EMAIL).orElseThrow();

		assertThat(found.isPublicProfile()).isFalse();
		assertThat(found.getBio()).isNull();
	}

	@Test
	void shouldReturnEmptyWhenEmailDoesNotExist() {
		assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
	}

	@Test
	void shouldRejectDuplicateEmail() {
		userRepository.saveAndFlush(User.create(EMAIL, "hash", "Anna", CREATED_AT));

		assertThatThrownBy(() -> userRepository.saveAndFlush(User.create(EMAIL, "other", "Ania", CREATED_AT)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
