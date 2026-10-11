package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaConfig;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaPlugin;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Reads and writes single entities of an {@link EntityTable} through Jdbi, with statements the
 * dialect renders. {@link DialecticaPlugin} has to be installed on the {@code Jdbi}.
 * <pre>{@code
 * EntityAccess<Account> accounts = EntityAccess.of(jdbi, Dialectica.entity(Account.class));
 * accounts.upsert(account);
 * Optional<Account> found = accounts.find(account.uniqueId());
 * }</pre>
 * Each call runs on a handle of the {@code Jdbi}. Every operation but {@link #insert(Object)}
 * addresses a row by its primary key and needs the table to have one.
 *
 * @param <T> entity type
 */
public final class EntityAccess<T> {
	private final Jdbi jdbi;
	private final EntityTable<T> table;
	private final Dialect dialect;
	private final EntityRowMapper<T> mapper;
	private final SqlStatement insert;
	private final @Nullable SqlStatement upsert;
	private final @Nullable SqlStatement update;
	private final @Nullable SqlStatement select;
	private final @Nullable SqlStatement delete;

	private EntityAccess(Jdbi jdbi, EntityTable<T> table) {
		this.jdbi = jdbi;
		this.table = table;
		this.dialect = jdbi.getConfig(DialecticaConfig.class).dialect();
		this.mapper = new EntityRowMapper<>(table);

		StatementRenderer renderer = dialect.statementRenderer();
		boolean keyed = !table.primaryKey().isEmpty();
		this.insert = renderer.insert(table.table());
		this.upsert = keyed ? renderer.upsert(table.table()) : null;
		this.update = keyed && table.primaryKey().size() < table.columns().size() ? renderer.update(table.table()) : null;
		this.select = keyed ? renderer.select(table.table()) : null;
		this.delete = keyed ? renderer.delete(table.table()) : null;
	}

	/**
	 * Creates the access to an entity table.
	 *
	 * @param jdbi  Jdbi with {@link DialecticaPlugin} installed
	 * @param table entity table
	 * @param <T>   entity type
	 * @return the access
	 * @throws IllegalStateException when the plugin is not installed
	 */
	public static <T> @NotNull EntityAccess<T> of(@NotNull Jdbi jdbi, @NotNull EntityTable<T> table) {
		return new EntityAccess<>(Objects.requireNonNull(jdbi, "jdbi"), Objects.requireNonNull(table, "table"));
	}

	/**
	 * Returns the entity table this access works on.
	 *
	 * @return the entity table
	 */
	public @NotNull EntityTable<T> table() {
		return table;
	}

	/**
	 * Inserts an entity.
	 *
	 * @param entity entity to insert
	 */
	public void insert(@NotNull T entity) {
		write(insert, entity);
	}

	/**
	 * Inserts an entity, or updates the row with its primary key when there is one. On MySQL and
	 * MariaDB a taken unique key also turns the insert into an update.
	 *
	 * @param entity entity to write
	 */
	public void upsert(@NotNull T entity) {
		write(require(upsert, "upsert"), entity);
	}

	/**
	 * Updates every column outside the primary key of the row with the entity's primary key.
	 *
	 * @param entity entity to write
	 * @return whether a row had that key
	 */
	public boolean update(@NotNull T entity) {
		return write(require(update, "update"), entity) > 0;
	}

	/**
	 * Finds the entity with a primary key.
	 *
	 * @param key values of the primary key columns, in key order
	 * @return the entity, or empty when no row has the key
	 * @throws IllegalArgumentException when the values do not fit the key's columns
	 */
	public @NotNull Optional<T> find(@NotNull Object... key) {
		SqlStatement statement = require(select, "find");
		List<Argument> arguments = key(key);

		return jdbi.withHandle(handle -> bind(handle.createQuery(statement.getSql()), arguments).map(mapper).findOne());
	}

	/**
	 * Deletes the row with a primary key.
	 *
	 * @param key values of the primary key columns, in key order
	 * @return whether a row had that key
	 * @throws IllegalArgumentException when the values do not fit the key's columns
	 */
	public boolean delete(@NotNull Object... key) {
		SqlStatement statement = require(delete, "delete");
		List<Argument> arguments = key(key);

		return jdbi.withHandle(handle -> bind(handle.createUpdate(statement.getSql()), arguments).execute()) > 0;
	}

	private int write(SqlStatement statement, T entity) {
		Objects.requireNonNull(entity, "entity");
		List<Argument> arguments = statement.getParameters().stream()
				.map(name -> argument(table.column(name), entity))
				.toList();

		return jdbi.withHandle(handle -> bind(handle.createUpdate(statement.getSql()), arguments).execute());
	}

	private List<Argument> key(Object[] values) {
		List<EntityColumn<T, ?>> key = table.primaryKey();
		if (values.length != key.size())
			throw new IllegalArgumentException(table.table().getName() + " has a key of " + key.size() + " columns, not " + values.length);

		return IntStream.range(0, values.length)
				.mapToObj(index -> {
					EntityColumn<T, ?> column = key.get(index);
					Object value = values[index];
					if (!column.type().isInstance(value))
						throw new IllegalArgumentException("Key column " + column.name() + " takes a " + column.type().getSimpleName() + ", not " + value);

					return argument(column.column().getType(), value);
				})
				.toList();
	}

	private Argument argument(EntityColumn<T, ?> column, T entity) {
		return argument(column.column().getType(), column.get(entity));
	}

	private Argument argument(ColumnType type, Object value) {
		return (position, statement, context) -> dialect.bind(statement, position, type, value);
	}

	private static <S extends org.jdbi.v3.core.statement.SqlStatement<S>> S bind(S statement, List<Argument> arguments) {
		for (int index = 0; index < arguments.size(); index++)
			statement.bind(index, arguments.get(index));

		return statement;
	}

	private SqlStatement require(@Nullable SqlStatement statement, String operation) {
		if (statement == null)
			throw new IllegalStateException("Cannot " + operation + " " + table.table().getName()
					+ ": it needs a primary key and, to update, a column outside it");

		return statement;
	}
}
