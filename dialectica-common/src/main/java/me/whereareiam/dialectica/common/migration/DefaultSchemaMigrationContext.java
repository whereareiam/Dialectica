package me.whereareiam.dialectica.common.migration;

import lombok.RequiredArgsConstructor;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public final class DefaultSchemaMigrationContext implements SchemaMigrationContext {
	private final Jdbi jdbi;
	private final Handle handle;
	private final String databaseType;
	private final String scope;

	@Override
	public @NotNull Jdbi jdbi() {
		return jdbi;
	}

	@Override
	public @NotNull Handle handle() {
		return handle;
	}

	@Override
	public @NotNull String databaseType() {
		return databaseType;
	}

	@Override
	public @NotNull String scope() {
		return scope;
	}

	@Override
	public boolean tableExists(@NotNull String tableName) {
		try {
			DatabaseMetaData metaData = handle.getConnection().getMetaData();
			return hasTable(metaData, tableName) || hasTable(metaData, tableName.toUpperCase());
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to inspect table " + tableName, exception);
		}
	}

	@Override
	public boolean columnExists(
			@NotNull String tableName,
			@NotNull String columnName
	) {
		try {
			DatabaseMetaData metaData = handle.getConnection().getMetaData();
			return hasColumn(metaData, tableName, columnName)
					|| hasColumn(metaData, tableName.toUpperCase(), columnName.toUpperCase());
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to inspect column " + tableName + "." + columnName, exception);
		}
	}

	@Override
	public void execute(@NotNull String sql) {
		handle.execute(sql);
	}

	@Override
	public void renameTable(
			@NotNull String sourceTable,
			@NotNull String targetTable
	) {

		String productName;
		try {
			productName = handle.getConnection().getMetaData().getDatabaseProductName();
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to detect database product for table rename", exception);
		}

		String normalizedProduct = productName == null ? "" : productName.trim().toLowerCase();
		String sql = normalizedProduct.contains("mysql") || normalizedProduct.contains("mariadb")
				? "RENAME TABLE %s TO %s".formatted(sourceTable, targetTable)
				: "ALTER TABLE %s RENAME TO %s".formatted(sourceTable, targetTable);
		handle.execute(sql);
	}

	private boolean hasTable(DatabaseMetaData metaData, String tableName) throws SQLException {
		try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
			return resultSet.next();
		}
	}

	private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
		try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
			return resultSet.next();
		}
	}
}
