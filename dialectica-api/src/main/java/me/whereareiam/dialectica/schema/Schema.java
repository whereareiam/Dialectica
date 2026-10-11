package me.whereareiam.dialectica.schema;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The tables one owner creates together, such as the tables of one plugin or one of its parts.
 * Tables may reference each other in any declaration order; they are created so that a referenced
 * table exists before the tables that reference it. A table of another owner is referenced by its
 * name and has to exist already.
 * <pre>{@code
 * Schema schema = Schema.of(links, accounts);
 * Dialectica.on(connection).create(schema);   // creates accounts, then links
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Schema {
	/**
	 * Tables, in the order they were declared.
	 */
	private final @NotNull List<TableDefinition> tables;

	private Schema(@NotNull List<TableDefinition> tables) {
		this.tables = tables;
	}

	/**
	 * Creates a schema of the given tables.
	 *
	 * @param tables tables of the schema, in any order
	 * @return the schema
	 * @throws IllegalArgumentException when two tables have the same name
	 */
	public static @NotNull Schema of(@NotNull TableDefinition... tables) {
		List<TableDefinition> declared = List.copyOf(Arrays.asList(Objects.requireNonNull(tables, "tables")));
		Set<String> names = new HashSet<>();
		for (TableDefinition table : declared)
			if (!names.add(table.getName()))
				throw new IllegalArgumentException("The schema declares table " + table.getName() + " twice");

		return new Schema(declared);
	}
}
