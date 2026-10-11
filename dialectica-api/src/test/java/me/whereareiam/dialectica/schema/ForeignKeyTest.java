package me.whereareiam.dialectica.schema;

import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForeignKeyTest {
	@Test
	void takesNoActionUnlessTold() {
		ForeignKey foreignKey = ForeignKey.builder("unique_id").references("identica_accounts", "unique_id").build();

		assertEquals(ReferentialAction.NO_ACTION, foreignKey.getOnDelete());
	}

	@Test
	void refusesColumnsOfDifferentNumber() {
		ForeignKey.Builder builder = ForeignKey.builder("provider_id", "provider_subject")
				.references("identica_provider_links", "provider_id");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void refusesAForeignKeyWithoutReference() {
		assertThrows(NullPointerException.class, () -> ForeignKey.builder("unique_id").build());
	}
}
