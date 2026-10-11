package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Renames a table. Its rows, keys and indexes stay; index names do not follow the new table name.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class RenameTable implements SchemaChange {
	/**
	 * Current name of the table.
	 */
	private final @NotNull String from;

	/**
	 * New name of the table.
	 */
	private final @NotNull String to;

	/**
	 * Creates the change.
	 *
	 * @param from current name of the table
	 * @param to   new name of the table
	 */
	public RenameTable(@NotNull String from, @NotNull String to) {
		this.from = Objects.requireNonNull(from, "from");
		this.to = Objects.requireNonNull(to, "to");
	}
}
