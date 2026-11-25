package pl.travelapi.trip.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.travelapi.support.IntegrationTest;
import pl.travelapi.trip.infrastructure.TripRepository;
import pl.travelapi.user.infrastructure.UserRepository;

@IntegrationTest
class TripCrudIntegrationTest {

	private static final String TRIPS_URL = "/api/v1/trips";
	private static final String KRAKOW = place("Kraków", 50.06, 19.94, "PL", "Polska");
	private static final String BERLIN = place("Berlin", 52.52, 13.40, "DE", "Niemcy");
	private static final String PARIS = place("Paryż", 48.86, 2.35, "FR", "Francja");

	private final MockMvc mockMvc;
	private final TripRepository tripRepository;
	private final UserRepository userRepository;

	private String bearer;
	private String otherBearer;

	@Autowired
	TripCrudIntegrationTest(MockMvc mockMvc, TripRepository tripRepository, UserRepository userRepository) {
		this.mockMvc = mockMvc;
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
	}

	@BeforeEach
	void registerUsers() throws Exception {
		bearer = "Bearer " + registerAndLogIn("anna-trips@example.com", "Anna");
		otherBearer = "Bearer " + registerAndLogIn("ola-trips@example.com", "Ola");
	}

	@AfterEach
	void cleanUp() {
		tripRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldCreateTripAndReturnItWithStopsAndLegs() throws Exception {
		MvcResult created = mockMvc.perform(post(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("Europa 2026", "2026-05-01", List.of(KRAKOW, BERLIN, PARIS), List.of("TRAIN", "PLANE"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("Europa 2026"))
			.andExpect(jsonPath("$.startDate").value("2026-05-01"))
			.andExpect(jsonPath("$.endDate").value(nullValue()))
			.andExpect(jsonPath("$.totalDistanceKm").isNumber())
			.andReturn();
		long id = idOf(created);
		assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(TRIPS_URL + "/" + id);

		mockMvc.perform(get(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.waypoints.length()").value(3))
			.andExpect(jsonPath("$.waypoints[1].order").value(1))
			.andExpect(jsonPath("$.waypoints[1].placeName").value("Berlin"))
			.andExpect(jsonPath("$.waypoints[2].countryCode").value("FR"))
			.andExpect(jsonPath("$.segments.length()").value(2))
			.andExpect(jsonPath("$.segments[0].transportMode").value("TRAIN"))
			.andExpect(jsonPath("$.segments[1].transportMode").value("PLANE"))
			.andExpect(jsonPath("$.segments[1].order").value(1));
	}

	@Test
	void shouldSaveAndReturnLegDistancesGeometryAndTotal() throws Exception {
		long id = createTrip(bearer,
				body("Trasy", "2026-05-01", List.of(KRAKOW, BERLIN, PARIS), List.of("CAR", "PLANE")));

		String details = mockMvc.perform(get(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			// OSRM is unreachable in tests, so the car leg is a straight line.
			.andExpect(jsonPath("$.segments[0].geometry.length()").value(2))
			.andExpect(jsonPath("$.segments[0].geometry[0].latitude").value(50.06))
			.andExpect(jsonPath("$.segments[0].geometry[0].longitude").value(19.94))
			.andExpect(jsonPath("$.segments[0].geometry[1].latitude").value(52.52))
			.andExpect(jsonPath("$.segments[1].geometry.length()").value(greaterThan(2)))
			.andReturn()
			.getResponse()
			.getContentAsString();

		BigDecimal firstLeg = decimalAt(details, "$.segments[0].distanceKm");
		BigDecimal secondLeg = decimalAt(details, "$.segments[1].distanceKm");
		assertThat(firstLeg).isPositive();
		assertThat(secondLeg).isPositive();
		assertThat(decimalAt(details, "$.totalDistanceKm")).isEqualByComparingTo(firstLeg.add(secondLeg));

		String list = mockMvc.perform(get(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(decimalAt(list, "$[0].totalDistanceKm")).isEqualByComparingTo(firstLeg.add(secondLeg));
	}

	@Test
	void shouldListOnlyOwnTripsNewestFirst() throws Exception {
		createTrip(bearer, body("Older", "2026-01-10", List.of(KRAKOW, BERLIN), List.of("CAR")));
		createTrip(bearer, body("Newer", "2026-06-01", List.of(KRAKOW, BERLIN, PARIS), List.of("CAR", "CAR")));
		createTrip(otherBearer, body("Not mine", "2026-07-01", List.of(KRAKOW, BERLIN), List.of("CAR")));

		mockMvc.perform(get(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].title").value("Newer"))
			.andExpect(jsonPath("$[0].stopCount").value(3))
			.andExpect(jsonPath("$[0].totalDistanceKm").isNumber())
			.andExpect(jsonPath("$[1].title").value("Older"))
			.andExpect(jsonPath("$[1].stopCount").value(2));
	}

	@Test
	void shouldReturnEmptyListWhenUserHasNoTrips() throws Exception {
		mockMvc.perform(get(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldReplaceDetailsStopsAndLegsOnPutEvenWhenOrderNumbersRepeat() throws Exception {
		long id = createTrip(bearer, body("Before", "2026-05-01", List.of(KRAKOW, BERLIN), List.of("TRAIN")));

		mockMvc.perform(put(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("After", "2026-05-02", List.of(PARIS, KRAKOW, BERLIN), List.of("CAR", "PLANE"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("After"));

		mockMvc.perform(get(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("After"))
			.andExpect(jsonPath("$.startDate").value("2026-05-02"))
			.andExpect(jsonPath("$.waypoints.length()").value(3))
			.andExpect(jsonPath("$.waypoints[0].placeName").value("Paryż"))
			.andExpect(jsonPath("$.segments.length()").value(2))
			.andExpect(jsonPath("$.segments[0].transportMode").value("CAR"))
			.andExpect(jsonPath("$.segments[0].geometry.length()").value(2))
			.andExpect(jsonPath("$.totalDistanceKm").value(greaterThan(0.0)));
	}

	@Test
	void shouldReturn404ForAnotherUsersTripOnEveryOperation() throws Exception {
		long id = createTrip(bearer, body("Private", "2026-05-01", List.of(KRAKOW, BERLIN), List.of("CAR")));
		String url = TRIPS_URL + "/" + id;

		mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, otherBearer))
			.andExpect(status().isNotFound());
		mockMvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, otherBearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("Hijacked", "2026-05-01", List.of(KRAKOW, BERLIN), List.of("CAR"))))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, otherBearer))
			.andExpect(status().isNotFound());

		mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Private"));
	}

	@Test
	void shouldReturn404ForMissingTrip() throws Exception {
		mockMvc.perform(get(TRIPS_URL + "/999999").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Nie znaleziono podróży."));
	}

	@Test
	void shouldDeleteTrip() throws Exception {
		long id = createTrip(bearer, body("To delete", "2026-05-01", List.of(KRAKOW, BERLIN), List.of("CAR")));

		mockMvc.perform(delete(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isNotFound());
		assertThat(tripRepository.count()).isZero();
	}

	@Test
	void shouldRejectInvalidTripWithFieldErrorsAndSaveNothing() throws Exception {
		String invalid = "{\"title\":\" \",\"startDate\":\"2026-05-10\",\"endDate\":\"2026-05-01\","
				+ "\"waypoints\":[" + KRAKOW + "],\"segments\":[{\"transportMode\":\"CAR\"}]}";

		mockMvc.perform(post(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.title").value("Podaj nazwę podróży."))
			.andExpect(jsonPath("$.fieldErrors.endDate")
				.value("Data zakończenia nie może być wcześniejsza niż data rozpoczęcia."))
			.andExpect(jsonPath("$.fieldErrors.waypoints")
				.value("Podróż musi mieć co najmniej 2 przystanki i maksymalnie 50."));

		assertThat(tripRepository.count()).isZero();
	}

	@Test
	void shouldRejectWrongNumberOfLegs() throws Exception {
		mockMvc.perform(post(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("Europa", "2026-05-01", List.of(KRAKOW, BERLIN, PARIS), List.of("CAR"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.segments")
				.value("Liczba odcinków musi być o 1 mniejsza niż liczba przystanków."));
	}

	@Test
	void shouldRejectWaypointOutOfRangeWithNestedFieldName() throws Exception {
		String badPlace = place("Nowhere", 91.0, 19.94, "PL", "Polska");

		mockMvc.perform(post(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("Europa", "2026-05-01", List.of(KRAKOW, badPlace), List.of("CAR"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors['waypoints[1].latitude']")
				.value("Szerokość geograficzna musi być od -90 do 90."));
	}

	@Test
	void shouldRequireToken() throws Exception {
		mockMvc.perform(get(TRIPS_URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(post(TRIPS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldDocumentEndpointsInSwagger() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/trips'].post").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips/{id}'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips/{id}'].put").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips/{id}'].delete").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/trips/{id}'].get.responses['404']").exists())
			.andExpect(jsonPath("$.components.schemas.SaveTripRequest.properties.waypoints").exists())
			.andExpect(jsonPath("$.components.schemas.TripResponse.properties.segments").exists())
			.andExpect(jsonPath("$.components.schemas.SegmentResponse.properties.geometry").exists())
			.andExpect(jsonPath("$.components.schemas.GeometryPointResponse.properties.latitude").exists())
			.andExpect(jsonPath("$.components.schemas.GeometryPointResponse.properties.longitude").exists());
	}

	private long createTrip(String authorization, String body) throws Exception {
		MvcResult result = mockMvc.perform(post(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andReturn();
		return idOf(result);
	}

	private static long idOf(MvcResult result) throws Exception {
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private static BigDecimal decimalAt(String json, String path) {
		Number value = JsonPath.read(json, path);
		return new BigDecimal(value.toString());
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

	private static String body(String title, String startDate, List<String> waypoints, List<String> modes) {
		String segments = modes.stream()
			.map(mode -> "{\"transportMode\":\"" + mode + "\"}")
			.collect(Collectors.joining(","));
		return "{\"title\":\"%s\",\"startDate\":\"%s\",\"waypoints\":[%s],\"segments\":[%s]}"
			.formatted(title, startDate, String.join(",", waypoints), segments);
	}
}
