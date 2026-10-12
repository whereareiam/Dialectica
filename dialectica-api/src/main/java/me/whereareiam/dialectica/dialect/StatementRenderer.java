package me.whereareiam.dialectica.dialect;

import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders the statements that read and write single rows of a table in one dialect, naming
 * tables and columns exactly as the dialect's {@link SchemaRenderer} does.
 * <p>
 * Every statement but {@link #insert(TableDefinition)} and {@link #insertIfAbsent(TableDefinition)}
 * addresses a row by its primary key and refuses a table without one with an
 * {@link IllegalArgumentException}.
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
	 * Renders the insert of a row that leaves the table as it is when a row already holds the
	 * inserted row's primary key or one of its unique keys. Any other failure, such as a missing
	 * referenced row, still fails the statement.
	 * <p>
	 * PostgreSQL and SQLite write {@code INSERT ... ON CONFLICT DO NOTHING}, which then changes no
	 * row. MySQL, MariaDB and H2 have no such clause, so the statement is a plain insert that fails
	 * with an error {@link me.whereareiam.dialectica.Dialect#isKeyConflict(java.sql.SQLException)}
	 * recognises; their failed statement does not end a transaction around it. Either way, of
	 * several instances that insert the same key at once, exactly one inserts the row.
	 *
	 * @param table table to insert into
	 * @return the statement, parameterized by every column in table order
	 */
	@NotNull SqlStatement insertIfAbsent(@NotNull TableDefinition table);

	/**
	 * Renders the write of a row that inserts it, or, when a row has its primary key, updates only
	 * the given columns of that row. With no columns, the existing row stays as it is. A conflict
	 * on another unique key of the table fails the write.
	 * <p>
	 * For a table whose primary key is its only unique key, PostgreSQL and SQLite render
	 * {@code INSERT ... ON CONFLICT (key) DO UPDATE SET column = excluded.column}, and MySQL and
	 * MariaDB {@code INSERT ... ON DUPLICATE KEY UPDATE}, each as {@link Upsert.OnConflict one
	 * statement}. For any other table, and always on H2, they render {@link Upsert.InsertThenUpdate
	 * an insert and an update}, so that a conflict on another unique key fails alike everywhere and
	 * concurrent writes of the same key all succeed: PostgreSQL's {@code ON CONFLICT (key)} lets a
	 * concurrent insert fail on another unique key, and MySQL's and MariaDB's statement cannot tell
	 * the keys apart.
	 *
	 * @param table   table to write to
	 * @param columns columns outside the primary key to update when the row exists
	 * @return the upsert
	 * @throws IllegalArgumentException when the table has no primary key, a column belongs to the
	 *                                  key, a column is not one of the table's, or a column is
	 *                                  given twice
	 */
	@NotNull Upsert upsert(@NotNull TableDefinition table, @NotNull List<String> columns);

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
