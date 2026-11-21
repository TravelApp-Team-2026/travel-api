package pl.travelapi.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pl.travelapi.shared.security.RestSecurityExceptionHandler;

@Configuration
public class SecurityConfig {

	/** BCrypt work factor (log2 of the number of rounds); raise it as hardware gets faster. */
	static final int BCRYPT_STRENGTH = 12;

	private static final String[] PUBLIC_PATHS = {
		"/v3/api-docs/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/api/v1/auth/**"
	};

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
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.anyRequest().authenticated());
		return http.build();
	}
}

