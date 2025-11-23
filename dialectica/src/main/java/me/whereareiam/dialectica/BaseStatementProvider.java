package me.whereareiam.dialectica;

import me.whereareiam.dialectica.type.DatabaseType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for {@link StatementProvider} implementations that eliminates
 * the need for repetitive switch statements.
 * <p>
 * Subclasses can provide SQL strings for each database type, or use the same
 * SQL for all databases by providing a single value.
 * <p>
 * Example usage:
 * <pre>{@code
 * public static class TruncateAll extends BaseStatementProvider {
 *     public TruncateAll() {
 *         super("TRUNCATE TABLE intercept_message_files RESTART IDENTITY CASCADE",
 *               "TRUNCATE TABLE intercept_message_files");
 *     }
 * }
 * }</pre>
 * <p>
 * Or for identical SQL across databases:
 * <pre>{@code
 * public static class FindById extends BaseStatementProvider {
 *     public FindById() {
 *         super("SELECT * FROM intercept_message_files WHERE id = :id");
 *     }
 * }
 * }</pre>
 * <p>
 * For multiple statements (e.g., DELETE followed by ALTER TABLE to reset AUTO_INCREMENT):
 * <pre>{@code
 * public static class TruncateAll extends BaseStatementProvider {
 *     public TruncateAll() {
 *         super("TRUNCATE TABLE intercept_message_files RESTART IDENTITY CASCADE",
 *               Arrays.asList("DELETE FROM intercept_message_files",
 *                            "ALTER TABLE intercept_message_files AUTO_INCREMENT = 1"));
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("unused")
public abstract class BaseStatementProvider implements StatementProvider {
	private final Map<DatabaseType, String> sqlMap;
	private final Map<DatabaseType, List<String>> multiStatementMap;

	/**
	 * Creates a provider with the same SQL for all database types.
	 *
	 * @param sql the SQL statement to use for all database types
	 */
	protected BaseStatementProvider(String sql) {
		this.sqlMap = new EnumMap<>(DatabaseType.class);
		this.multiStatementMap = new EnumMap<>(DatabaseType.class);
		for (DatabaseType type : DatabaseType.values()) {
			this.sqlMap.put(type, sql);
		}
	}

	/**
	 * Creates a provider with database-specific SQL.
	 * The order of arguments is: POSTGRES, MARIADB
	 *
	 * @param postgresSql the SQL statement for PostgreSQL
	 * @param mariaDbSql  the SQL statement for MariaDB
	 */
	protected BaseStatementProvider(String postgresSql, String mariaDbSql) {
		this.sqlMap = new EnumMap<>(DatabaseType.class);
		this.multiStatementMap = new EnumMap<>(DatabaseType.class);
		this.sqlMap.put(DatabaseType.POSTGRES, postgresSql);
		this.sqlMap.put(DatabaseType.MARIADB, mariaDbSql);
	}

	/**
	 * Creates a provider with database-specific SQL, where MariaDB uses multiple statements.
	 * The order of arguments is: POSTGRES (single statement), MARIADB (list of statements)
	 *
	 * @param postgresSql       the SQL statement for PostgreSQL
	 * @param mariaDbStatements the list of SQL statements for MariaDB (executed in order)
	 */
	protected BaseStatementProvider(String postgresSql, List<String> mariaDbStatements) {
		this.sqlMap = new EnumMap<>(DatabaseType.class);
		this.multiStatementMap = new EnumMap<>(DatabaseType.class);
		this.sqlMap.put(DatabaseType.POSTGRES, postgresSql);
		this.multiStatementMap.put(DatabaseType.MARIADB, mariaDbStatements);
		// For getStatemenet(), use the first statement
		if (!mariaDbStatements.isEmpty()) {
			this.sqlMap.put(DatabaseType.MARIADB, mariaDbStatements.get(0));
		}
	}

	@Override
	public final String getStatemenet(DatabaseType databaseType) {
		String sql = sqlMap.get(databaseType);
		if (sql == null) throw new IllegalStateException("No SQL defined for DatabaseType: " + databaseType);

		return sql;
	}

	@Override
	public List<String> getStatements(DatabaseType databaseType) {
		List<String> multiStatements = multiStatementMap.get(databaseType);
		if (multiStatements != null) {
			return multiStatements;
		}
		// Default: return single statement wrapped in a list
		return Collections.singletonList(getStatemenet(databaseType));
	}
}

