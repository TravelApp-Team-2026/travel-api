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
		@Size(min = 8, max = 64, message = "Hasło musi mieć od 8 do 64 znaków.")
		// BCrypt hashes at most 72 bytes (UTF-8); polish letters and emoji take more than one byte per character
		@MaxUtf8Bytes(value = 72, message = "Hasło jest zbyt długie. Skróć je lub użyj mniej znaków specjalnych i polskich liter.")
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
