package me.whereareiam.dialectica;

import me.whereareiam.dialectica.common.schema.DefaultSchemaManager;
import org.jdbi.v3.core.Jdbi;

/**
 * Main entry point for Dialectica database schema management.
 * <p>
 * This class provides a convenient API that wires the public API to the implementation.
 * Users should use this class as the primary interface for Dialectica functionality.
 * <p>
 * Example usage:
 * <pre>{@code
 * Jdbi jdbi = Jdbi.create(dataSource);
 * jdbi.installPlugin(new DialectPlugin(DatabaseType.POSTGRES));
 *
 * Dialectica.schema(jdbi)
 *     .scanPackages("me.whereareiam.intercept.entity")
 *     .initialize();
 * }</pre>
 */
public final class Dialectica {
	/**
	 * Creates a new SchemaManager for the given Jdbi instance.
	 * This is the recommended way to initialize database schemas.
	 *
	 * @param jdbi the Jdbi instance to use for schema operations
	 * @return a new SchemaManager instance
	 */
	public static SchemaManager schema(Jdbi jdbi) {
		return new DefaultSchemaManager(jdbi);
	}
}
