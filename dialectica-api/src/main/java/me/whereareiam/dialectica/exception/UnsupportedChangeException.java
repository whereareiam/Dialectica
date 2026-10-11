package me.whereareiam.dialectica.exception;

import lombok.Getter;
import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.schema.change.SchemaChange;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a dialect cannot carry a schema change out in place. SQLite, for example, cannot add
 * a {@code NOT NULL} column without a default or drop a column that belongs to a key or an index;
 * both would need the table to be rebuilt, which Dialectica does not do.
 */
@Getter
public class UnsupportedChangeException extends RuntimeException {
	/**
	 * Dialect that cannot carry the change out.
	 */
	private final @NotNull Dialect dialect;

	/**
	 * Change that was refused.
	 */
	private final @NotNull SchemaChange change;

	/**
	 * Creates the exception.
	 *
	 * @param dialect dialect that cannot carry the change out
	 * @param change  change that was refused
	 * @param reason  why the dialect cannot carry it out
	 */
	public UnsupportedChangeException(@NotNull Dialect dialect, @NotNull SchemaChange change, @NotNull String reason) {
		super(dialect.name() + " cannot apply " + change + ": " + reason);
		this.dialect = dialect;
		this.change = change;
	}
}
