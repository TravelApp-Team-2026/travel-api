package pl.travelapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class ApplicationContextIntegrationTest {

	@SuppressWarnings("resource")
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	private final DataSource dataSource;

	@Autowired
	ApplicationContextIntegrationTest(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Test
	void shouldStartContextAndConnectToDatabase() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(2)).isTrue();
		}
	}
}
