package me.whereareiam.dialectica.schema;

import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForeignKeyDefinitionTest {
	@Test
	void takesNoActionUnlessTold() {
		ForeignKeyDefinition foreignKey = ForeignKeyDefinition.builder("unique_id").references("identica_accounts", "unique_id").build();

		assertEquals(ReferentialAction.NO_ACTION, foreignKey.getOnDelete());
	}

	@Test
	void refusesColumnsOfDifferentNumber() {
		ForeignKeyDefinition.Builder builder = ForeignKeyDefinition.builder("provider_id", "provider_subject")
				.references("identica_provider_links", "provider_id");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void refusesAForeignKeyWithoutReference() {
		assertThrows(NullPointerException.class, () -> ForeignKeyDefinition.builder("unique_id").build());
	}
}
