package me.whereareiam.dialectica.dialect.postgresql;

import me.whereareiam.dialectica.common.AnsiDialect;
import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The dialect of PostgreSQL.
 */
public final class PostgresqlDialect extends AnsiDialect {
	private final SchemaRenderer schemaRenderer = new PostgresqlSchemaRenderer(this);
	private final StatementRenderer statementRenderer = new PostgresqlStatementRenderer(this);

	@Override
	public @NotNull String name() {
		return "PostgreSQL";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		return "PostgreSQL".equalsIgnoreCase(metaData.getDatabaseProductName());
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
