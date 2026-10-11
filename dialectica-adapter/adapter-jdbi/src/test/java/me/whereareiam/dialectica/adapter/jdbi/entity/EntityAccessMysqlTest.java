package me.whereareiam.dialectica.adapter.jdbi.entity;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the {@link EntityAccessContract} on MySQL 8 in a container.
 */
@Tag("database-container")
@Testcontainers
class EntityAccessMysqlTest extends EntityAccessContract {
	@Container
	private static final MySQLContainer<?> DATABASE = new MySQLContainer<>("mysql:8.4");

	@Override
	protected Jdbi createJdbi() {
		return Jdbi.create(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
	}
}
