package me.whereareiam.dialectica.dialect.mariadb;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.dialect.mysql.MysqlStatementRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * Renders MariaDB's entity statements, which are MySQL's except that an upsert reads the inserted
 * values through {@code VALUES(column)}; MariaDB has no row alias.
 */
public final class MariadbStatementRenderer extends MysqlStatementRenderer {
	/**
	 * Creates the renderer of a dialect.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	public MariadbStatementRenderer(@NotNull Dialect dialect) {
		super(dialect);
	}

	@Override
	protected @NotNull String rowAlias() {
		return "";
	}

	@Override
	protected @NotNull String insertedValue(@NotNull String column) {
		return "VALUES(" + identifier(column) + ")";
	}
}
