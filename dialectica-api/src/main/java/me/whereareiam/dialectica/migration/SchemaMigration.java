package me.whereareiam.dialectica.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single ordered schema migration within a named scope.
 */
public interface SchemaMigration {
	/**
	 * Returns the ordered version of this migration within its scope.
	 *
	 * @return migration version
	 */
	int version();

	/**
	 * Returns a stable human-readable migration name.
	 *
	 * @return migration name
	 */
	@NotNull String name();

	/**
	 * Applies the migration.
	 *
	 * @param context migration execution context
	 * @throws Exception when the migration fails
	 */
	void migrate(@NotNull SchemaMigrationContext context) throws Exception;
}
