package pl.travelapi.country.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.travelapi.country.application.VisitedCountryService;
import pl.travelapi.country.domain.VisitedCountry;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.exception.GlobalExceptionHandler;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;

@WebMvcTest(CountryController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, RestSecurityExceptionHandler.class })
class CountryControllerTest {

	private static final String VISITED_URL = "/api/v1/countries/visited";

	private final MockMvc mockMvc;

	@MockitoBean
	private VisitedCountryService visitedCountryService;

	@Autowired
	CountryControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	private static RequestPostProcessor loggedIn() {
		return jwt().jwt(token -> token.subject("1"));
	}

	@Test
	void shouldReturnVisitedCountriesOfLoggedInUser() throws Exception {
		when(visitedCountryService.findVisited(1L, null)).thenReturn(List.of(
				new VisitedCountry("PL", "Polska", 3),
				new VisitedCountry("CZ", "Czechy", 2)));

		mockMvc.perform(get(VISITED_URL).with(loggedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].code").value("PL"))
			.andExpect(jsonPath("$[0].name").value("Polska"))
			.andExpect(jsonPath("$[0].tripCount").value(3))
			.andExpect(jsonPath("$[1].code").value("CZ"));
	}

	@Test
	void shouldPassYearToService() throws Exception {
		when(visitedCountryService.findVisited(1L, Year.of(2026)))
			.thenReturn(List.of(new VisitedCountry("AT", "Austria", 1)));

		mockMvc.perform(get(VISITED_URL).param("year", "2026").with(loggedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Austria"));
	}

	@Test
	void shouldReturnEmptyList() throws Exception {
		when(visitedCountryService.findVisited(1L, null)).thenReturn(List.of());

		mockMvc.perform(get(VISITED_URL).with(loggedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldReturn400WithPolishMessageForInvalidYear() throws Exception {
		mockMvc.perform(get(VISITED_URL).param("year", "abc").with(loggedIn()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Nieprawidłowe żądanie"))
			.andExpect(jsonPath("$.fieldErrors.year").value("Rok musi być liczbą od 1900 do 2100."));

		verifyNoInteractions(visitedCountryService);
	}

	@Test
	void shouldReturn401WithoutToken() throws Exception {
		mockMvc.perform(get(VISITED_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"));

		verifyNoInteractions(visitedCountryService);
	}
}
