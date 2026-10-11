package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Drops a column from a table. SQLite drops only a column that belongs to no key and no index,
 * and needs SQLite 3.35 or newer.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class DropColumn implements SchemaChange {
	/**
	 * Name of the table to shrink.
	 */
	private final @NotNull String table;

	/**
	 * Name of the column to drop.
	 */
	private final @NotNull String column;

	/**
	 * Creates the change.
	 *
	 * @param table  name of the table to shrink
	 * @param column name of the column to drop
	 */
	public DropColumn(@NotNull String table, @NotNull String column) {
		this.table = Objects.requireNonNull(table, "table");
		this.column = Objects.requireNonNull(column, "column");
	}
}
