package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaConfig;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaPlugin;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.dialect.Upsert;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.exception.KeyConflictException;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Reads and writes single entities of an {@link EntityTable} through Jdbi, with statements the
 * dialect renders. {@link DialecticaPlugin} has to be installed on the {@code Jdbi}.
 * <pre>{@code
 * EntityAccess<Account> accounts = EntityAccess.of(jdbi, Dialectica.entity(Account.class));
 * boolean created = accounts.insertIfAbsent(account);
 * links.upsert(link, "is_primary", "last_seen_at");
 * accounts.update(account.uniqueId()).set("last_seen_at", now).execute();
 * Optional<Account> found = accounts.find(account.uniqueId());
 * }</pre>
 * Each call runs on a handle of the {@code Jdbi}. Every operation but {@link #insert(Object)} and
 * {@link #insertIfAbsent(Object)} addresses a row by its primary key and needs the table to have
 * one.
 *
 * @param <T> entity type
 */
public final class EntityAccess<T> {
	private final Jdbi jdbi;
	private final EntityTable<T> table;
	private final Dialect dialect;
	private final EntityRowMapper<T> mapper;
	private final SqlStatement insert;
	private final SqlStatement insertIfAbsent;
	private final @Nullable Upsert upsert;
	private final @Nullable SqlStatement replace;
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
		this.insertIfAbsent = renderer.insertIfAbsent(table.table());
		this.upsert = keyed ? renderer.upsert(table.table(), valueColumns(table)) : null;
		this.replace = keyed && table.primaryKey().size() < table.columns().size() ? renderer.replace(table.table()) : null;
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
	 * Inserts an entity unless a row already holds its primary key or one of its unique keys, and
	 * leaves that row as it is. Of several instances that insert the same key at once, exactly one
	 * inserts the row and sees {@code true}. Any other failure, such as a missing referenced row,
	 * still throws.
	 *
	 * @param entity entity to insert
	 * @return whether the entity was inserted
	 */
	public boolean insertIfAbsent(@NotNull T entity) {
		Objects.requireNonNull(entity, "entity");
		return jdbi.withHandle(handle -> {
			try {
				return write(handle, insertIfAbsent, entity) > 0;
			} catch (StatementException e) {
				if (isKeyConflict(e)) return false;

				throw e;
			}
		});
	}

	/**
	 * Inserts an entity or, when a row has its primary key, updates every column of that row
	 * outside the key.
	 *
	 * @param entity entity to write
	 * @throws KeyConflictException when another row holds one of the entity's other unique keys
	 * @throws IllegalStateException when the table has no primary key
	 * @see #upsert(Object, String...)
	 */
	public void upsert(@NotNull T entity) {
		Objects.requireNonNull(entity, "entity");
		write(require(upsert, "upsert"), entity);
	}

	/**
	 * Inserts an entity or, when a row has its primary key, updates only the given columns of that
	 * row; the others keep the row's values:
	 * <pre>{@code
	 * links.upsert(link, "is_primary", "last_seen_at");
	 * }</pre>
	 * The primary key decides whether the row exists. Of several instances that write the same key
	 * at once, each one succeeds, and the row ends with the values of the last. On MySQL and MariaDB
	 * connections that set {@code useAffectedRows=true}, an upsert of a table with other unique keys
	 * that leaves the row's values as they are also throws a {@link KeyConflictException}.
	 *
	 * @param entity  entity to write
	 * @param columns names of columns outside the primary key, at least one
	 * @throws KeyConflictException     when another row holds one of the entity's other unique keys
	 * @throws IllegalArgumentException when no column is given, the table has no such column, it
	 *                                  belongs to the key, or it is given twice
	 * @throws IllegalStateException    when the table has no primary key
	 */
	public void upsert(@NotNull T entity, @NotNull String... columns) {
		Objects.requireNonNull(entity, "entity");
		require(upsert, "upsert");
		List<String> names = List.of(Objects.requireNonNull(columns, "columns"));
		if (names.isEmpty())
			throw new IllegalArgumentException("An upsert of some columns of " + table.table().getName() + " needs a column to update");
		names.forEach(table::column);

		write(dialect.statementRenderer().upsert(table.table(), names), entity);
	}

	/**
	 * Inserts an entity or, when a row has its primary key, updates only the given columns of that
	 * row, as {@link #upsert(Object, String...)} does.
	 *
	 * @param entity  entity to write
	 * @param columns columns of the table outside the primary key, at least one
	 * @throws KeyConflictException     when another row holds one of the entity's other unique keys
	 * @throws IllegalArgumentException when no column is given, a column is not one of the table's,
	 *                                  belongs to the key, or is given twice
	 * @throws IllegalStateException    when the table has no primary key
	 */
	@SafeVarargs
	public final void upsert(@NotNull T entity, @NotNull EntityColumn<T, ?>... columns) {
		String[] names = new String[Objects.requireNonNull(columns, "columns").length];
		for (int index = 0; index < columns.length; index++) {
			EntityColumn<T, ?> given = Objects.requireNonNull(columns[index], "column");
			if (!table.column(given.name()).column().equals(given.column()))
				throw new IllegalArgumentException(given + " is not a column of " + table.table().getName());

			names[index] = given.name();
		}

		upsert(entity, names);
	}

	/**
	 * Replaces every column outside the primary key of the row with the entity's primary key by the
	 * entity's values.
	 *
	 * @param entity entity to write
	 * @return whether a row had that key
	 */
	public boolean replace(@NotNull T entity) {
		return write(require(replace, "replace"), entity) > 0;
	}

	/**
	 * Starts an update of some columns of the row with a primary key, which changes only the columns
	 * it sets and, given conditions, only while the row matches them:
	 * <pre>{@code
	 * accounts.update(uniqueId).set("last_seen_at", now).execute();
	 * boolean used = codes.update(uniqueId, code).set("used_at", now).when("used_at", 0L).execute();
	 * }</pre>
	 *
	 * @param key values of the primary key columns, in key order
	 * @return the update, which runs on {@link EntityUpdate#execute()}
	 * @throws IllegalArgumentException when the values do not fit the key's columns
	 * @throws IllegalStateException    when the table has no primary key
	 */
	public @NotNull EntityUpdate<T> update(@NotNull Object... key) {
		if (table.primaryKey().isEmpty())
			throw new IllegalStateException("Cannot update " + table.table().getName() + ": it needs a primary key");

		return new EntityUpdate<>(this, key(key));
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

	/**
	 * Runs a statement that writes rows.
	 *
	 * @param statement statement to run
	 * @param arguments values of its parameters, in order
	 * @return the number of rows the statement matched
	 */
	int execute(SqlStatement statement, List<Argument> arguments) {
		return jdbi.withHandle(handle -> bind(handle.createUpdate(statement.getSql()), arguments).execute());
	}

	/**
	 * Runs an upsert of an entity. An {@link Upsert.InsertThenUpdate} runs its update when the insert
	 * inserted no row or failed with a key conflict, and throws a {@link KeyConflictException} when
	 * the update then matches no row.
	 */
	private void write(Upsert upsert, T entity) {
		jdbi.useHandle(handle -> {
			if (upsert instanceof Upsert.OnConflict onConflict) {
				write(handle, onConflict.statement(), entity);
				return;
			}

			Upsert.InsertThenUpdate insertThenUpdate = (Upsert.InsertThenUpdate) upsert;
			StatementException conflict = null;
			try {
				if (write(handle, insertThenUpdate.insert(), entity) > 0) return;
			} catch (StatementException e) {
				if (!isKeyConflict(e)) throw e;

				conflict = e;
			}
			if (write(handle, insertThenUpdate.update(), entity) == 0)
				throw new KeyConflictException(table.table().getName(), conflict);
		});
	}

	/**
	 * Returns the dialect the statements are rendered for.
	 *
	 * @return the dialect
	 */
	Dialect dialect() {
		return dialect;
	}

	/**
	 * Binds a value of a column type as the dialect stores it.
	 *
	 * @param type  column type
	 * @param value value, or {@code null}
	 * @return the argument
	 */
	Argument argument(ColumnType type, @Nullable Object value) {
		return (position, statement, context) -> dialect.bind(statement, position, type, value);
	}

	private int write(SqlStatement statement, T entity) {
		Objects.requireNonNull(entity, "entity");
		return jdbi.withHandle(handle -> write(handle, statement, entity));
	}

	private int write(Handle handle, SqlStatement statement, T entity) {
		List<Argument> arguments = statement.getParameters().stream()
				.map(name -> argument(table.column(name), entity))
				.toList();

		return bind(handle.createUpdate(statement.getSql()), arguments).execute();
	}

	private boolean isKeyConflict(StatementException exception) {
		for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause())
			if (cause instanceof SQLException sql && dialect.isKeyConflict(sql)) return true;

		return false;
	}

	private static List<String> valueColumns(EntityTable<?> table) {
		return table.columns().stream()
				.filter(column -> !table.primaryKey().contains(column))
				.map(EntityColumn::name)
				.toList();
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

	private static <S extends org.jdbi.v3.core.statement.SqlStatement<S>> S bind(S statement, List<Argument> arguments) {
		for (int index = 0; index < arguments.size(); index++)
			statement.bind(index, arguments.get(index));

		return statement;
	}

	private <S> S require(@Nullable S statement, String operation) {
		if (statement == null)
			throw new IllegalStateException("Cannot " + operation + " " + table.table().getName()
					+ ": it needs a primary key and, to replace, a column outside it");

		return statement;
	}
}
