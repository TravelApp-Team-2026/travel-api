package pl.travelapi.geo.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.travelapi.geo.application.PlaceSearchService;
import pl.travelapi.geo.domain.GeocodingUnavailableException;
import pl.travelapi.geo.domain.Place;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.exception.GlobalExceptionHandler;
import pl.travelapi.shared.exception.InvalidRequestException;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;

@WebMvcTest(GeoController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, RestSecurityExceptionHandler.class })
class GeoControllerTest {

	private static final String SEARCH_URL = "/api/v1/geo/search";

	private final MockMvc mockMvc;

	@MockitoBean
	private PlaceSearchService placeSearchService;

	@Autowired
	GeoControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	private static RequestPostProcessor loggedIn() {
		return jwt().jwt(token -> token.subject("1"));
	}

	@Test
	void shouldReturnFoundPlaces() throws Exception {
		when(placeSearchService.search("Kraków"))
			.thenReturn(List.of(new Place("Kraków", 50.0619474, 19.9368564, "Polska", "PL")));

		mockMvc.perform(get(SEARCH_URL).param("q", "Kraków").with(loggedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].placeName").value("Kraków"))
			.andExpect(jsonPath("$[0].latitude").value(50.0619474))
			.andExpect(jsonPath("$[0].longitude").value(19.9368564))
			.andExpect(jsonPath("$[0].countryName").value("Polska"))
			.andExpect(jsonPath("$[0].countryCode").value("PL"));
	}

	@Test
	void shouldReturnEmptyListWhenNothingFound() throws Exception {
		when(placeSearchService.search("xyzxyz")).thenReturn(List.of());

		mockMvc.perform(get(SEARCH_URL).param("q", "xyzxyz").with(loggedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldReturn401WithoutToken() throws Exception {
		mockMvc.perform(get(SEARCH_URL).param("q", "Kraków"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"));

		verifyNoInteractions(placeSearchService);
	}

	@Test
	void shouldReturn400WithFieldErrorWhenQueryIsMissing() throws Exception {
		when(placeSearchService.search(""))
			.thenThrow(new InvalidRequestException("q", "Wpisz nazwę miejsca."));

		mockMvc.perform(get(SEARCH_URL).with(loggedIn()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.q").value("Wpisz nazwę miejsca."));
	}

	@Test
	void shouldReturn503WhenGeocodingIsUnavailable() throws Exception {
		when(placeSearchService.search("Kraków"))
			.thenThrow(new GeocodingUnavailableException(GeocodingUnavailableException.UNAVAILABLE_MESSAGE));

		mockMvc.perform(get(SEARCH_URL).param("q", "Kraków").with(loggedIn()))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.title").value("Usługa niedostępna"))
			.andExpect(jsonPath("$.detail").value(GeocodingUnavailableException.UNAVAILABLE_MESSAGE));
	}
}
