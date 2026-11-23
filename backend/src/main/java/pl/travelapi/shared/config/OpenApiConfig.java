package pl.travelapi.shared.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the JWT scheme in Swagger UI ("Authorize" button). Protected controllers refer to it with
 * {@code @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)}.
 */
@Configuration
@SecurityScheme(
		name = OpenApiConfig.BEARER_AUTH,
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "Token `accessToken` z odpowiedzi POST /api/v1/auth/login")
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";
}
