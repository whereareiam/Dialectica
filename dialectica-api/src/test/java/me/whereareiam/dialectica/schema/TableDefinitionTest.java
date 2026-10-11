package me.whereareiam.dialectica.schema;

import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableDefinitionTest {
	private static ColumnDefinition required(String name) {
		return ColumnDefinition.builder(name, ColumnType.varchar(64)).notNull().build();
	}

	@Test
	void keepsKeysAndIndexesInDeclarationOrder() {
		TableDefinition table = TableDefinition.builder("identica_provider_links")
				.column(required("provider_id"))
				.column(required("provider_subject"))
				.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).build())
				.primaryKey("provider_id", "provider_subject")
				.unique("unique_id", "provider_id")
				.index(IndexDefinition.unique("identica_provider_links_subject", "provider_subject"))
				.build();

		assertEquals(List.of("provider_id", "provider_subject"), table.getPrimaryKey());
		assertEquals(List.of(List.of("unique_id", "provider_id")), table.getUniqueKeys());
		assertEquals("identica_provider_links_subject", table.getIndexes().get(0).getName());
	}

	@Test
	void refusesANullablePrimaryKeyColumn() {
		TableDefinition.Builder builder = TableDefinition.builder("identica_accounts")
				.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).build())
				.primaryKey("unique_id");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void refusesAnUnknownColumnInAKeyOrAnIndex() {
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts")
				.column(required("username"))
				.unique("missing")
				.build());
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts")
				.column(required("username"))
				.index(IndexDefinition.plain("identica_accounts_missing", "missing"))
				.build());
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts")
				.column(required("username"))
				.foreignKey(ForeignKeyDefinition.builder("missing").references("identica_other", "id").build())
				.build());
	}

	@Test
	void refusesADuplicateColumnOrIndex() {
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts")
				.column(required("username"))
				.column(required("username"))
				.build());
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts")
				.column(required("username"))
				.index(IndexDefinition.plain("identica_accounts_username", "username"))
				.index(IndexDefinition.unique("identica_accounts_username", "username"))
				.build());
	}

	@Test
	void refusesATableWithoutColumns() {
		assertThrows(IllegalArgumentException.class, () -> TableDefinition.builder("identica_accounts").build());
	}
}
