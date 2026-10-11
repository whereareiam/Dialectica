package me.whereareiam.dialectica.adapter.jdbi.entity;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the {@link EntityAccessContract} on MariaDB in a container.
 */
@Tag("database-container")
@Testcontainers
class EntityAccessMariadbTest extends EntityAccessContract {
	@Container
	private static final MariaDBContainer<?> DATABASE = new MariaDBContainer<>("mariadb:12");

	@Override
	protected Jdbi createJdbi() {
		return Jdbi.create(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
	}
}
