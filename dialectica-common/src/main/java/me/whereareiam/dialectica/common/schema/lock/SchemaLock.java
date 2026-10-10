package me.whereareiam.dialectica.common.schema.lock;

import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Keeps other application instances from creating the same schema at the same time. Instances
 * that share one database start together in a proxy network, and neither PostgreSQL nor MySQL
 * makes {@code CREATE TABLE IF NOT EXISTS} safe against a concurrent creation of the same table.
 */
public interface SchemaLock extends AutoCloseable {
	/**
	 * Takes the lock the database offers and waits for another holder. Server databases shared by
	 * several instances are locked; embedded ones, which a single process owns, need none.
	 *
	 * @param handle       handle inside the transaction that creates the schema
	 * @param databaseType database type identifier, see {@link DatabaseType}
	 * @return held lock
	 * @throws IllegalStateException when the lock stays taken for the whole timeout
	 */
	static @NotNull SchemaLock acquire(@NotNull Handle handle, @NotNull String databaseType) {
		return switch (databaseType.toLowerCase(Locale.ROOT)) {
			case DatabaseType.POSTGRES -> new PostgresSchemaLock(handle);
			case DatabaseType.MYSQL, DatabaseType.MARIADB -> new MysqlSchemaLock(handle);
			default -> () -> {};
		};
	}

	/**
	 * Releases the lock unless the database releases it together with the transaction.
	 */
	@Override
	void close();
}
