package me.whereareiam.dialectica.dialect.mariadb;

import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.dialect.mysql.MysqlDialect;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The dialect of MariaDB, reached through MariaDB's own driver. It is MySQL's dialect except for a
 * literal default of a {@code TEXT} column and the way an upsert reads the inserted values.
 */
public final class MariadbDialect extends MysqlDialect {
	private final SchemaRenderer schemaRenderer = new MariadbSchemaRenderer(this);
	private final StatementRenderer statementRenderer = new MariadbStatementRenderer(this);

	@Override
	public @NotNull String name() {
		return "MariaDB";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		return "MariaDB".equalsIgnoreCase(metaData.getDatabaseProductName());
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
