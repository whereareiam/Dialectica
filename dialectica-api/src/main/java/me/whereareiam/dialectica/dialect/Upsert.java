package me.whereareiam.dialectica.dialect;

import me.whereareiam.dialectica.Dialect;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * How a dialect writes a row that may already exist under its primary key: it inserts the row, or
 * updates some of the existing row's columns. A conflict on another unique key of the table fails
 * the write with a {@link me.whereareiam.dialectica.exception.KeyConflictException} on every
 * dialect.
 * <p>
 * For a table whose primary key is its only unique key, a dialect whose insert can resolve a
 * conflict on that key renders {@link OnConflict one statement}. Otherwise it renders
 * {@link InsertThenUpdate an insert and an update}, run one after the other.
 */
public sealed interface Upsert permits Upsert.OnConflict, Upsert.InsertThenUpdate {
	/**
	 * One statement that inserts the row and, when a row has its primary key, updates that row's
	 * columns instead, such as {@code INSERT ... ON CONFLICT (key) DO UPDATE SET ...}.
	 *
	 * @param statement the statement, parameterized by every column in table order
	 */
	record OnConflict(@NotNull SqlStatement statement) implements Upsert {
		/**
		 * Checks the statement.
		 *
		 * @param statement the statement
		 */
		public OnConflict {
			Objects.requireNonNull(statement, "statement");
		}
	}

	/**
	 * An insert, and the update of the row with the primary key to run when the insert inserted no
	 * row or failed with an error that {@link Dialect#isKeyConflict(java.sql.SQLException)}
	 * recognises. When the update then matches no row, the insert conflicted on another unique key,
	 * which is a {@link me.whereareiam.dialectica.exception.KeyConflictException}.
	 * <p>
	 * PostgreSQL and SQLite insert with {@code ON CONFLICT DO NOTHING}, which skips the row on a
	 * conflict on any unique key. MySQL, MariaDB and H2 insert plainly; their failed insert does not
	 * end a transaction around it.
	 *
	 * @param insert the insert, parameterized by every column in table order
	 * @param update the update, parameterized by the columns to update, then the primary key
	 */
	record InsertThenUpdate(@NotNull SqlStatement insert, @NotNull SqlStatement update) implements Upsert {
		/**
		 * Checks the statements.
		 *
		 * @param insert the insert
		 * @param update the update
		 */
		public InsertThenUpdate {
			Objects.requireNonNull(insert, "insert");
			Objects.requireNonNull(update, "update");
		}
	}
}
