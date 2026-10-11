package me.whereareiam.dialectica.common.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads what exists in the current catalog and schema of a connection through its
 * {@link DatabaseMetaData}.
 * <p>
 * Names are matched the way the renderers write them: a lowercase snake_case name is written
 * without quotes, which H2 stores in upper case, so it matches regardless of case; any other name
 * matches exactly.
 */
public final class JdbcInspection {
	private static final Pattern BARE_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]*");
	private static final Set<String> TABLE_TYPES = Set.of("TABLE", "BASE TABLE", "PARTITIONED TABLE");

	private final Connection connection;

	/**
	 * Creates an inspection of the given connection, which stays owned by the caller.
	 *
	 * @param connection connection to inspect
	 */
	public JdbcInspection(@NotNull Connection connection) {
		this.connection = connection;
	}

	/**
	 * Tells whether a table exists.
	 *
	 * @param table name of the table
	 * @return whether the table exists
	 * @throws SQLException when the metadata cannot be read
	 */
	public boolean tableExists(@NotNull String table) throws SQLException {
		return storedTable(table) != null;
	}

	/**
	 * Tells whether a table has a column. A missing table has none.
	 *
	 * @param table  name of the table
	 * @param column name of the column
	 * @return whether the column exists
	 * @throws SQLException when the metadata cannot be read
	 */
	public boolean columnExists(@NotNull String table, @NotNull String column) throws SQLException {
		String stored = storedTable(table);
		if (stored == null) return false;

		DatabaseMetaData metaData = connection.getMetaData();
		try (ResultSet columns = metaData.getColumns(connection.getCatalog(), schema(), escape(metaData, stored), null)) {
			while (columns.next())
				if (stored.equals(columns.getString("TABLE_NAME")) && matches(column, columns.getString("COLUMN_NAME"))) return true;
		}

		return false;
	}

	/**
	 * Tells whether a table has an index. A missing table has none.
	 *
	 * @param table name of the indexed table
	 * @param index name of the index
	 * @return whether the index exists
	 * @throws SQLException when the metadata cannot be read
	 */
	public boolean indexExists(@NotNull String table, @NotNull String index) throws SQLException {
		String stored = storedTable(table);
		if (stored == null) return false;

		try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), schema(), stored, false, true)) {
			while (indexes.next())
				if (matches(index, indexes.getString("INDEX_NAME"))) return true;
		}

		return false;
	}

	/**
	 * Tells whether a column belongs to the primary key, an index (including the ones behind unique
	 * constraints) or a foreign key of its table, or is referenced by a foreign key of another
	 * table.
	 *
	 * @param table  name of the table
	 * @param column name of the column
	 * @return whether a key or an index covers the column
	 * @throws SQLException when the metadata cannot be read
	 */
	public boolean columnKeyed(@NotNull String table, @NotNull String column) throws SQLException {
		String stored = storedTable(table);
		if (stored == null) return false;

		DatabaseMetaData metaData = connection.getMetaData();
		String catalog = connection.getCatalog();
		String schema = schema();
		try (ResultSet keys = metaData.getPrimaryKeys(catalog, schema, stored)) {
			if (contains(keys, "COLUMN_NAME", column)) return true;
		}
		try (ResultSet indexes = metaData.getIndexInfo(catalog, schema, stored, false, true)) {
			if (contains(indexes, "COLUMN_NAME", column)) return true;
		}
		try (ResultSet imported = metaData.getImportedKeys(catalog, schema, stored)) {
			if (contains(imported, "FKCOLUMN_NAME", column)) return true;
		}
		try (ResultSet exported = metaData.getExportedKeys(catalog, schema, stored)) {
			return contains(exported, "PKCOLUMN_NAME", column);
		}
	}

	private @Nullable String storedTable(String table) throws SQLException {
		try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), schema(), "%", null)) {
			while (tables.next()) {
				String type = tables.getString("TABLE_TYPE");
				String name = tables.getString("TABLE_NAME");
				if (type != null && TABLE_TYPES.contains(type) && matches(table, name)) return name;
			}
		}

		return null;
	}

	private @Nullable String schema() throws SQLException {
		return connection.getSchema();
	}

	private static boolean contains(ResultSet rows, String label, String name) throws SQLException {
		while (rows.next())
			if (matches(name, rows.getString(label))) return true;

		return false;
	}

	private static boolean matches(String declared, @Nullable String stored) {
		if (stored == null) return false;
		if (stored.equals(declared)) return true;

		return BARE_IDENTIFIER.matcher(declared).matches() && stored.equalsIgnoreCase(declared);
	}

	private static String escape(DatabaseMetaData metaData, String name) throws SQLException {
		String escape = metaData.getSearchStringEscape();
		if (escape == null || escape.isEmpty()) return name;

		return name.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
	}
}
