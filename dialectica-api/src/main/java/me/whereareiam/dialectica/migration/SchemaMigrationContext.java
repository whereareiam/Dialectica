package me.whereareiam.dialectica.migration;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * Context passed to schema migrations.
 */
public interface SchemaMigrationContext {
	/**
	 * Returns the owning Jdbi instance.
	 *
	 * @return Jdbi instance
	 */
	@NotNull Jdbi jdbi();

	/**
	 * Returns the active handle for the current migration execution.
	 *
	 * @return active handle
	 */
	@NotNull Handle handle();

	/**
	 * Returns the configured Dialectica database type.
	 *
	 * @return database type identifier
	 */
	@NotNull String databaseType();

	/**
	 * Returns the current migration scope.
	 *
	 * @return scope name
	 */
	@NotNull String scope();

	/**
	 * Checks whether a table exists.
	 *
	 * @param tableName table name
	 * @return {@code true} when the table exists
	 */
	boolean tableExists(@NotNull String tableName);

	/**
	 * Checks whether a column exists.
	 *
	 * @param tableName  table name
	 * @param columnName column name
	 * @return {@code true} when the column exists
	 */
	boolean columnExists(
			@NotNull String tableName,
			@NotNull String columnName
	);

	/**
	 * Executes a raw SQL statement.
	 *
	 * @param sql SQL statement
	 */
	void execute(@NotNull String sql);

	/**
	 * Renames a table using the current database dialect.
	 *
	 * @param sourceTable source table name
	 * @param targetTable target table name
	 */
	void renameTable(
			@NotNull String sourceTable,
			@NotNull String targetTable
	);
}
