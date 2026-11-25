package pl.travelapi.geo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.travelapi.geo.domain.GeocodingUnavailableException;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.shared.http.RequestSpacer;

class NominatimGeocodingProviderTest {

	private static final String BASE_URL = "https://nominatim.test";
	private static final String USER_AGENT = "TravelApi/test (+https://example.test)";

	private static final String KRAKOW_JSON = """
			[{"place_id":1,"lat":"50.0619474","lon":"19.9368564","name":"Kraków",
			  "display_name":"Kraków, województwo małopolskie, Polska",
			  "address":{"city":"Kraków","country":"Polska","country_code":"pl"}}]
			""";

	private MockRestServiceServer server;
	private NominatimGeocodingProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = providerWith(builder, new RequestSpacer(Duration.ZERO, Duration.ZERO, () -> 0L, nanos -> { }));
	}

	private static NominatimGeocodingProvider providerWith(RestClient.Builder builder, RequestSpacer spacer) {
		GeocodingProperties properties = new GeocodingProperties(BASE_URL, USER_AGENT, Duration.ZERO,
				Duration.ZERO, Duration.ofHours(1), 10, 5);
		return new NominatimGeocodingProvider(builder, properties, spacer);
	}

	@Test
	void shouldSendIdentifiedSearchRequestAndMapResult() {
		server.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("q", "Krakow"))
			.andExpect(queryParam("format", "jsonv2"))
			.andExpect(queryParam("addressdetails", "1"))
			.andExpect(queryParam("accept-language", "pl"))
			.andExpect(queryParam("limit", "5"))
			.andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
			.andRespond(withSuccess(KRAKOW_JSON, MediaType.APPLICATION_JSON));

		List<Place> places = provider.search("Krakow");

		assertThat(places).containsExactly(new Place("Kraków", 50.0619474, 19.9368564, "Polska", "PL"));
		server.verify();
	}

	@Test
	void shouldUseFirstPartOfDisplayNameWhenNameIsEmpty() {
		String json = """
				[{"lat":"39.6","lon":"2.9","name":"",
				  "display_name":"Mallorca, Illes Balears, Hiszpania",
				  "address":{"country":"Hiszpania","country_code":"es"}}]
				""";
		server.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

		assertThat(provider.search("Mallorca")).containsExactly(new Place("Mallorca", 39.6, 2.9, "Hiszpania", "ES"));
	}

	@Test
	void shouldSkipResultsWithoutCountry() {
		String json = """
				[{"lat":"0.0","lon":"0.0","name":"Ocean","display_name":"Ocean","address":{}},
				 {"lat":"0.0","lon":"0.0","name":"Brak adresu","display_name":"Brak adresu"}]
				""";
		server.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

		assertThat(provider.search("ocean")).isEmpty();
	}

	@Test
	void shouldReturnEmptyListWhenNothingFound() {
		server.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertThat(provider.search("xyzxyz")).isEmpty();
	}

	@Test
	void shouldThrowUnavailableWhenProviderReturnsServerError() {
		server.expect(requestTo(startsWith(BASE_URL + "/search"))).andRespond(withServerError());

		assertThatThrownBy(() -> provider.search("Kraków"))
			.isInstanceOfSatisfying(GeocodingUnavailableException.class,
					ex -> assertThat(ex.getMessage()).isEqualTo(GeocodingUnavailableException.UNAVAILABLE_MESSAGE));
	}

	@Test
	void shouldThrowUnavailableOnTimeout() {
		server.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andRespond(request -> {
				throw new SocketTimeoutException("Read timed out");
			});

		assertThatThrownBy(() -> provider.search("Kraków"))
			.isInstanceOf(GeocodingUnavailableException.class);
	}

	@Test
	void shouldThrowBusyWithoutCallingProviderWhenRateLimitRejects() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer strictServer = MockRestServiceServer.bindTo(builder).build();
		RequestSpacer spacer = new RequestSpacer(Duration.ofSeconds(1), Duration.ZERO, () -> 0L, nanos -> { });
		NominatimGeocodingProvider limited = providerWith(builder, spacer);
		strictServer.expect(requestTo(startsWith(BASE_URL + "/search")))
			.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		limited.search("pierwsze");

		assertThatThrownBy(() -> limited.search("drugie"))
			.isInstanceOfSatisfying(GeocodingUnavailableException.class,
					ex -> assertThat(ex.getMessage()).isEqualTo(GeocodingUnavailableException.BUSY_MESSAGE));
		strictServer.verify();
	}
}
