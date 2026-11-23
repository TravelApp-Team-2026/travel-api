package pl.travelapi.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Service
public class UserProfileService {

	static final String USER_NOT_FOUND_MESSAGE = "Nie znaleziono użytkownika.";

	private final UserRepository userRepository;

	public UserProfileService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * @throws ResourceNotFoundException if the account no longer exists (the token can outlive it)
	 */
	@Transactional(readOnly = true)
	public User getProfile(long userId) {
		return findUser(userId);
	}

	/**
	 * Partial update: a {@code null} argument leaves the value unchanged. The display name and bio are
	 * trimmed; a blank bio clears it.
	 *
	 * @throws ResourceNotFoundException if the account no longer exists
	 */
	@Transactional
	public User updateProfile(long userId, String displayName, String bio, Boolean publicProfile) {
		User user = findUser(userId);
		user.updateProfile(
				displayName != null ? displayName.trim() : user.getDisplayName(),
				bio != null ? blankToNull(bio.trim()) : user.getBio(),
				publicProfile != null ? publicProfile : user.isPublicProfile());
		return user;
	}

	private User findUser(long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE));
	}

	private static String blankToNull(String value) {
		return value.isEmpty() ? null : value;
	}
}
