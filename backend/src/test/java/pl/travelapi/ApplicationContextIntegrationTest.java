package pl.travelapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.travelapi.support.IntegrationTest;

@IntegrationTest
class ApplicationContextIntegrationTest {

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
