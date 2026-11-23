package pl.travelapi.user.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.travelapi.user.domain.User;

/**
 * @param bio {@code null} when the user has not written anything
 * @param isPublic whether other users can see the profile, trips and statistics (the e-mail is never shown)
 */
public record ProfileResponse(
		@Schema(example = "1") Long id,
		@Schema(example = "anna@example.com") String email,
		@Schema(example = "Anna") String displayName,
		@Schema(example = "Podróżuję głównie pociągiem.", nullable = true) String bio,
		@Schema(example = "false") boolean isPublic) {

	public static ProfileResponse from(User user) {
		return new ProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getBio(),
				user.isPublicProfile());
	}
}
