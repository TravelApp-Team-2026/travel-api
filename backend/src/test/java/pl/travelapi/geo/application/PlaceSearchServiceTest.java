package pl.travelapi.geo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.travelapi.geo.domain.GeocodingProvider;
import pl.travelapi.geo.domain.GeocodingUnavailableException;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.geo.infrastructure.PlaceCache;
import pl.travelapi.shared.exception.InvalidRequestException;
import pl.travelapi.support.MutableClock;

class PlaceSearchServiceTest {

	private static final Place KRAKOW = new Place("Kraków", 50.06, 19.94, "Polska", "PL");

	private final GeocodingProvider provider = mock(GeocodingProvider.class);
	private final PlaceSearchService service =
			new PlaceSearchService(provider, new PlaceCache(new MutableClock(), Duration.ofHours(1), 10));

	@Test
	void shouldReturnPlacesFromProvider() {
		when(provider.search("Kraków")).thenReturn(List.of(KRAKOW));

		assertThat(service.search("Kraków")).containsExactly(KRAKOW);
	}

	@Test
	void shouldRejectBlankQueryWithoutCallingProvider() {
		assertThatThrownBy(() -> service.search("   "))
			.isInstanceOfSatisfying(InvalidRequestException.class, ex -> {
				assertThat(ex.field()).isEqualTo("q");
				assertThat(ex.getMessage()).isEqualTo("Wpisz nazwę miejsca.");
			});
		assertThatThrownBy(() -> service.search(null)).isInstanceOf(InvalidRequestException.class);

		verifyNoInteractions(provider);
	}

	@Test
	void shouldRejectTooLongQueryWithoutCallingProvider() {
		assertThatThrownBy(() -> service.search("a".repeat(201)))
			.isInstanceOfSatisfying(InvalidRequestException.class,
					ex -> assertThat(ex.getMessage()).isEqualTo("Nazwa miejsca może mieć maksymalnie 200 znaków."));

		verifyNoInteractions(provider);
	}

	@Test
	void shouldAcceptQueryOfExactly200Characters() {
		String query = "a".repeat(200);
		when(provider.search(query)).thenReturn(List.of());

		assertThat(service.search(query)).isEmpty();
	}

	@Test
	void shouldAskProviderOnceForRepeatedQueryDifferingOnlyInSpacesAndCase() {
		when(provider.search("Nowy Jork")).thenReturn(List.of(KRAKOW));

		service.search("  Nowy    Jork ");
		List<Place> second = service.search("nowy jork");

		assertThat(second).containsExactly(KRAKOW);
		verify(provider, times(1)).search("Nowy Jork");
	}

	@Test
	void shouldCacheEmptyResult() {
		when(provider.search("xyzxyz")).thenReturn(List.of());

		service.search("xyzxyz");
		service.search("xyzxyz");

		verify(provider, times(1)).search("xyzxyz");
	}

	@Test
	void shouldNotCacheFailure() {
		when(provider.search("Kraków"))
			.thenThrow(new GeocodingUnavailableException(GeocodingUnavailableException.UNAVAILABLE_MESSAGE))
			.thenReturn(List.of(KRAKOW));

		assertThatThrownBy(() -> service.search("Kraków")).isInstanceOf(GeocodingUnavailableException.class);
		assertThat(service.search("Kraków")).containsExactly(KRAKOW);

		verify(provider, times(2)).search("Kraków");
	}
}
