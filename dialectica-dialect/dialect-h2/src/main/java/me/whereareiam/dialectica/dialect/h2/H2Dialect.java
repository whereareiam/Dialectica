package me.whereareiam.dialectica.dialect.h2;

import me.whereareiam.dialectica.common.AnsiDialect;
import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The dialect of H2 2.x in its default mode.
 */
public final class H2Dialect extends AnsiDialect {
	private final SchemaRenderer schemaRenderer = new H2SchemaRenderer(this);
	private final StatementRenderer statementRenderer = new H2StatementRenderer(this);

	@Override
	public @NotNull String name() {
		return "H2";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		return "H2".equalsIgnoreCase(metaData.getDatabaseProductName());
	}

	@Override
	public @NotNull SchemaRenderer schemaRenderer() {
		return schemaRenderer;
	}

	@Override
	public @NotNull StatementRenderer statementRenderer() {
		return statementRenderer;
	}
}
