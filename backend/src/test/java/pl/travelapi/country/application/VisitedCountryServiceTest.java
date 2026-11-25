package pl.travelapi.country.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.travelapi.country.domain.VisitedCountry;
import pl.travelapi.country.infrastructure.VisitedCountryRepository;

class VisitedCountryServiceTest {

	private static final long USER_ID = 7L;

	private final VisitedCountryRepository repository = mock(VisitedCountryRepository.class);
	private final VisitedCountryService service = new VisitedCountryService(repository);

	@Test
	void shouldUseWholeHistoryWithoutYear() {
		List<VisitedCountry> countries = List.of(new VisitedCountry("PL", "Polska", 3));
		when(repository.findByUserId(USER_ID)).thenReturn(countries);

		assertThat(service.findVisited(USER_ID, null)).isEqualTo(countries);
		verify(repository).findByUserId(USER_ID);
		verifyNoMoreInteractions(repository);
	}

	@Test
	void shouldLimitTripsToStartDatesInGivenYear() {
		when(repository.findByUserIdAndTripStartBetween(USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
			.thenReturn(List.of(new VisitedCountry("CZ", "Czechy", 1)));

		assertThat(service.findVisited(USER_ID, Year.of(2026)))
			.containsExactly(new VisitedCountry("CZ", "Czechy", 1));
	}

	@Test
	void shouldSortByTripCountThenByNameInPolishOrder() {
		when(repository.findByUserId(USER_ID)).thenReturn(List.of(
				new VisitedCountry("SE", "Szwecja", 1),
				new VisitedCountry("MT", "Malta", 1),
				new VisitedCountry("LV", "Łotwa", 1),
				new VisitedCountry("CZ", "Czechy", 2),
				new VisitedCountry("SK", "Słowacja", 1),
				new VisitedCountry("LT", "Litwa", 1),
				new VisitedCountry("PL", "Polska", 3)));

		assertThat(service.findVisited(USER_ID, null))
			.extracting(VisitedCountry::name)
			.containsExactly("Polska", "Czechy", "Litwa", "Łotwa", "Malta", "Słowacja", "Szwecja");
	}

	@Test
	void shouldReturnEmptyListWhenNoTrips() {
		when(repository.findByUserId(USER_ID)).thenReturn(List.of());

		assertThat(service.findVisited(USER_ID, null)).isEmpty();
	}
}
