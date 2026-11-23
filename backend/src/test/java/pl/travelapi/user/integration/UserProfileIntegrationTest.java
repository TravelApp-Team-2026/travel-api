package pl.travelapi.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.travelapi.user.domain.User;
import pl.travelapi.user.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileIntegrationTest {

	private static final String PROFILE_URL = "/api/v1/users/me";

	@SuppressWarnings("resource")
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	private final MockMvc mockMvc;
	private final UserRepository userRepository;

	private String bearer;

	@Autowired
	UserProfileIntegrationTest(MockMvc mockMvc, UserRepository userRepository) {
		this.mockMvc = mockMvc;
		this.userRepository = userRepository;
	}

	@BeforeEach
	void registerAndLogIn() throws Exception {
		bearer = "Bearer " + registerAndLogIn("anna@example.com", "Anna");
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
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

	@Test
	void shouldReturnProfileOfNewUser() throws Exception {
		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("anna@example.com"))
			.andExpect(jsonPath("$.displayName").value("Anna"))
			.andExpect(jsonPath("$.bio").value(nullValue()))
			.andExpect(jsonPath("$.isPublic").value(false));
	}

	@Test
	void shouldSaveChangesAndReturnThemOnNextRead() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"  Anna Nowak \",\"bio\":\"Podróżuję pociągiem.\",\"isPublic\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Anna Nowak"));

		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Anna Nowak"))
			.andExpect(jsonPath("$.bio").value("Podróżuję pociągiem."))
			.andExpect(jsonPath("$.isPublic").value(true));

		User saved = userRepository.findByEmail("anna@example.com").orElseThrow();
		assertThat(saved.getDisplayName()).isEqualTo("Anna Nowak");
		assertThat(saved.isPublicProfile()).isTrue();
	}

	@Test
	void shouldChangeOnlySentFieldsAndClearBioWithEmptyText() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bio\":\"Lubię góry.\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"isPublic\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Anna"))
			.andExpect(jsonPath("$.bio").value("Lubię góry."))
			.andExpect(jsonPath("$.isPublic").value(true));

		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bio\":\"\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bio").value(nullValue()));
		assertThat(userRepository.findByEmail("anna@example.com").orElseThrow().getBio()).isNull();
	}

	@Test
	void shouldIgnoreEmailInPatch() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"other@example.com\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("anna@example.com"));
	}

	@Test
	void shouldRejectInvalidDataAndKeepProfile() throws Exception {
		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\" \",\"isPublic\":true}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.displayName").value("Nazwa wyświetlana nie może być pusta."));

		User saved = userRepository.findByEmail("anna@example.com").orElseThrow();
		assertThat(saved.getDisplayName()).isEqualTo("Anna");
		assertThat(saved.isPublicProfile()).isFalse();
	}

	@Test
	void shouldChangeOnlyOwnProfile() throws Exception {
		String otherBearer = "Bearer " + registerAndLogIn("ola@example.com", "Ola");

		mockMvc.perform(patch(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, otherBearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Ola K.\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ola@example.com"));

		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(jsonPath("$.displayName").value("Anna"));
	}

	@Test
	void shouldReturn404WhenAccountWasDeletedButTokenIsStillValid() throws Exception {
		userRepository.deleteAll();

		mockMvc.perform(get(PROFILE_URL).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Nie znaleziono użytkownika."));
	}

	@Test
	void shouldRequireToken() throws Exception {
		mockMvc.perform(get(PROFILE_URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(patch(PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldDocumentEndpointsInSwagger() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].patch").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['400']").exists())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.schemas.ProfileResponse.properties.isPublic").exists())
			.andExpect(jsonPath("$.components.schemas.UpdateProfileRequest.properties.isPublic").exists());
	}
}
