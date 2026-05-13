package me.whereareiam.dialectica.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Builder for configuring a named schema migration scope.
 */
public interface MigrationScopeBuilder {
	/**
	 * Scans the specified packages for migration classes implementing {@link SchemaMigration}.
	 *
	 * @param packageNames package names to scan
	 * @return this builder for chaining
	 */
	MigrationScopeBuilder scanPackages(@NotNull String... packageNames);

	/**
	 * Scans the specified packages for migration classes using the given classloader.
	 *
	 * @param classLoader  classloader to use
	 * @param packageNames package names to scan
	 * @return this builder for chaining
	 */
	MigrationScopeBuilder scanPackages(
			@NotNull ClassLoader classLoader,
			@NotNull String... packageNames
	);

	/**
	 * Registers a migration class explicitly.
	 *
	 * @param migrationClass migration class to register
	 * @return this builder for chaining
	 */
	MigrationScopeBuilder registerMigration(@NotNull Class<? extends SchemaMigration> migrationClass);
}
