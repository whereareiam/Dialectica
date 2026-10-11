package me.whereareiam.dialectica.common.entity;

import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.entity.Row;
import me.whereareiam.dialectica.schema.TableDefinition;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The table of an annotated entity, which builds entities through reflection.
 *
 * @param <T> entity type
 */
final class ReflectedEntityTable<T> extends EntityTable<T> {
	private final Function<Object[], T> instantiation;

	ReflectedEntityTable(
			Class<T> type,
			String name,
			List<EntityColumn<T, ?>> columns,
			UnaryOperator<TableDefinition.Builder> constraints,
			Function<Object[], T> instantiation
	) {
		super(type, name, columns, constraints);
		this.instantiation = instantiation;
	}

	@Override
	public @NotNull T create(@NotNull Row<T> row) {
		List<EntityColumn<T, ?>> columns = columns();
		Object[] values = new Object[columns.size()];
		for (int index = 0; index < values.length; index++)
			values[index] = row.get(columns.get(index));

		return instantiation.apply(values);
	}

	/**
	 * Finds how to build an entity from its column values, in field order.
	 */
	static <T> Function<Object[], T> instantiation(Class<T> type, List<Field> fields) {
		Class<?>[] parameterTypes = fields.stream().map(Field::getType).toArray(Class<?>[]::new);
		Constructor<T> allColumns = constructor(type, parameterTypes);
		if (allColumns != null) return values -> construct(allColumns, values);
		if (type.isRecord()) throw new IllegalArgumentException(type.getName() + " has no canonical constructor");

		Constructor<T> empty = constructor(type);
		if (empty == null)
			throw new IllegalArgumentException(type.getName() + " needs a constructor that takes every column field in declaration order, or one without parameters");

		List<Writer> writers = fields.stream().map(field -> writer(type, field)).toList();

		return values -> {
			T entity = construct(empty);
			for (int index = 0; index < values.length; index++)
				writers.get(index).write(entity, values[index]);

			return entity;
		};
	}

	private static <T> Constructor<T> constructor(Class<T> type, Class<?>... parameterTypes) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);

			return constructor;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static <T> T construct(Constructor<T> constructor, Object... values) {
		try {
			return constructor.newInstance(values);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("Cannot build " + constructor.getDeclaringClass().getName() + " from a row: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(constructor + " failed", e.getCause());
		}
	}

	private static Writer writer(Class<?> type, Field field) {
		String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
		try {
			Method setter = type.getDeclaredMethod(setterName, field.getType());
			setter.setAccessible(true);

			return (entity, value) -> setter.invoke(entity, value);
		} catch (NoSuchMethodException e) {
			field.setAccessible(true);

			return field::set;
		}
	}

	@FunctionalInterface
	private interface Writer {
		void apply(Object entity, Object value) throws ReflectiveOperationException;

		default void write(Object entity, Object value) {
			try {
				apply(entity, value);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException("Cannot write a row value into " + entity.getClass().getName(), e.getCause());
			} catch (ReflectiveOperationException | IllegalArgumentException e) {
				throw new IllegalStateException("Cannot write a row value into " + entity.getClass().getName() + ": " + e.getMessage(), e);
			}
		}
	}
}
