package pl.travelapi.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pl.travelapi.shared.security.JwtConfig;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;

@Configuration
@Import(JwtConfig.class)
public class SecurityConfig {

	/** BCrypt work factor (log2 of the number of rounds); raise it as hardware gets faster. */
	static final int BCRYPT_STRENGTH = 12;

	private static final RequestMatcher PUBLIC_PATHS = new OrRequestMatcher(
			PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
			PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
			PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
			PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**"));

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, RestSecurityExceptionHandler securityErrors)
			throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint(securityErrors)
				.accessDeniedHandler(securityErrors))
			.oauth2ResourceServer(oauth -> oauth
				.bearerTokenResolver(bearerTokenResolver())
				.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(securityErrors)
				.accessDeniedHandler(securityErrors))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.anyRequest().authenticated());
		return http.build();
	}

	/**
	 * Reads {@code Authorization: Bearer <token>}, but ignores it on public paths. Otherwise an expired token
	 * still stored in the app would make even login or registration fail with 401.
	 */
	private static BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
		return request -> PUBLIC_PATHS.matches(request) ? null : delegate.resolve(request);
	}
}
