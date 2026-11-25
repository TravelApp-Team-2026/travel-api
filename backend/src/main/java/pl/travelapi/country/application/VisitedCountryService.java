package pl.travelapi.country.application;

import java.text.Collator;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.travelapi.country.domain.VisitedCountry;
import pl.travelapi.country.infrastructure.VisitedCountryRepository;

@Service
public class VisitedCountryService {

	private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

	private final VisitedCountryRepository visitedCountryRepository;

	public VisitedCountryService(VisitedCountryRepository visitedCountryRepository) {
		this.visitedCountryRepository = visitedCountryRepository;
	}

	/**
	 * Countries visited by the user, most trips first, then by name in Polish alphabetical order.
	 *
	 * @param year only trips starting in this year; {@code null} means the whole history
	 */
	@Transactional(readOnly = true)
	public List<VisitedCountry> findVisited(long userId, Year year) {
		List<VisitedCountry> countries = year == null
				? visitedCountryRepository.findByUserId(userId)
				: visitedCountryRepository.findByUserIdAndTripStartBetween(userId, year.atDay(1),
						year.plusYears(1).atDay(1));
		Collator collator = Collator.getInstance(POLISH);
		return countries.stream()
			.sorted(Comparator.comparingLong(VisitedCountry::tripCount).reversed()
				.thenComparing(VisitedCountry::name, collator))
			.toList();
	}
}
