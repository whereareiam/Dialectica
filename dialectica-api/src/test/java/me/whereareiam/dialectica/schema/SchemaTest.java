package me.whereareiam.dialectica.schema;

import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaTest {
	@Test
	void refusesTwoTablesOfOneName() {
		TableDefinition table = TableDefinition.builder("identica_accounts")
				.column(ColumnDefinition.builder("username", ColumnType.varchar(64)).build())
				.build();

		assertThrows(IllegalArgumentException.class, () -> Schema.of(table, table));
	}
}
