package me.whereareiam.dialectica.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The values of one row, by column, from which {@link EntityTable#create(Row)} builds an entity.
 *
 * @param <T> type of the entity
 */
public interface Row<T> {
	/**
	 * Returns the value of a column in this row.
	 *
	 * @param column column of the entity's table
	 * @param <V>    Java type of the column's values
	 * @return the value, or {@code null} for SQL {@code NULL} or a column the row was not read with
	 */
	<V> @Nullable V get(@NotNull EntityColumn<T, V> column);
}
