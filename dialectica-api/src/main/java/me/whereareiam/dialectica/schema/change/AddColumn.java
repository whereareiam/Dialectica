package me.whereareiam.dialectica.schema.change;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.dialectica.schema.ColumnDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Adds a column to an existing table, after its other columns. A column that refuses {@code NULL}
 * needs a default when the table may already hold rows; SQLite always requires one.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class AddColumn implements SchemaChange {
	/**
	 * Name of the table to extend.
	 */
	private final @NotNull String table;

	/**
	 * Column to add.
	 */
	private final @NotNull ColumnDefinition column;

	/**
	 * Creates the change.
	 *
	 * @param table  name of the table to extend
	 * @param column column to add
	 */
	public AddColumn(@NotNull String table, @NotNull ColumnDefinition column) {
		this.table = Objects.requireNonNull(table, "table");
		this.column = Objects.requireNonNull(column, "column");
	}
}
