package me.whereareiam.dialectica;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Dialectica integration tests using Testcontainers.
 * Provides PostgreSQL and MariaDB containers and Jdbi instances.
 */
@Testcontainers
public abstract class BaseTest {
	@Container
	protected static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:18.1");

	@Container
	protected static final MariaDBContainer<?> mariaDbContainer = new MariaDBContainer<>("mariadb:12");

	protected static Jdbi postgresJdbi;
	protected static Jdbi mariaDbJdbi;
	private static final List<HikariDataSource> dataSources = new ArrayList<>();

	@BeforeAll
	static void setUpContainers() {
		postgresJdbi = createJdbi(
				postgresContainer.getJdbcUrl(),
				postgresContainer.getUsername(),
				postgresContainer.getPassword(),
				DatabaseType.POSTGRES
		);
		mariaDbJdbi = createJdbi(
				mariaDbContainer.getJdbcUrl(),
				mariaDbContainer.getUsername(),
				mariaDbContainer.getPassword(),
				DatabaseType.MARIADB
		);
	}

	@AfterAll
	static void tearDownContainers() {
		for (HikariDataSource dataSource : dataSources) {
			if (dataSource != null && !dataSource.isClosed()) {
				dataSource.close();
			}
		}
		dataSources.clear();
	}

	private static Jdbi createJdbi(String jdbcUrl, String username, String password, DatabaseType databaseType) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setMaximumPoolSize(5);
		config.setMinimumIdle(1);

		HikariDataSource dataSource = new HikariDataSource(config);
		dataSources.add(dataSource);
		Jdbi jdbi = Jdbi.create(dataSource);
		jdbi.installPlugin(new SqlObjectPlugin());
		jdbi.installPlugin(new DialectPlugin(databaseType));

		return jdbi;
	}

	protected Jdbi getJdbi(DatabaseType type) {
		return switch (type) {
			case POSTGRES -> postgresJdbi;
			case MARIADB -> mariaDbJdbi;
		};
	}
}

