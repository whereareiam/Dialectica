package me.whereareiam.dialectica.adapter.jdbi.entity;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the {@link EntityAccessContract} on PostgreSQL in a container.
 */
@Tag("database-container")
@Testcontainers
class EntityAccessPostgresqlTest extends EntityAccessContract {
	@Container
	private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:18.1");

	@Override
	protected Jdbi createJdbi() {
		return Jdbi.create(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
	}
}
