package me.whereareiam.dialectica;

import me.whereareiam.dialectica.schema.type.ColumnType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Runs the {@link SchemaWriterContract} on an in-memory H2 database.
 */
class SchemaWriterH2Test extends SchemaWriterContract {
	@Override
	protected Connection open() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:mem:");
	}

	@Override
	protected String identicaDatabaseType() {
		return "H2";
	}

	@Override
	protected String typeName(ColumnType type) {
		if (type instanceof ColumnType.UuidType) return "UUID";
		if (type instanceof ColumnType.VarcharType) return "CHARACTER VARYING";
		if (type instanceof ColumnType.TextType) return "CHARACTER LARGE OBJECT";
		if (type instanceof ColumnType.BigintType) return "BIGINT";
		if (type instanceof ColumnType.IntegerType) return "INTEGER";

		return "BOOLEAN";
	}
}
