package me.whereareiam.dialectica.common.schema.lock;

import org.jdbi.v3.core.Handle;

import java.sql.SQLException;

/**
 * A named lock of MySQL and MariaDB, which commit on every DDL statement, so a transaction cannot
 * hold a lock for them. Such names are shared by the whole server, so the name carries the
 * database.
 */
final class MysqlSchemaLock implements SchemaLock {
	private static final String PREFIX = "dialectica:schema:";
	private static final int MAX_NAME_LENGTH = 64;
	private static final int TIMEOUT_SECONDS = 60;

	private final Handle handle;
	private final String name;

	MysqlSchemaLock(Handle handle) {
		this.handle = handle;
		this.name = name(database(handle));

		Integer obtained = handle.createQuery("SELECT GET_LOCK(:name, :timeout)")
				.bind("name", name)
				.bind("timeout", TIMEOUT_SECONDS)
				.mapTo(Integer.class)
				.one();
		if (obtained == null || obtained != 1)
			throw new IllegalStateException("Another instance is still initializing the schema of this database after "
					+ TIMEOUT_SECONDS + "s (lock " + name + ")");
	}

	@Override
	public void close() {
		handle.createQuery("SELECT RELEASE_LOCK(:name)")
				.bind("name", name)
				.mapTo(Integer.class)
				.list();
	}

	private static String database(Handle handle) {
		try {
			return String.valueOf(handle.getConnection().getCatalog());
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to read the database name for the schema lock", e);
		}
	}

	private static String name(String database) {
		String name = PREFIX + database;

		return name.length() <= MAX_NAME_LENGTH ? name : PREFIX + Integer.toHexString(database.hashCode());
	}
}
