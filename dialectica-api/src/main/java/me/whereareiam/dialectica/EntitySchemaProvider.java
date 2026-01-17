package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.Entity;

/**
 * Interface for providing database-specific DDL statements for entity table creation.
 * <p>
 * Entities can implement this interface directly, or provide a nested class that implements it.
 * The DDL statements should use "CREATE TABLE IF NOT EXISTS" to be idempotent.
 * <p>
 * Example:
 * <pre>{@code
 * @DialectEntity(tableName = "players")
 * public class PlayerEntity implements EntitySchemaProvider {
 *     private UUID uniqueId;
 *
 *     @Override
 *     public String getCreateTableStatement(String type) {
 *         if ("postgres".equals(type)) return "CREATE TABLE IF NOT EXISTS players (...)";
 *         return "CREATE TABLE IF NOT EXISTS players (...)";
 *     }
 * }
 * }</pre>
 */
public interface EntitySchemaProvider {
	/**
	 * Gets the CREATE TABLE DDL statement for this entity.
	 * The statement should use "CREATE TABLE IF NOT EXISTS" to be idempotent.
	 *
	 * @param databaseType the database type identifier (affects syntax like auto-increment, data types)
	 * @return the DDL statement for creating the table
	 */
	String statement(String databaseType);

	/**
	 * Gets the table name for this entity.
	 * If not provided, it will be derived from the {@link Entity} annotation.
	 *
	 * @return the table name, or empty string to use annotation value
	 */
	default String getTableName() {
		return "";
	}
}
