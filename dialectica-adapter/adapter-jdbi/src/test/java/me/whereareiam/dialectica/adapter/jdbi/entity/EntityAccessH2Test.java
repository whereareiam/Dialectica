package me.whereareiam.dialectica.adapter.jdbi.entity;

import org.jdbi.v3.core.Jdbi;

import java.util.UUID;

/**
 * Runs the {@link EntityAccessContract} on an in-memory H2 database.
 */
class EntityAccessH2Test extends EntityAccessContract {
	@Override
	protected Jdbi createJdbi() {
		return Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
	}
}
