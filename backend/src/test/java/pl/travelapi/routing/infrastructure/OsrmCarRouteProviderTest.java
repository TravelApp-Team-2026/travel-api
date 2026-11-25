package pl.travelapi.routing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import pl.travelapi.routing.domain.GeoPoint;
import pl.travelapi.routing.domain.Route;
import pl.travelapi.shared.http.RequestSpacer;

class OsrmCarRouteProviderTest {

	private static final String BASE_URL = "https://osrm.test";
	private static final String USER_AGENT = "TravelApi/test (+https://example.test)";
	private static final double MAX_SNAP_METERS = 10_000;

	private static final GeoPoint WROCLAW = new GeoPoint(51.1079, 17.0385);
	private static final GeoPoint BERLIN = new GeoPoint(52.52, 13.405);

	// Longitude comes first. The unknown fields check that the rest of a real response is ignored.
	private static final String ROUTE_JSON = """
			{"code":"Ok",
			 "routes":[{"distance":345678.9,"duration":12345.6,"weight_name":"routability","legs":[],
			   "geometry":{"type":"LineString",
			     "coordinates":[[17.0385,51.1079],[15.2,51.9],[13.405,52.52]]}}],
			 "waypoints":[{"distance":%s,"name":"A","location":[17.0385,51.1079]},
			   {"distance":%s,"name":"B","location":[13.405,52.52]}]}
			""";

	private MockRestServiceServer server;
	private OsrmCarRouteProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = providerWith(builder, new RequestSpacer(Duration.ZERO, Duration.ZERO, () -> 0L, nanos -> { }));
	}

	private static OsrmCarRouteProvider providerWith(RestClient.Builder builder, RequestSpacer spacer) {
		OsrmProperties properties = new OsrmProperties(BASE_URL, USER_AGENT, Duration.ZERO, Duration.ZERO,
				MAX_SNAP_METERS);
		return new OsrmCarRouteProvider(builder, properties, spacer);
	}

	private static String routeJson(double startSnapMeters, double endSnapMeters) {
		return ROUTE_JSON.formatted(startSnapMeters, endSnapMeters);
	}

	private void respondWith(ResponseCreator response) {
		server.expect(requestTo(startsWith(BASE_URL + "/route/v1/driving/"))).andRespond(response);
	}

	@Test
	void shouldSendIdentifiedRequestWithLongitudeFirst() {
		server.expect(requestTo(BASE_URL
				+ "/route/v1/driving/17.0385,51.1079;13.405,52.52?overview=simplified&geometries=geojson"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
			.andRespond(withSuccess(routeJson(12.3, 4.5), MediaType.APPLICATION_JSON));

		provider.findRoute(WROCLAW, BERLIN);

		server.verify();
	}

	@Test
	void shouldSendVerySmallCoordinatesWithoutExponent() {
		server.expect(requestTo(startsWith(BASE_URL + "/route/v1/driving/0.000010,-0.000020;13.405,52.52?")))
			.andRespond(withSuccess(routeJson(0, 0), MediaType.APPLICATION_JSON));

		provider.findRoute(new GeoPoint(-0.00002, 0.00001), BERLIN);

		server.verify();
	}

	@Test
	void shouldMapGeometryAndDistanceInKilometres() {
		respondWith(withSuccess(routeJson(12.3, 4.5), MediaType.APPLICATION_JSON));

		Optional<Route> route = provider.findRoute(WROCLAW, BERLIN);

		assertThat(route).hasValueSatisfying(found -> {
			assertThat(found.distanceKm()).isEqualTo(new BigDecimal("345.68"));
			assertThat(found.geometry()).containsExactly(WROCLAW, new GeoPoint(51.9, 15.2), BERLIN);
		});
	}

	@Test
	void shouldAcceptStopExactlyAtSnapLimit() {
		respondWith(withSuccess(routeJson(MAX_SNAP_METERS, 0), MediaType.APPLICATION_JSON));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isPresent();
	}

	@Test
	void shouldReturnEmptyWhenStartIsTooFarFromAnyRoad() {
		respondWith(withSuccess(routeJson(MAX_SNAP_METERS + 1, 0), MediaType.APPLICATION_JSON));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldReturnEmptyWhenEndIsTooFarFromAnyRoad() {
		respondWith(withSuccess(routeJson(0, 250_000), MediaType.APPLICATION_JSON));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "NoRoute", "NoSegment" })
	void shouldReturnEmptyWhenOsrmFindsNoRoute(String code) {
		respondWith(withBadRequest()
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"code\":\"%s\",\"message\":\"Impossible route\"}".formatted(code)));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldReturnEmptyWhenServerFails() {
		respondWith(withServerError());

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldReturnEmptyWhenRateLimitedByServer() {
		respondWith(withTooManyRequests());

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldReturnEmptyOnTimeout() {
		respondWith(request -> {
			throw new SocketTimeoutException("Read timed out");
		});

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"{broken",
			"{\"code\":\"NoRoute\"}",
			"{\"code\":\"Ok\"}",
			"{\"code\":\"Ok\",\"routes\":[]}",
			"{\"code\":\"Ok\",\"routes\":[{\"distance\":1.0}]}",
			"{\"code\":\"Ok\",\"routes\":[{\"distance\":1.0,\"geometry\":{\"coordinates\":[[17.0,51.0]]}}]}",
			"{\"code\":\"Ok\",\"routes\":[{\"distance\":1.0,\"geometry\":{\"coordinates\":[[17.0],[13.0]]}}]}",
			"{\"code\":\"Ok\",\"routes\":[{\"distance\":1.0,"
					+ "\"geometry\":{\"coordinates\":[[17.0,51.0],[13.0,52.0]]}}]}"
	})
	void shouldReturnEmptyForUnusableResponse(String body) {
		respondWith(withSuccess(body, MediaType.APPLICATION_JSON));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldReturnEmptyWhenResponseIsNotJson() {
		respondWith(withSuccess("<html>busy</html>", MediaType.TEXT_HTML));

		assertThat(provider.findRoute(WROCLAW, BERLIN)).isEmpty();
	}

	@Test
	void shouldNotCallServerWhenTooManyRequestsAreWaiting() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer strictServer = MockRestServiceServer.bindTo(builder).build();
		RequestSpacer spacer = new RequestSpacer(Duration.ofSeconds(1), Duration.ZERO, () -> 0L, nanos -> { });
		OsrmCarRouteProvider limited = providerWith(builder, spacer);
		strictServer.expect(requestTo(startsWith(BASE_URL)))
			.andRespond(withSuccess(routeJson(0, 0), MediaType.APPLICATION_JSON));

		assertThat(limited.findRoute(WROCLAW, BERLIN)).isPresent();
		assertThat(limited.findRoute(WROCLAW, BERLIN)).isEmpty();

		strictServer.verify();
	}

	@Test
	void shouldKeepInterruptFlagWhenInterruptedWhileWaiting() throws InterruptedException {
		RequestSpacer spacer = new RequestSpacer(Duration.ofSeconds(1), Duration.ofSeconds(5), () -> 0L,
				nanos -> {
					throw new InterruptedException();
				});
		OsrmCarRouteProvider interrupted = providerWith(RestClient.builder(), spacer);
		// Takes the free slot, so the provider has to wait for the next one.
		spacer.acquire();

		Optional<Route> route = interrupted.findRoute(WROCLAW, BERLIN);

		// Thread.interrupted() also clears the flag, so it does not leak into other tests.
		assertThat(Thread.interrupted()).isTrue();
		assertThat(route).isEmpty();
	}
}
