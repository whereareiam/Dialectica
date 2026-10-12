package me.whereareiam.dialectica.dialect;

import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders the statements that read and write single rows of a table in one dialect, naming
 * tables and columns exactly as the dialect's {@link SchemaRenderer} does.
 * <p>
 * Every statement but {@link #insert(TableDefinition)} addresses a row by its primary key and refuses a
 * table without one with an {@link IllegalArgumentException}.
 */
public interface StatementRenderer {
	/**
	 * Renders the insert of a row with every column.
	 *
	 * @param table table to insert into
	 * @return the statement, parameterized by every column in table order
	 */
	@NotNull SqlStatement insert(@NotNull TableDefinition table);

	/**
	 * Renders an insert that updates the row instead when its primary key is taken. On MySQL and
	 * MariaDB a taken unique key also turns the insert into an update.
	 *
	 * @param table table to write to
	 * @return the statement, parameterized by every column in table order
	 */
	@NotNull SqlStatement upsert(@NotNull TableDefinition table);

	/**
	 * Renders the update of every column outside the primary key of the row with a primary key,
	 * which replaces the row's values.
	 *
	 * @param table table to update
	 * @return the statement, parameterized by the columns outside the key, then the key
	 * @throws IllegalArgumentException when every column belongs to the primary key
	 */
	@NotNull SqlStatement replace(@NotNull TableDefinition table);

	/**
	 * Renders the update of some columns of the row with a primary key, which changes the row only
	 * while its current values meet conditions:
	 * <pre>{@code
	 * UPDATE table SET column = ?, ... WHERE key = ? AND ... AND condition AND ...
	 * }</pre>
	 * Each condition renders as its {@link Comparison}, such as {@code column = ?},
	 * {@code column IS NULL} or {@code column < ?}, in the order given. The update is one statement,
	 * so of several instances that update the same row under the same conditions at once, only the
	 * first changes it.
	 *
	 * @param table      table to update
	 * @param columns    columns to set, outside the primary key; at least one
	 * @param conditions conditions on the row's current values
	 * @return the statement, parameterized by the columns to set, then the key, then the columns of
	 *         the conditions that {@linkplain Comparison#takesValue() take a value}
	 * @throws IllegalArgumentException when the table has no primary key, no columns are given, a
	 *                                  column to set belongs to the key, a column is not one of the
	 *                                  table's, a column is set twice, a condition is given twice,
	 *                                  or an {@linkplain Comparison#isOrdered() ordered} condition
	 *                                  compares a column that does not hold numbers
	 */
	@NotNull SqlStatement update(
			@NotNull TableDefinition table,
			@NotNull List<String> columns,
			@NotNull List<Condition> conditions
	);

	/**
	 * Renders the select of every column of the row with a primary key.
	 *
	 * @param table table to read
	 * @return the statement, parameterized by the primary key
	 */
	@NotNull SqlStatement select(@NotNull TableDefinition table);

	/**
	 * Renders the delete of the row with a primary key.
	 *
	 * @param table table to delete from
	 * @return the statement, parameterized by the primary key
	 */
	@NotNull SqlStatement delete(@NotNull TableDefinition table);
}
