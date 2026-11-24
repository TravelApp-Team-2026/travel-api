package pl.travelapi.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Full application with MockMvc and a real PostgreSQL (Testcontainers).
 * <p>
 * Every integration test uses exactly this annotation, so Spring builds the context once and all test
 * classes share it, together with one database container. Do not add other context settings
 * ({@code @MockitoBean}, {@code @TestPropertySource}, extra {@code @Import}) to a single test class:
 * that creates a second context and a second container. Tests share the database, so each class
 * removes its own data in {@code @AfterEach}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
public @interface IntegrationTest {
}
