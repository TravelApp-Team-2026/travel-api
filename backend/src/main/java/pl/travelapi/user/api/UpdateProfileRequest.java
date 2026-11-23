package pl.travelapi.user.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: an omitted or {@code null} field is left unchanged. The e-mail cannot be changed here.
 */
public record UpdateProfileRequest(

		@Schema(description = "Nowa nazwa wyświetlana (1-50 znaków). Pominięta lub null: bez zmian.",
				example = "Anna", nullable = true)
		// at least one non-whitespace character; null is valid (= no change)
		@Pattern(regexp = "(?s).*\\S.*", message = "Nazwa wyświetlana nie może być pusta.")
		@Size(max = 50, message = "Nazwa wyświetlana może mieć maksymalnie 50 znaków.")
		String displayName,

		@Schema(description = "Opis „O mnie” (maks. 300 znaków). Pusty tekst usuwa opis. Pominięty lub null: bez zmian.",
				example = "Podróżuję głównie pociągiem.", nullable = true)
		@Size(max = 300, message = "Opis „O mnie” może mieć maksymalnie 300 znaków.")
		String bio,

		@Schema(description = "Czy profil jest publiczny. Pominięty lub null: bez zmian.", example = "true",
				nullable = true)
		Boolean isPublic) {
}
