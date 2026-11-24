package pl.travelapi.geo.domain;

/**
 * A place found by geocoding.
 *
 * @param countryCode ISO 3166-1 alpha-2 code in upper case, e.g. {@code PL}
 */
public record Place(String name, double latitude, double longitude, String countryName, String countryCode) {
}
