package me.whereareiam.dialectica;

import me.whereareiam.dialectica.common.DialectConfig;
import me.whereareiam.dialectica.common.DialectLocator;
import me.whereareiam.dialectica.common.handler.DialectHandlerFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.sqlobject.SqlObjects;

/**
 * Jdbi plugin that enables database-specific SQL resolution.
 * <p>
 * This plugin:
 * 1. Stores the database type identifier in Jdbi's configuration
 * 2. Configures a custom {@link DialectLocator} that resolves SQL from {@link StatementProvider}
 * 3. Registers handler factory that wires Dialectica handlers
 * <p>
 * Install this plugin when creating your Jdbi instance:
 * <pre>{@code
 * Jdbi jdbi = Jdbi.create(dataSource);
 * jdbi.installPlugin(new DialectPlugin(DatabaseType.POSTGRES));
 * }</pre>
 */
@SuppressWarnings("unused")
public final class DialectPlugin implements JdbiPlugin {
	private final String databaseType;

	/**
	 * Creates a new DialectPlugin with the given database type identifier.
	 *
	 * @param databaseType the database type identifier
	 */
	public DialectPlugin(String databaseType) {
		if (databaseType == null || databaseType.isBlank())
			throw new IllegalArgumentException("DatabaseType must not be null or blank");
		this.databaseType = databaseType;
	}

	@Override
	public void customizeJdbi(Jdbi jdbi) {
		// Store database type in Jdbi config
		DialectConfig config = jdbi.getConfig().get(DialectConfig.class);
		config.setDatabaseType(databaseType);

		// Configure custom SqlLocator that resolves from StatementProvider
		jdbi.getConfig(SqlObjects.class).setSqlLocator(new DialectLocator());

		// Register handler factory that provides real handler implementations
		jdbi.getConfig(Extensions.class).registerHandlerFactory(new DialectHandlerFactory());
	}
}

