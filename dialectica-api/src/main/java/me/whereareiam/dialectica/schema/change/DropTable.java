package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Drops a table with its rows and indexes.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class DropTable implements SchemaChange {
	/**
	 * Name of the table to drop.
	 */
	private final @NotNull String table;

	/**
	 * Creates the change.
	 *
	 * @param table name of the table to drop
	 */
	public DropTable(@NotNull String table) {
		this.table = Objects.requireNonNull(table, "table");
	}
}
