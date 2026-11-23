package pl.travelapi.auth.api;

import java.time.Instant;
import pl.travelapi.auth.application.AuthService.LoginResult;

/**
 * @param accessToken JWT to send as {@code Authorization: Bearer <accessToken>}
 * @param tokenType always {@code Bearer}
 * @param expiresAt when the token stops being valid; after that the user has to log in again
 * @param user the logged-in user, so the app does not need a separate request after login
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserSummary user) {

	public static LoginResponse from(LoginResult result) {
		return new LoginResponse(
				result.token().value(),
				"Bearer",
				result.token().expiresAt(),
				new UserSummary(result.user().getId(), result.user().getEmail(), result.user().getDisplayName()));
	}

	public record UserSummary(Long id, String email, String displayName) {
	}

	/** Keeps the token out of logs. */
	@Override
	public String toString() {
		return "LoginResponse[tokenType=" + tokenType + ", expiresAt=" + expiresAt + ", user=" + user + "]";
	}
}
