package me.whereareiam.dialectica;

import java.util.Collections;
import java.util.List;

/**
 * Interface for providing database-specific SQL statements.
 * <p>
 * Implementations should provide SQL strings that are specific to each database type.
 * For statements that are identical across databases, simply return the same SQL for all types.
 * <p>
 * To provide multiple statements (e.g., DELETE followed by ALTER TABLE to reset AUTO_INCREMENT),
 * override {@link #getStatements(String)} instead of {@link #getStatemenet(String)}.
 */
public interface StatementProvider {
	/**
	 * Gets the SQL statement string for a specific database type.
	 *
	 * @param databaseType the database type identifier
	 * @return the SQL statement string appropriate for the given database type
	 */
	String getStatemenet(String databaseType);

	/**
	 * Gets multiple SQL statements for a specific database type.
	 * <p>
	 * By default, this returns a list containing the single statement from {@link #getStatemenet(String)}.
	 * Override this method to provide multiple statements that should be executed in sequence.
	 *
	 * @param databaseType the database type identifier
	 * @return a list of SQL statements to execute in order
	 */
	default List<String> getStatements(String databaseType) {
		return Collections.singletonList(getStatemenet(databaseType));
	}
}
