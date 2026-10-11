package me.whereareiam.dialectica.dialect.mysql;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.common.dialect.AnsiStatementRenderer;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders MySQL's entity statements; an upsert is an {@code INSERT ... ON DUPLICATE KEY UPDATE} that
 * reads the inserted values through the row alias {@code excluded}, which needs MySQL 8.0.19 or
 * newer.
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
	public @NotNull SqlStatement upsert(@NotNull TableDefinition table) {
		requireKey(table);
		List<String> values = valueColumns(table);
		List<String> updated = values.isEmpty() ? table.getPrimaryKey().subList(0, 1) : values;

		return new SqlStatement(insert(table).getSql() + " " + rowAlias() + "ON DUPLICATE KEY UPDATE "
				+ join(updated, ", ", column -> identifier(column) + " = " + insertedValue(column)), columns(table));
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
