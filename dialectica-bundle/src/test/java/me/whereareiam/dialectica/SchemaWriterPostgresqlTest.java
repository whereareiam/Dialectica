package me.whereareiam.dialectica;

import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Runs the {@link SchemaWriterContract} on PostgreSQL in a container.
 */
@Tag("database-container")
@Testcontainers
class SchemaWriterPostgresqlTest extends SchemaWriterContract {
	@Container
	private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:18.1");

	@Override
	protected Connection open() throws SQLException {
		return DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
	}

	@Override
	protected String identicaDatabaseType() {
		return "POSTGRES";
	}

	@Override
	protected String typeName(ColumnType type) {
		if (type instanceof ColumnType.UuidType) return "UUID";
		if (type instanceof ColumnType.VarcharType) return "VARCHAR";
		if (type instanceof ColumnType.TextType) return "TEXT";
		if (type instanceof ColumnType.BigintType) return "INT8";
		if (type instanceof ColumnType.IntegerType) return "INT4";

		return "BOOL";
	}
}
