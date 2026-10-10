package me.whereareiam.dialectica.common.schema.lock;

import org.jdbi.v3.core.Handle;

/**
 * A transaction-level advisory lock, which is scoped to the current database and which PostgreSQL
 * releases itself when the transaction commits or rolls back.
 */
final class PostgresSchemaLock implements SchemaLock {
	/** "DLSCHEMA" in ASCII; Strata's migration lock uses another key. */
	private static final long KEY = 0x444C_5343_4845_4D41L;

	PostgresSchemaLock(Handle handle) {
		handle.createQuery("SELECT pg_advisory_xact_lock(:key)")
				.bind("key", KEY)
				.mapTo(String.class)
				.list();
	}

	@Override
	public void close() {
		// The transaction owns the lock.
	}
}
