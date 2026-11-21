package pl.travelapi.auth.api;

import java.time.Instant;
import pl.travelapi.user.domain.User;

public record RegisterResponse(Long id, String email, String displayName, Instant createdAt) {

	public static RegisterResponse from(User user) {
		return new RegisterResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
	}
}
