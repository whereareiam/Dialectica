package me.whereareiam.dialectica;

import me.whereareiam.dialectica.migration.MigrationScopeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Interface for managing database schema initialization.
 * <p>
 * This interface provides methods for registering entities and initializing database schemas.
 * Implementations are provided by the Dialectica library.
 */
@SuppressWarnings("unused")
public interface SchemaManager {
	/**
	 * Scans the specified package for classes annotated with {@link me.whereareiam.dialectica.annotation.Entity}.
	 * Uses multiple classloader strategies to find classes in plugin environments.
	 *
	 * @param packageNames the package names to scan (e.g., "me.whereareiam.intercept.entity")
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager scanPackages(String... packageNames);

	/**
	 * Scans the specified package using a specific classloader.
	 * Useful in plugin environments where the default classloader detection might not work.
	 *
	 * @param classLoader  the classloader to use for scanning
	 * @param packageNames the package names to scan
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager scanPackages(ClassLoader classLoader, String... packageNames);

	/**
	 * Registers an entity class manually.
	 *
	 * @param entityClass the entity class to register
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager registerEntity(Class<?> entityClass);

	/**
	 * Registers a named schema migration scope.
	 * Migrations are ordered and applied independently within each scope.
	 *
	 * @param scope   logical migration scope name
	 * @param builder scope configuration callback
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager registerMigrationScope(
			@NotNull String scope,
			@NotNull Consumer<MigrationScopeBuilder> builder
	);

	/**
	 * Overrides the default migration history table name.
	 *
	 * @param tableName migration history table name
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager setMigrationTable(@NotNull String tableName);

	/**
	 * Sets whether to use lazy initialization (create tables on first access).
	 *
	 * @param lazyInitialization true for lazy initialization, false for immediate
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager setLazyInitialization(boolean lazyInitialization);

	/**
	 * Sets whether to fail on error or continue with other entities.
	 *
	 * @param failOnError true to fail on first error, false to continue
	 * @return this SchemaManager instance for method chaining
	 */
	SchemaManager setFailOnError(boolean failOnError);

	/**
	 * Initializes all registered entities by creating their tables.
	 * Tables are created in dependency order.
	 *
	 * @throws IllegalStateException if Jdbi or database type is not configured
	 */
	void initialize();

	/**
	 * Ensures a specific entity's table is initialized (for lazy initialization).
	 *
	 * @param entityClass the entity class to ensure is initialized
	 */
	void ensureInitialized(Class<?> entityClass);
}
