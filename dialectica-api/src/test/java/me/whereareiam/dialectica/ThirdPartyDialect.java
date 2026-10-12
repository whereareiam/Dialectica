package me.whereareiam.dialectica;

import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A dialect that is not part of Dialectica, registered through this module's test resources.
 */
public class ThirdPartyDialect implements Dialect {
	@Override
	public @NotNull String name() {
		return "ThirdParty";
	}

	@Override
	public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
		String product = metaData.getDatabaseProductName();

		return "ThirdParty".equals(product) || "Contested".equals(product);
	}

	@Override
	public @NotNull SchemaRenderer schemaRenderer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull StatementRenderer statementRenderer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull String identifier(@NotNull String name) {
		return name;
	}

	@Override
	public void bind(@NotNull PreparedStatement statement, int index, @NotNull ColumnType type, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object read(@NotNull ResultSet row, int column, @NotNull ColumnType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isKeyConflict(@NotNull SQLException exception) {
		return false;
	}

	/**
	 * A second dialect that claims one of the same databases.
	 */
	public static final class Rival extends ThirdPartyDialect {
		@Override
		public @NotNull String name() {
			return "Rival";
		}

		@Override
		public boolean accepts(@NotNull DatabaseMetaData metaData) throws SQLException {
			return "Contested".equals(metaData.getDatabaseProductName());
		}
	}
}
