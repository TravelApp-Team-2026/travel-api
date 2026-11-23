package pl.travelapi.user.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.travelapi.shared.config.SecurityConfig;
import pl.travelapi.shared.exception.GlobalExceptionHandler;
import pl.travelapi.shared.exception.ResourceNotFoundException;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;
import pl.travelapi.user.application.UserProfileService;
import pl.travelapi.user.domain.User;

@WebMvcTest(UserController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, RestSecurityExceptionHandler.class })
class UserControllerTest {

	private static final String PROFILE_URL = "/api/v1/users/me";

	private final MockMvc mockMvc;

	@MockitoBean
	private UserProfileService userProfileService;

	@Autowired
	UserControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	/** Authenticated as the user with id 1. */
	private static RequestPostProcessor user1() {
		return jwt().jwt(token -> token.subject("1"));
	}

	private static User user(String bio, boolean isPublic) {
		User user = User.create("anna@example.com", "$2a$10$hash", "Anna", Instant.parse("2026-01-01T10:00:00Z"));
		ReflectionTestUtils.setField(user, "id", 1L);
		user.updateProfile("Anna", bio, isPublic);
		return user;
	}

	@Test
	void shouldReturnProfileOfLoggedInUser() throws Exception {
		when(userProfileService.getProfile(1L)).thenReturn(user("Lubię góry.", true));

		mockMvc.perform(get(PROFILE_URL).with(user1()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.email").value("anna@example.com"))
			.andExpect(jsonPath("$.displayName").value("Anna"))
			.andExpect(jsonPath("$.bio").value("Lubię góry."))
			.andExpect(jsonPath("$.isPublic").value(true))
			.andExpect(jsonPath("$.public").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void shouldReturnNullBioAndPrivateProfileByDefault() throws Exception {
		when(userProfileService.getProfile(1L)).thenReturn(user(null, false));

		mockMvc.perform(get(PROFILE_URL).with(user1()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bio").value(nullValue()))
			.andExpect(jsonPath("$.isPublic").value(false));
	}

	@Test
	void shouldReturn401WithoutToken() throws Exception {
		mockMvc.perform(get(PROFILE_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Brak autoryzacji"));
		mockMvc.perform(patch(PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());

		verifyNoInteractions(userProfileService);
	}

	@Test
	void shouldReturn404WhenUserNoLongerExists() throws Exception {
		when(userProfileService.getProfile(anyLong()))
			.thenThrow(new ResourceNotFoundException("Nie znaleziono użytkownika."));

		mockMvc.perform(get(PROFILE_URL).with(user1()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Nie znaleziono użytkownika."));
	}

	@Test
	void shouldPassChangesToServiceAndReturnUpdatedProfile() throws Exception {
		when(userProfileService.updateProfile(1L, "Anna", "Lubię góry.", true)).thenReturn(user("Lubię góry.", true));

		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Anna\",\"bio\":\"Lubię góry.\",\"isPublic\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bio").value("Lubię góry."))
			.andExpect(jsonPath("$.isPublic").value(true));

		verify(userProfileService).updateProfile(1L, "Anna", "Lubię góry.", true);
	}

	@Test
	void shouldPassOmittedFieldsAsNull() throws Exception {
		when(userProfileService.updateProfile(1L, null, null, false)).thenReturn(user(null, false));

		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"isPublic\":false}"))
			.andExpect(status().isOk());

		verify(userProfileService).updateProfile(1L, null, null, false);
	}

	@Test
	void shouldReturn400WithPolishMessagesForInvalidData() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"   \",\"bio\":\"%s\"}".formatted("b".repeat(301))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.displayName").value("Nazwa wyświetlana nie może być pusta."))
			.andExpect(jsonPath("$.fieldErrors.bio").value("Opis „O mnie” może mieć maksymalnie 300 znaków."));

		verifyNoInteractions(userProfileService);
	}

	@Test
	void shouldRejectEmptyAndTooLongDisplayName() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.displayName").value("Nazwa wyświetlana nie może być pusta."));
		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"%s\"}".formatted("n".repeat(51))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.displayName")
				.value("Nazwa wyświetlana może mieć maksymalnie 50 znaków."));

		verifyNoInteractions(userProfileService);
	}

	@Test
	void shouldAcceptBioOfExactly300CharactersAndEmptyBio() throws Exception {
		when(userProfileService.updateProfile(anyLong(), any(), any(), any()))
			.thenReturn(user(null, false));

		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bio\":\"%s\"}".formatted("b".repeat(300))))
			.andExpect(status().isOk());
		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bio\":\"\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void shouldReturn400ForInvalidIsPublicType() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).with(user1())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"isPublic\":\"maybe\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Nie można odczytać przesłanych danych. Sprawdź format żądania."));

		verifyNoInteractions(userProfileService);
	}
}
