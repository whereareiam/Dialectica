package me.whereareiam.dialectica.dialect.postgresql;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.common.dialect.AnsiStatementRenderer;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.schema.Table;
import org.jetbrains.annotations.NotNull;

/**
 * Renders PostgreSQL's entity statements; an upsert is an {@code INSERT ... ON CONFLICT}.
 */
public final class PostgresqlStatementRenderer extends AnsiStatementRenderer {
	/**
	 * Creates the renderer of a dialect.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	public PostgresqlStatementRenderer(@NotNull Dialect dialect) {
		super(dialect);
	}

	@Override
	public @NotNull SqlStatement upsert(@NotNull Table table) {
		return insertOnConflict(table);
	}
}
