package me.whereareiam.dialectica;

import org.jetbrains.annotations.NotNull;

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
	@NotNull SchemaManager scanPackages(@NotNull String... packageNames);

	/**
	 * Scans the specified package using a specific classloader.
	 * Useful in plugin environments where the default classloader detection might not work.
	 *
	 * @param classLoader  the classloader to use for scanning
	 * @param packageNames the package names to scan
	 * @return this SchemaManager instance for method chaining
	 */
	@NotNull SchemaManager scanPackages(@NotNull ClassLoader classLoader, @NotNull String... packageNames);

	/**
	 * Registers an entity class manually.
	 *
	 * @param entityClass the entity class to register
	 * @return this SchemaManager instance for method chaining
	 */
	@NotNull SchemaManager registerEntity(@NotNull Class<?> entityClass);


	/**
	 * Sets whether to use lazy initialization (create tables on first access).
	 *
	 * @param lazyInitialization true for lazy initialization, false for immediate
	 * @return this SchemaManager instance for method chaining
	 */
	@NotNull SchemaManager setLazyInitialization(boolean lazyInitialization);

	/**
	 * Sets whether to fail on error or continue with other entities.
	 *
	 * @param failOnError true to fail on first error, false to continue
	 * @return this SchemaManager instance for method chaining
	 */
	@NotNull SchemaManager setFailOnError(boolean failOnError);

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
	void ensureInitialized(@NotNull Class<?> entityClass);
}
