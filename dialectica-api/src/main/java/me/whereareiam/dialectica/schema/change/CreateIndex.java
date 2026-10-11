package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.dialectica.schema.IndexDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Creates an index on an existing table.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class CreateIndex implements SchemaChange {
	/**
	 * Name of the indexed table.
	 */
	private final @NotNull String table;

	/**
	 * Index to create.
	 */
	private final @NotNull IndexDefinition index;

	/**
	 * Creates the change.
	 *
	 * @param table name of the indexed table
	 * @param index index to create
	 */
	public CreateIndex(@NotNull String table, @NotNull IndexDefinition index) {
		this.table = Objects.requireNonNull(table, "table");
		this.index = Objects.requireNonNull(index, "index");
	}
}
