package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import org.jdbi.v3.core.argument.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An update of some columns of the row with a primary key, started by
 * {@link EntityAccess#update(Object...)}. It sets only the columns it names, and with conditions it
 * changes the row only while the row's current values equal theirs:
 * <pre>{@code
 * accounts.update(uniqueId).set("last_seen_at", now).execute();
 *
 * boolean used = codes.update(uniqueId, code)
 *         .set("used_at", now)
 *         .when("used_at", 0L)
 *         .execute();
 * }</pre>
 * {@link #execute()} runs one statement, so when several instances run the same conditional update
 * of a row at once, exactly one of them changes it and sees {@code true}.
 * <p>
 * Columns are named as in the table, or given as the table's {@link EntityColumn}s. A value has to
 * have the column's Java type, so a {@code bigint()} column takes a {@code Long}: {@code 0L}, not
 * {@code 0}.
 *
 * @param <T> entity type
 */
public final class EntityUpdate<T> {
	private final EntityAccess<T> access;
	private final List<Argument> key;
	private final Map<EntityColumn<T, ?>, @Nullable Object> values = new LinkedHashMap<>();
	private final Map<EntityColumn<T, ?>, @Nullable Object> conditions = new LinkedHashMap<>();

	EntityUpdate(EntityAccess<T> access, List<Argument> key) {
		this.access = access;
		this.key = key;
	}

	/**
	 * Sets a column to a value.
	 *
	 * @param column name of a column outside the primary key
	 * @param value  value of the column's Java type, or {@code null} for a nullable column
	 * @return this update
	 * @throws IllegalArgumentException when the table has no such column, it belongs to the key, is
	 *                                  set already, or the value does not fit it
	 */
	public @NotNull EntityUpdate<T> set(@NotNull String column, @Nullable Object value) {
		return assign(table().column(Objects.requireNonNull(column, "column")), value);
	}

	/**
	 * Sets a column to a value.
	 *
	 * @param column a column of the table, outside the primary key
	 * @param value  value, or {@code null} for a nullable column
	 * @param <V>    Java type of the column's values
	 * @return this update
	 * @throws IllegalArgumentException when the column is not one of the table's, belongs to the key,
	 *                                  is set already, or refuses {@code null}
	 */
	public <V> @NotNull EntityUpdate<T> set(@NotNull EntityColumn<T, V> column, @Nullable V value) {
		return assign(column, value);
	}

	/**
	 * Lets the update change the row only while a column equals a value. A {@code null} value
	 * requires the column to be {@code NULL}.
	 *
	 * @param column name of a column
	 * @param value  value of the column's Java type, or {@code null}
	 * @return this update
	 * @throws IllegalArgumentException when the table has no such column, it has a condition
	 *                                  already, or the value does not fit it
	 */
	public @NotNull EntityUpdate<T> when(@NotNull String column, @Nullable Object value) {
		return compare(table().column(Objects.requireNonNull(column, "column")), value);
	}

	/**
	 * Lets the update change the row only while a column equals a value. A {@code null} value
	 * requires the column to be {@code NULL}.
	 *
	 * @param column a column of the table
	 * @param value  value, or {@code null}
	 * @param <V>    Java type of the column's values
	 * @return this update
	 * @throws IllegalArgumentException when the column is not one of the table's or has a condition
	 *                                  already
	 */
	public <V> @NotNull EntityUpdate<T> when(@NotNull EntityColumn<T, V> column, @Nullable V value) {
		return compare(column, value);
	}

	/**
	 * Runs the update as one statement.
	 *
	 * @return whether the row with the key existed and matched every condition
	 * @throws IllegalStateException when no column is set
	 */
	public boolean execute() {
		if (values.isEmpty())
			throw new IllegalStateException("An update of " + table().table().getName() + " needs a column to set");

		List<String> columns = values.keySet().stream().map(EntityColumn::name).toList();
		List<String> equalities = new ArrayList<>();
		List<String> nulls = new ArrayList<>();
		conditions.forEach((column, value) -> (value == null ? nulls : equalities).add(column.name()));
		SqlStatement statement = access.dialect().statementRenderer().update(table().table(), columns, equalities, nulls);

		List<Argument> arguments = new ArrayList<>();
		values.forEach((column, value) -> arguments.add(access.argument(column.column().getType(), value)));
		arguments.addAll(key);
		conditions.forEach((column, value) -> {
			if (value != null) arguments.add(access.argument(column.column().getType(), value));
		});

		return access.execute(statement, arguments) > 0;
	}

	private EntityUpdate<T> assign(EntityColumn<T, ?> given, @Nullable Object value) {
		EntityColumn<T, ?> column = resolve(given, value);
		if (table().primaryKey().contains(column))
			throw new IllegalArgumentException("Column " + column.name() + " belongs to the primary key of " + table().table().getName() + " and cannot be set");
		if (value == null && !column.column().isNullable())
			throw new IllegalArgumentException("Column " + column.name() + " of " + table().table().getName() + " is not nullable");
		if (values.containsKey(column))
			throw new IllegalArgumentException("Column " + column.name() + " is set already");

		values.put(column, value);
		return this;
	}

	private EntityUpdate<T> compare(EntityColumn<T, ?> given, @Nullable Object value) {
		EntityColumn<T, ?> column = resolve(given, value);
		if (conditions.containsKey(column))
			throw new IllegalArgumentException("Column " + column.name() + " has a condition already");

		conditions.put(column, value);
		return this;
	}

	/**
	 * Finds the table's own column for a column, which may come from another reading of the same
	 * entity, and checks a value against it.
	 */
	private EntityColumn<T, ?> resolve(EntityColumn<T, ?> given, @Nullable Object value) {
		Objects.requireNonNull(given, "column");
		EntityColumn<T, ?> column = table().column(given.name());
		if (!column.column().equals(given.column()))
			throw new IllegalArgumentException(given + " is not a column of " + table().table().getName());
		if (value != null && !column.type().isInstance(value))
			throw new IllegalArgumentException("Column " + column.name() + " takes a " + column.type().getSimpleName() + ", not " + value);

		return column;
	}

	private EntityTable<T> table() {
		return access.table();
	}
}
