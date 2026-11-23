package pl.travelapi.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * No format or length rules here on purpose: any wrong value simply ends in the same 401 as a wrong
 * password, so the login form does not reveal the registration rules.
 */
public record LoginRequest(

		@NotBlank(message = "E-mail jest wymagany.")
		String email,

		@NotBlank(message = "Hasło jest wymagane.")
		String password) {

	/** Keeps the password out of logs. */
	@Override
	public String toString() {
		return "LoginRequest[email=" + email + "]";
	}
}
