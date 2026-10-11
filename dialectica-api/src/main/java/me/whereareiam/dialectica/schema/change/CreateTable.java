package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Creates a table with its keys, followed by its indexes. The tables its foreign keys reference
 * have to exist already.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class CreateTable implements SchemaChange {
	/**
	 * Table to create.
	 */
	private final @NotNull TableDefinition table;

	/**
	 * Creates the change.
	 *
	 * @param table table to create
	 */
	public CreateTable(@NotNull TableDefinition table) {
		this.table = Objects.requireNonNull(table, "table");
	}
}
