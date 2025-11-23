package me.whereareiam.dialectica;

import me.whereareiam.dialectica.type.DatabaseType;

import java.util.Collections;
import java.util.List;

/**
 * Interface for providing database-specific SQL statements.
 * <p>
 * Implementations should provide SQL strings that are specific to each database type.
 * For statements that are identical across databases, simply return the same SQL for all types.
 * <p>
 * To provide multiple statements (e.g., DELETE followed by ALTER TABLE to reset AUTO_INCREMENT),
 * override {@link #getStatements(DatabaseType)} instead of {@link #getStatemenet(DatabaseType)}.
 */
public interface StatementProvider {
	/**
	 * Gets the SQL statement string for a specific database type.
	 *
	 * @param databaseType the database type (POSTGRES or MARIADB)
	 * @return the SQL statement string appropriate for the given database type
	 */
	String getStatemenet(DatabaseType databaseType);

	/**
	 * Gets multiple SQL statements for a specific database type.
	 * <p>
	 * By default, this returns a list containing the single statement from {@link #getStatemenet(DatabaseType)}.
	 * Override this method to provide multiple statements that should be executed in sequence.
	 *
	 * @param databaseType the database type (POSTGRES or MARIADB)
	 * @return a list of SQL statements to execute in order
	 */
	default List<String> getStatements(DatabaseType databaseType) {
		return Collections.singletonList(getStatemenet(databaseType));
	}
}