package me.whereareiam.dialectica.exception;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when an upsert cannot write a row because another row of the table already holds one of
 * the row's unique keys besides its primary key, such as a second link of the same account to the
 * same provider. The other row stays as it is. Every dialect throws it for such a conflict; where
 * the database refused the insert with an error, that error is the cause.
 */
@Getter
public class KeyConflictException extends RuntimeException {
	/**
	 * Name of the table the row was written to.
	 */
	private final @NotNull String table;

	/**
	 * Creates the exception.
	 *
	 * @param table name of the table the row was written to
	 * @param cause the database's error that refused the insert, or {@code null} when the database
	 *              skipped the insert without one
	 */
	public KeyConflictException(@NotNull String table, @Nullable Throwable cause) {
		super("Another row of " + table + " already holds a unique key of the written row", cause);
		this.table = table;
	}
}
