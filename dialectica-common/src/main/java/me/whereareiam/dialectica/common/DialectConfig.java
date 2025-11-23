package me.whereareiam.dialectica.common;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class to store DatabaseType in Jdbi's config registry.
 * This allows handlers to access the database type at runtime.
 */
@NoArgsConstructor
@AllArgsConstructor
public final class DialectConfig implements JdbiConfig<DialectConfig> {
	@Setter
	private DatabaseType databaseType;

	/**
	 * Gets the DatabaseType from the config registry.
	 *
	 * @param config the Jdbi config registry
	 * @return the DatabaseType, or null if not set
	 */
	public static DatabaseType getDatabaseType(ConfigRegistry config) {
		DialectConfig dbConfig = config.get(DialectConfig.class);

		return dbConfig != null ? dbConfig.databaseType : null;
	}

	@Override
	public DialectConfig createCopy() {
		return new DialectConfig(databaseType);
	}
}

