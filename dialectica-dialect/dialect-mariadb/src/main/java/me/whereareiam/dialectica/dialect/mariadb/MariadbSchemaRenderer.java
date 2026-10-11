package me.whereareiam.dialectica.dialect.mariadb;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.dialect.mysql.MysqlSchemaRenderer;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;

/**
 * Renders MariaDB's SQL, which is MySQL's except that a {@code TEXT} column takes a plain literal
 * default.
 */
public final class MariadbSchemaRenderer extends MysqlSchemaRenderer {
	/**
	 * Creates the renderer of a dialect.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	public MariadbSchemaRenderer(@NotNull Dialect dialect) {
		super(dialect);
	}

	@Override
	protected @NotNull String defaultValue(@NotNull ColumnType type, @NotNull Object value) {
		if (value instanceof String text) return string(text);

		return super.defaultValue(type, value);
	}
}
