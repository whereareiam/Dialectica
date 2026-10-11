package me.whereareiam.dialectica;

import me.whereareiam.dialectica.exception.UnsupportedChangeException;
import me.whereareiam.dialectica.schema.Schema;
import me.whereareiam.dialectica.schema.change.DropColumn;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs the {@link SchemaWriterContract} on an in-memory SQLite database.
 */
class SchemaWriterSqliteTest extends SchemaWriterContract {
	@Override
	protected Connection open() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite::memory:");
	}

	@Override
	protected String identicaDatabaseType() {
		return "SQLITE";
	}

	@Override
	protected String typeName(ColumnType type) {
		if (type instanceof ColumnType.VarcharType) return "VARCHAR";
		if (type instanceof ColumnType.UuidType || type instanceof ColumnType.TextType) return "TEXT";

		return "INTEGER";
	}

	@Test
	void refusesToDropAColumnThatAKeyOrAnIndexCovers() throws SQLException {
		SchemaWriter writer = Dialectica.on(connection);
		writer.create(Schema.of(ACCOUNTS, LINKS));

		assertThrows(UnsupportedChangeException.class, () -> writer.apply(new DropColumn("dialectica_links", "external_id")));
		assertThrows(UnsupportedChangeException.class, () -> writer.apply(new DropColumn("dialectica_links", "unique_id")));
		assertThrows(UnsupportedChangeException.class, () -> writer.apply(new DropColumn("dialectica_accounts", "username")));
		assertThrows(UnsupportedChangeException.class, () -> writer.apply(new DropColumn("dialectica_accounts", "unique_id")));
	}
}
