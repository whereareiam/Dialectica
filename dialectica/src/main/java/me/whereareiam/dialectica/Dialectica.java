package me.whereareiam.dialectica;

import me.whereareiam.dialectica.common.entity.EntityReader;
import me.whereareiam.dialectica.common.schema.JdbcInspection;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.entity.annotation.Table;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Entry point of Dialectica: reads the table of an annotated entity and writes schemas to a
 * database.
 * <pre>{@code
 * EntityTable<Account> accounts = Dialectica.entity(Account.class);
 * try (Connection connection = dataSource.getConnection()) {
 *     Dialectica.on(connection).create(Schema.of(accounts.table()));
 * }
 * }</pre>
 */
public final class Dialectica {
	private Dialectica() {
	}

	/**
	 * Reads the table of an entity type from its annotations.
	 *
	 * @param type entity type, annotated with {@link Table}
	 * @param <T>  entity type
	 * @return the entity's table
	 * @throws IllegalArgumentException when the type is not a valid entity, naming the type and the
	 *                                  field at fault
	 */
	public static <T> @NotNull EntityTable<T> entity(@NotNull Class<T> type) {
		return EntityReader.read(Objects.requireNonNull(type, "type"));
	}

	/**
	 * Returns a writer for the database behind a connection. The connection's metadata selects the
	 * {@link Dialect}; the connection stays owned by the caller.
	 *
	 * @param connection connection to write through
	 * @return the writer
	 * @throws SQLException             when the metadata cannot be read
	 * @throws IllegalArgumentException when no available dialect accepts the database
	 */
	public static @NotNull SchemaWriter on(@NotNull Connection connection) throws SQLException {
		Dialect dialect = Dialect.of(connection.getMetaData());

		return new SchemaWriter(connection, dialect, new JdbcInspection(connection));
	}
}
