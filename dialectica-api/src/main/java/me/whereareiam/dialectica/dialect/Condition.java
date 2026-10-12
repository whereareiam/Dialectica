package me.whereareiam.dialectica.dialect;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A condition on a column's current value that an update of a row by its key has to meet, such as
 * {@code expires_at = ?} or {@code accepted_counter < ?}.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Condition {
	/**
	 * Name of the column to compare.
	 */
	private final @NotNull String column;

	/**
	 * How the column is compared.
	 */
	private final @NotNull Comparison comparison;

	/**
	 * Creates a condition.
	 *
	 * @param column     name of the column to compare
	 * @param comparison how the column is compared
	 */
	public Condition(@NotNull String column, @NotNull Comparison comparison) {
		this.column = Objects.requireNonNull(column, "column");
		this.comparison = Objects.requireNonNull(comparison, "comparison");
	}
}
