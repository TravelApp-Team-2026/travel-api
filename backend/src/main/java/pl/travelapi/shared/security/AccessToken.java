package pl.travelapi.shared.security;

import java.time.Instant;

/** A signed JWT and the moment it stops being valid. */
public record AccessToken(String value, Instant expiresAt) {

	/** Keeps the token out of logs. */
	@Override
	public String toString() {
		return "AccessToken[value=***, expiresAt=" + expiresAt + "]";
	}
}
