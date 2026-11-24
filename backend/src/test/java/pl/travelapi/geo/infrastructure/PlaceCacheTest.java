package pl.travelapi.geo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.support.MutableClock;

class PlaceCacheTest {

	private static final List<Place> PLACES = List.of(new Place("Kraków", 50.06, 19.94, "Polska", "PL"));

	private final MutableClock clock = new MutableClock();
	private final PlaceCache cache = new PlaceCache(clock, Duration.ofHours(1), 2);

	@Test
	void shouldReturnStoredPlaces() {
		cache.put("kraków", PLACES);

		assertThat(cache.get("kraków")).contains(PLACES);
	}

	@Test
	void shouldReturnEmptyForUnknownKey() {
		assertThat(cache.get("berlin")).isEmpty();
	}

	@Test
	void shouldStoreEmptyResultToo() {
		cache.put("nigdzie", List.of());

		assertThat(cache.get("nigdzie")).contains(List.of());
	}

	@Test
	void shouldForgetEntryAfterTtl() {
		cache.put("kraków", PLACES);
		clock.advance(Duration.ofHours(1));

		assertThat(cache.get("kraków")).isEmpty();
	}

	@Test
	void shouldDropExpiredEntriesFirstWhenFull() {
		cache.put("a", PLACES);
		clock.advance(Duration.ofMinutes(30));
		cache.put("b", PLACES);
		clock.advance(Duration.ofMinutes(31));

		cache.put("c", PLACES);

		assertThat(cache.get("a")).isEmpty();
		assertThat(cache.get("b")).contains(PLACES);
		assertThat(cache.get("c")).contains(PLACES);
	}

	@Test
	void shouldClearEverythingWhenFullOfFreshEntries() {
		cache.put("a", PLACES);
		cache.put("b", PLACES);

		cache.put("c", PLACES);

		assertThat(cache.get("a")).isEmpty();
		assertThat(cache.get("b")).isEmpty();
		assertThat(cache.get("c")).contains(PLACES);
	}
}
