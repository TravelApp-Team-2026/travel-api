package pl.travelapi.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Year;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.travelapi.shared.exception.InvalidRequestException;

class YearParameterTest {

	@Test
	void shouldReturnNullWhenMissingOrBlank() {
		assertThat(YearParameter.parse(null)).isNull();
		assertThat(YearParameter.parse("")).isNull();
		assertThat(YearParameter.parse("  ")).isNull();
	}

	@Test
	void shouldParseYearWithinRange() {
		assertThat(YearParameter.parse("2026")).isEqualTo(Year.of(2026));
		assertThat(YearParameter.parse(" 2026 ")).isEqualTo(Year.of(2026));
		assertThat(YearParameter.parse("1900")).isEqualTo(Year.of(1900));
		assertThat(YearParameter.parse("2100")).isEqualTo(Year.of(2100));
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "2026.5", "1899", "2101", "-2026", "99999999999" })
	void shouldRejectInvalidYear(String raw) {
		assertThatThrownBy(() -> YearParameter.parse(raw))
			.isInstanceOfSatisfying(InvalidRequestException.class, e -> {
				assertThat(e.field()).isEqualTo("year");
				assertThat(e.getMessage()).isEqualTo("Rok musi być liczbą od 1900 do 2100.");
			});
	}
}
