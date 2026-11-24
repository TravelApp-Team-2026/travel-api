package pl.travelapi.geo.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import pl.travelapi.geo.domain.Place;

/** In-memory cache of search results with a time limit and a size limit. */
public class PlaceCache {

	private record Entry(List<Place> places, Instant expiresAt) {
	}

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private final Clock clock;
	private final Duration ttl;
	private final int maxEntries;

	public PlaceCache(Clock clock, Duration ttl, int maxEntries) {
		this.clock = clock;
		this.ttl = ttl;
		this.maxEntries = maxEntries;
	}

	public Optional<List<Place>> get(String key) {
		Entry entry = entries.get(key);
		if (entry == null) {
			return Optional.empty();
		}
		if (!entry.expiresAt().isAfter(clock.instant())) {
			entries.remove(key, entry);
			return Optional.empty();
		}
		return Optional.of(entry.places());
	}

	public void put(String key, List<Place> places) {
		if (entries.size() >= maxEntries) {
			makeRoom();
		}
		entries.put(key, new Entry(List.copyOf(places), clock.instant().plus(ttl)));
	}

	/** Drops expired entries; if the cache is still full, drops everything (simple and good enough). */
	private void makeRoom() {
		Instant now = clock.instant();
		entries.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
		if (entries.size() >= maxEntries) {
			entries.clear();
		}
	}
}
