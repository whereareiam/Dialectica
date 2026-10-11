package me.whereareiam.dialectica.entity;

import me.whereareiam.dialectica.schema.Column;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * A typed column of an {@link EntityTable}: the schema {@link Column} together with the Java type
 * of its values and the accessor that reads the value from an entity.
 * <p>
 * The Java type has to match the column type:
 * <table>
 *     <caption>Java types of column types</caption>
 *     <tr><th>Column type</th><th>Java type</th></tr>
 *     <tr><td>{@link ColumnType#uuid()}</td><td>{@link UUID}</td></tr>
 *     <tr><td>{@link ColumnType#varchar(int)}, {@link ColumnType#text()}</td><td>{@link String}</td></tr>
 *     <tr><td>{@link ColumnType#bigint()}</td><td>{@link Long}</td></tr>
 *     <tr><td>{@link ColumnType#integer()}</td><td>{@link Integer}</td></tr>
 *     <tr><td>{@link ColumnType#bool()}</td><td>{@link Boolean}</td></tr>
 * </table>
 * <pre>{@code
 * static final EntityColumn<Account, UUID> UNIQUE_ID = EntityColumn.of(UUID.class,
 *         Column.builder("unique_id", ColumnType.uuid()).notNull().build(),
 *         Account::uniqueId);
 * }</pre>
 *
 * @param <T> type of the entity
 * @param <V> Java type of the column's values
 */
public final class EntityColumn<T, V> {
	private final @NotNull Column column;
	private final @NotNull Class<V> type;
	private final @NotNull Function<T, V> accessor;

	private EntityColumn(@NotNull Class<V> type, @NotNull Column column, @NotNull Function<T, V> accessor) {
		this.type = type;
		this.column = column;
		this.accessor = accessor;
	}

	/**
	 * Creates an entity column.
	 *
	 * @param type     Java type of the values, a wrapper type for primitives
	 * @param column   schema column
	 * @param accessor reads the column's value from an entity
	 * @param <T>      type of the entity
	 * @param <V>      Java type of the values
	 * @return the entity column
	 * @throws IllegalArgumentException when the Java type does not match the column type
	 */
	public static <T, V> @NotNull EntityColumn<T, V> of(
			@NotNull Class<V> type,
			@NotNull Column column,
			@NotNull Function<T, V> accessor
	) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(column, "column");
		Objects.requireNonNull(accessor, "accessor");
		if (!javaType(column.getType()).equals(type))
			throw new IllegalArgumentException("Column " + column.getName() + " of type " + column.getType()
					+ " holds " + javaType(column.getType()).getSimpleName() + " values, not " + type.getSimpleName());

		return new EntityColumn<>(type, column, accessor);
	}

	/**
	 * Returns the Java type that holds values of a column type.
	 *
	 * @param type column type
	 * @return the Java type of its values
	 */
	public static @NotNull Class<?> javaType(@NotNull ColumnType type) {
		if (type instanceof ColumnType.UuidType) return UUID.class;
		if (type instanceof ColumnType.VarcharType || type instanceof ColumnType.TextType) return String.class;
		if (type instanceof ColumnType.BigintType) return Long.class;
		if (type instanceof ColumnType.IntegerType) return Integer.class;
		if (type instanceof ColumnType.BoolType) return Boolean.class;

		throw new IllegalArgumentException("Unknown column type " + type);
	}

	/**
	 * Returns the name of the column.
	 *
	 * @return the column name
	 */
	public @NotNull String name() {
		return column.getName();
	}

	/**
	 * Returns the schema column.
	 *
	 * @return the schema column
	 */
	public @NotNull Column column() {
		return column;
	}

	/**
	 * Returns the Java type of the column's values.
	 *
	 * @return the Java type
	 */
	public @NotNull Class<V> type() {
		return type;
	}

	/**
	 * Reads the column's value from an entity.
	 *
	 * @param entity entity to read
	 * @return the value, or {@code null}
	 */
	public @Nullable V get(@NotNull T entity) {
		return accessor.apply(entity);
	}

	@Override
	public String toString() {
		return "EntityColumn[" + column.getName() + " " + column.getType() + "]";
	}
}
