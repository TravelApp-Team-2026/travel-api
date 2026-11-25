package pl.travelapi.routing.infrastructure;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.travelapi.routing.domain.CarRouteProvider;
import pl.travelapi.shared.http.RequestSpacer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OsrmProperties.class)
class RoutingConfig {

	// The spacer is not a bean: each external service has its own limit, and geocoding already has one.
	@Bean
	CarRouteProvider carRouteProvider(RestClient.Builder builder, OsrmProperties properties) {
		RequestSpacer requestSpacer = new RequestSpacer(properties.minRequestInterval(), properties.maxQueueWait(),
				System::nanoTime, TimeUnit.NANOSECONDS::sleep);
		return new OsrmCarRouteProvider(builder, properties, requestSpacer);
	}
}
