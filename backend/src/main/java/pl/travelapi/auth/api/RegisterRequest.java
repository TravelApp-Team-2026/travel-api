package pl.travelapi.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.travelapi.shared.validation.MaxUtf8Bytes;

public record RegisterRequest(

		@NotBlank(message = "E-mail jest wymagany.")
		@Email(message = "Podaj poprawny adres e-mail.")
		@Size(max = 150, message = "E-mail może mieć maksymalnie 150 znaków.")
		String email,

		@NotBlank(message = "Hasło jest wymagane.")
		@Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków.")
		@MaxUtf8Bytes(value = 72, message = "Hasło jest zbyt długie.")
		String password,

		@NotBlank(message = "Nazwa wyświetlana jest wymagana.")
		@Size(max = 50, message = "Nazwa wyświetlana może mieć maksymalnie 50 znaków.")
		String displayName) {

	/** Keeps the password out of logs. */
	@Override
	public String toString() {
		return "RegisterRequest[email=" + email + ", displayName=" + displayName + "]";
	}
}
