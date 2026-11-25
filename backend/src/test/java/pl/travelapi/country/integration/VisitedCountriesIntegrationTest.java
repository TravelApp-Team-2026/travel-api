package pl.travelapi.country.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.trip.infrastructure.TripRepository;
import pl.travelapi.user.infrastructure.UserRepository;

@IntegrationTest
class VisitedCountriesIntegrationTest {

	private static final String VISITED_URL = "/api/v1/countries/visited";
	private static final String KRAKOW = place("Kraków", 50.06, 19.94, "PL", "Polska");
	private static final String WROCLAW = place("Wrocław", 51.11, 17.03, "PL", "Polska");
	private static final String BERLIN = place("Berlin", 52.52, 13.40, "DE", "Niemcy");
	private static final String PRAGUE = place("Praga", 50.08, 14.44, "CZ", "Czechy");
	private static final String BRNO = place("Brno", 49.20, 16.61, "CZ", "Czechy");
	private static final String VIENNA = place("Wiedeń", 48.21, 16.37, "AT", "Austria");
	private static final String PARIS = place("Paryż", 48.86, 2.35, "FR", "Francja");
	private static final String MADRID = place("Madryt", 40.42, -3.70, "ES", "Hiszpania");

	private final MockMvc mockMvc;
	private final TripRepository tripRepository;
	private final UserRepository userRepository;

	private String bearer;
	private String otherBearer;

	@Autowired
	VisitedCountriesIntegrationTest(MockMvc mockMvc, TripRepository tripRepository, UserRepository userRepository) {
		this.mockMvc = mockMvc;
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
	}

	@BeforeEach
	void registerUsers() throws Exception {
		bearer = "Bearer " + registerAndLogIn("anna-countries@example.com", "Anna");
		otherBearer = "Bearer " + registerAndLogIn("ola-countries@example.com", "Ola");
	}

	@AfterEach
	void cleanUp() {
		tripRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldReturnEmptyListWithoutTrips() throws Exception {
		mockMvc.perform(get(VISITED_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldCountEachTripOncePerCountryAndSortByTripsThenName() throws Exception {
		createAnnasTrips();

		mockMvc.perform(get(VISITED_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(Matchers.contains("Czechy", "Polska", "Austria", "Niemcy")))
			.andExpect(jsonPath("$[*].tripCount").value(Matchers.contains(2, 2, 1, 1)))
			.andExpect(jsonPath("$[0].code").value("CZ"))
			.andExpect(jsonPath("$[1].code").value("PL"));
	}

	@Test
	void shouldFilterByStartYearIncludingFirstAndLastDay() throws Exception {
		createAnnasTrips();

		mockMvc.perform(get(VISITED_URL).param("year", "2026").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(Matchers.contains("Czechy", "Austria", "Polska")))
			.andExpect(jsonPath("$[*].tripCount").value(Matchers.contains(2, 1, 1)));

		// a trip counts only in the year it starts in
		mockMvc.perform(get(VISITED_URL).param("year", "2025").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(Matchers.contains("Niemcy", "Polska")));

		mockMvc.perform(get(VISITED_URL).param("year", "2024").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldNotShowCountriesOfOtherUsers() throws Exception {
		createAnnasTrips();
		createTrip(otherBearer, body("Ola", "2026-03-01", List.of(PARIS, MADRID)));

		mockMvc.perform(get(VISITED_URL).header(HttpHeaders.AUTHORIZATION, otherBearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code").value(Matchers.contains("FR", "ES")));
		mockMvc.perform(get(VISITED_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code").value(Matchers.not(Matchers.hasItem("FR"))));
	}

	@Test
	void shouldRejectInvalidYear() throws Exception {
		mockMvc.perform(get(VISITED_URL).param("year", "1800").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.year").value("Rok musi być liczbą od 1900 do 2100."));
	}

	@Test
	void shouldRequireToken() throws Exception {
		mockMvc.perform(get(VISITED_URL)).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldDocumentEndpointInSwagger() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/countries/visited'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/countries/visited'].get.parameters[0].name").value("year"))
			.andExpect(jsonPath("$.paths['/api/v1/countries/visited'].get.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/countries/visited'].get.description")
				.value(Matchers.containsString("co najmniej jeden przystanek")))
			.andExpect(jsonPath("$.components.schemas.VisitedCountryResponse.properties.tripCount").exists());
	}

	/** Poland twice in one trip, Czechia in two trips, trips on the first and last day of 2026. */
	private void createAnnasTrips() throws Exception {
		createTrip(bearer, body("Sylwester", "2025-12-30", List.of(KRAKOW, BERLIN, WROCLAW)));
		createTrip(bearer, body("Praga", "2026-01-01", List.of(KRAKOW, PRAGUE)));
		createTrip(bearer, body("Morawy", "2026-12-31", List.of(VIENNA, BRNO)));
	}

	private void createTrip(String authorization, String body) throws Exception {
		mockMvc.perform(post("/api/v1/trips").header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated());
	}

	private String registerAndLogIn(String email, String displayName) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"secret-password\",\"displayName\":\"%s\"}"
					.formatted(email, displayName)))
			.andExpect(status().isCreated());
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"secret-password\"}".formatted(email)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}

	private static String place(String name, double latitude, double longitude, String countryCode,
			String countryName) {
		return "{\"placeName\":\"%s\",\"latitude\":%s,\"longitude\":%s,\"countryCode\":\"%s\",\"countryName\":\"%s\"}"
			.formatted(name, latitude, longitude, countryCode, countryName);
	}

	/** Train legs: no external routing service is involved when the trip is saved. */
	private static String body(String title, String startDate, List<String> waypoints) {
		String segments = waypoints.stream().skip(1)
			.map(waypoint -> "{\"transportMode\":\"TRAIN\"}")
			.collect(Collectors.joining(","));
		return "{\"title\":\"%s\",\"startDate\":\"%s\",\"waypoints\":[%s],\"segments\":[%s]}"
			.formatted(title, startDate, String.join(",", waypoints), segments);
	}
}
