package me.whereareiam.dialectica.dialect.h2;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.common.dialect.AnsiStatementRenderer;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Renders H2's entity statements; an upsert is a {@code MERGE INTO ... KEY}.
 */
public final class H2StatementRenderer extends AnsiStatementRenderer {
	/**
	 * Creates the renderer of a dialect.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	public H2StatementRenderer(@NotNull Dialect dialect) {
		super(dialect);
	}

	@Override
	public @NotNull SqlStatement upsert(@NotNull TableDefinition table) {
		requireKey(table);

		return new SqlStatement("MERGE INTO " + identifier(table.getName()) + " " + identifiers(columns(table))
				+ " KEY " + identifiers(table.getPrimaryKey()) + " VALUES " + markers(table), columns(table));
	}
}
