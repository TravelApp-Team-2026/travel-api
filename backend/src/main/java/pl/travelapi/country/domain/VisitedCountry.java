package pl.travelapi.country.domain;

/**
 * A country with at least one stop of the user's trips; filled directly by a query.
 *
 * @param code ISO 3166-1 alpha-2 code in upper case, e.g. {@code PL}
 * @param tripCount number of different trips with a stop in this country
 */
public record VisitedCountry(String code, String name, long tripCount) {
}
