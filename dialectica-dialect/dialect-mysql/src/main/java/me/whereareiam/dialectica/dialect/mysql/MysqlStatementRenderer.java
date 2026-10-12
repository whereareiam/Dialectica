package me.whereareiam.dialectica.dialect.mysql;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.common.dialect.AnsiStatementRenderer;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.Upsert;
import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders MySQL's entity statements. An upsert of a table without other unique keys is an
 * {@code INSERT ... ON DUPLICATE KEY UPDATE} that reads the inserted values through the row alias
 * {@code excluded}, which needs MySQL 8.0.19 or newer; of any other table, an insert followed by an
 * update. An insert if absent is a plain insert: {@code INSERT IGNORE} would also skip or alter rows
 * for other errors, and {@code ON DUPLICATE KEY UPDATE} reports a kept row like an inserted one.
 */
public class MysqlStatementRenderer extends AnsiStatementRenderer {
	/**
	 * Creates the renderer of a dialect.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	public MysqlStatementRenderer(@NotNull Dialect dialect) {
		super(dialect);
	}

	@Override
	public @NotNull SqlStatement insertIfAbsent(@NotNull TableDefinition table) {
		return insert(table);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * {@code ON DUPLICATE KEY UPDATE} resolves a conflict on any unique key, so it is rendered only
	 * for a table whose primary key is its only unique key.
	 */
	@Override
	public @NotNull Upsert upsert(@NotNull TableDefinition table, @NotNull List<String> columns) {
		requireUpsert(table, columns);
		if (hasOtherUniqueKeys(table)) return insertThenUpdate(table, columns, insert(table));

		String assignments = columns.isEmpty()
				? identifier(table.getPrimaryKey().get(0)) + " = " + identifier(table.getPrimaryKey().get(0))
				: join(columns, ", ", column -> identifier(column) + " = " + insertedValue(column));

		return new Upsert.OnConflict(new SqlStatement(insert(table).getSql() + " " + rowAlias() + "ON DUPLICATE KEY UPDATE " + assignments,
				columns(table)));
	}

	/**
	 * Returns what follows the inserted values to name them, with a trailing space, or nothing.
	 *
	 * @return the row alias clause
	 */
	protected @NotNull String rowAlias() {
		return "AS excluded ";
	}

	/**
	 * Refers to the value the insert would have written into a column.
	 *
	 * @param column name of the column
	 * @return the reference
	 */
	protected @NotNull String insertedValue(@NotNull String column) {
		return "excluded." + identifier(column);
	}
}
