package me.whereareiam.dialectica.common;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class to store the database type in Jdbi's config registry.
 * This allows handlers to access the database type identifier at runtime.
 */
@NoArgsConstructor
@AllArgsConstructor
public final class DialectConfig implements JdbiConfig<DialectConfig> {
	@Setter
	private String databaseType;

	/**
	 * Gets the database type from the config registry.
	 *
	 * @param config the Jdbi config registry
	 * @return the database type identifier, or null if not set
	 */
	public static String getDatabaseType(ConfigRegistry config) {
		DialectConfig dbConfig = config.get(DialectConfig.class);

		return dbConfig != null ? dbConfig.databaseType : null;
	}

	@Override
	public DialectConfig createCopy() {
		return new DialectConfig(databaseType);
	}
}

