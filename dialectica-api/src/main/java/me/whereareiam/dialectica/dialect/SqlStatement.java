package me.whereareiam.dialectica.dialect;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A rendered statement with positional parameters: its SQL, with a {@code ?} for each parameter,
 * and the columns whose values fill the parameters, in order.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class SqlStatement {
	/**
	 * SQL of the statement, without a trailing semicolon.
	 */
	private final @NotNull String sql;

	/**
	 * Names of the columns whose values fill the parameters, in the order of the {@code ?} marks.
	 * A column may appear more than once.
	 */
	private final @NotNull List<String> parameters;

	/**
	 * Creates a statement.
	 *
	 * @param sql        SQL with a {@code ?} for each parameter
	 * @param parameters columns that fill the parameters, in order
	 */
	public SqlStatement(@NotNull String sql, @NotNull List<String> parameters) {
		this.sql = Objects.requireNonNull(sql, "sql");
		this.parameters = List.copyOf(parameters);
	}
}
