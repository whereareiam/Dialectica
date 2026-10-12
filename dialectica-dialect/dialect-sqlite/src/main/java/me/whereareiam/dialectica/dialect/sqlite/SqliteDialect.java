package me.whereareiam.dialectica.dialect.sqlite;

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
 * The dialect of SQLite, which stores UUIDs as text and booleans as the integers 1 and 0.
 * Dropping a column needs SQLite 3.35 or newer.
 */
public final class SqliteDialect extends AnsiDialect {
	private final SchemaRenderer schemaRenderer = new SqliteSchemaRenderer(this);
	private final StatementRenderer statementRenderer = new SqliteStatementRenderer(this);

	@Override
	public @NotNull String name() {
		return "SQLite";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		return "SQLite".equalsIgnoreCase(metaData.getDatabaseProductName());
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

	/**
	 * Recognises no exception: SQLite's driver reports every refused constraint, a {@code NULL} in a
	 * {@code NOT NULL} column as well as a taken key, with one code and no state. SQLite's entity
	 * statements resolve the key conflicts they expect with {@code ON CONFLICT}, so they never need
	 * to recognise one.
	 */
	@Override
	public boolean isKeyConflict(@NotNull SQLException exception) {
		return false;
	}

	@Override
	protected int sqlType(@NotNull ColumnType type) {
		return type instanceof ColumnType.UuidType ? Types.VARCHAR : super.sqlType(type);
	}
}
