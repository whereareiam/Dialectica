package me.whereareiam.dialectica.dialect.mysql;

import me.whereareiam.dialectica.common.AnsiDialect;
import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * The dialect of MySQL 8.0.19 or newer, which quotes names with backticks and stores UUIDs as
 * {@code CHAR(36)} text. A MariaDB server reached through MySQL's driver reports itself as MySQL
 * and is handled by this dialect.
 */
public class MysqlDialect extends AnsiDialect {
	private final SchemaRenderer schemaRenderer = new MysqlSchemaRenderer(this);
	private final StatementRenderer statementRenderer = new MysqlStatementRenderer(this);

	@Override
	public @NotNull String name() {
		return "MySQL";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		return "MySQL".equalsIgnoreCase(metaData.getDatabaseProductName());
	}

	@Override
	public @NotNull SchemaRenderer schemaRenderer() {
		return schemaRenderer;
	}

	@Override
	public @NotNull StatementRenderer statementRenderer() {
		return statementRenderer;
	}

	@Override
	protected void bindUuid(@NotNull PreparedStatement statement, int index, @NotNull UUID value) throws SQLException {
		bindUuidAsText(statement, index, value);
	}

	@Override
	protected @Nullable UUID readUuid(@NotNull ResultSet row, int column) throws SQLException {
		return readUuidAsText(row, column);
	}

	@Override
	protected int sqlType(@NotNull ColumnType type) {
		return type instanceof ColumnType.UuidType ? Types.VARCHAR : super.sqlType(type);
	}

	@Override
	protected char quote() {
		return '`';
	}
}
