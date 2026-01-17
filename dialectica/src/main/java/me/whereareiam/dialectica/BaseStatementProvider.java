package me.whereareiam.dialectica;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private final Map<String, String> sqlMap;
	private final Map<String, List<String>> multiStatementMap;
	private String defaultSql;

	protected BaseStatementProvider() {
		this.sqlMap = new ConcurrentHashMap<>();
		this.multiStatementMap = new ConcurrentHashMap<>();
	}

	/**
	 * Creates a provider with the same SQL for all database types.
	 *
	 * @param sql the SQL statement to use for all database types
	 */
	protected BaseStatementProvider(String sql) {
		this();
		defaultSql(sql);
	}

	/**
	 * Creates a provider with database-specific SQL for one or more types.
	 *
	 * @param statement the SQL statement to register
	 * @param types     database type identifiers that should use this statement
	 */
	protected BaseStatementProvider(String statement, String... types) {
		this();
		register(statement, types);
	}

	/**
	 * Creates a provider with multiple statements for one or more types.
	 *
	 * @param statements the list of SQL statements (executed in order)
	 * @param types      database type identifiers that should use these statements
	 */
	protected BaseStatementProvider(List<String> statements, String... types) {
		this();
		register(statements, types);
	}

	protected final void defaultSql(String statement) {
		this.defaultSql = statement;
	}

	protected final void register(String statement, String... types) {
		if (types == null || types.length == 0)
			throw new IllegalArgumentException("At least one database type must be provided");
		for (String type : types) {
			if (type == null || type.isBlank()) continue;
			sqlMap.put(type, statement);
		}
	}

	protected final void register(List<String> statements, String... types) {
		if (types == null || types.length == 0)
			throw new IllegalArgumentException("At least one database type must be provided");
		for (String type : types) {
			if (type == null || type.isBlank()) continue;
			multiStatementMap.put(type, statements);
			if (statements != null && !statements.isEmpty()) {
				sqlMap.put(type, statements.get(0));
			}
		}
	}

	@Override
	public final String getStatemenet(String databaseType) {
		String sql = sqlMap.get(databaseType);
		if (sql != null) return sql;
		if (defaultSql != null) return defaultSql;

		throw new IllegalStateException("No SQL defined for database type: " + databaseType);
	}

	@Override
	public List<String> getStatements(String databaseType) {
		List<String> multiStatements = multiStatementMap.get(databaseType);
		if (multiStatements != null) return multiStatements;

		return Collections.singletonList(getStatemenet(databaseType));
	}
}

