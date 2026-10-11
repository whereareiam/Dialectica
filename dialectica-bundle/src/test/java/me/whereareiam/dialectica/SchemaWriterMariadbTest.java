package me.whereareiam.dialectica;

import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Runs the {@link SchemaWriterContract} on MariaDB in a container.
 */
@Tag("database-container")
@Testcontainers
class SchemaWriterMariadbTest extends SchemaWriterContract {
	@Container
	private static final MariaDBContainer<?> DATABASE = new MariaDBContainer<>("mariadb:12");

	@Override
	protected Connection open() throws SQLException {
		return DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
	}

	@Override
	protected String identicaDatabaseType() {
		return "MYSQL";
	}

	@Override
	protected String typeName(ColumnType type) {
		if (type instanceof ColumnType.UuidType) return "CHAR";
		if (type instanceof ColumnType.VarcharType) return "VARCHAR";
		if (type instanceof ColumnType.TextType) return "TEXT";
		if (type instanceof ColumnType.BigintType) return "BIGINT";
		if (type instanceof ColumnType.IntegerType) return "INT";

		return "BOOLEAN";
	}
}
