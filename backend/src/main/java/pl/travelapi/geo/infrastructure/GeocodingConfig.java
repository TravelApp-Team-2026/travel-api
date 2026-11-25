package pl.travelapi.geo.infrastructure;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.travelapi.shared.http.RequestSpacer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GeocodingProperties.class)
class GeocodingConfig {

	@Bean
	RequestSpacer requestSpacer(GeocodingProperties properties) {
		return new RequestSpacer(properties.minRequestInterval(), properties.maxQueueWait(),
				System::nanoTime, TimeUnit.NANOSECONDS::sleep);
	}

	@Bean
	PlaceCache placeCache(GeocodingProperties properties, Clock clock) {
		return new PlaceCache(clock, properties.cacheTtl(), properties.cacheMaxEntries());
	}
}
