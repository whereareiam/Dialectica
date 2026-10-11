package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Drops an index. The table is named because MySQL and MariaDB keep index names per table.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class DropIndex implements SchemaChange {
	/**
	 * Name of the indexed table.
	 */
	private final @NotNull String table;

	/**
	 * Name of the index to drop.
	 */
	private final @NotNull String index;

	/**
	 * Creates the change.
	 *
	 * @param table name of the indexed table
	 * @param index name of the index to drop
	 */
	public DropIndex(@NotNull String table, @NotNull String index) {
		this.table = Objects.requireNonNull(table, "table");
		this.index = Objects.requireNonNull(index, "index");
	}
}
