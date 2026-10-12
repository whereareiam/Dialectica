package me.whereareiam.dialectica.common.dialect;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.dialect.Comparison;
import me.whereareiam.dialectica.dialect.Condition;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.schema.ColumnDefinition;
import me.whereareiam.dialectica.schema.TableDefinition;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Renders the entity statements every supported dialect shares. Upserts differ between dialects,
 * so each dialect's renderer supplies its own.
 */
public abstract class AnsiStatementRenderer implements StatementRenderer {
	private final @NotNull Dialect dialect;

	/**
	 * Creates the renderer of a dialect, which names tables and columns.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	protected AnsiStatementRenderer(@NotNull Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public @NotNull SqlStatement insert(@NotNull TableDefinition table) {
		return new SqlStatement("INSERT INTO " + identifier(table.getName()) + " " + columnList(table) + " VALUES " + markers(table), columns(table));
	}

	@Override
	public @NotNull SqlStatement replace(@NotNull TableDefinition table) {
		List<String> values = valueColumns(table);
		if (values.isEmpty())
			throw new IllegalArgumentException("Table " + table.getName() + " has no column outside its primary key to update");

		List<String> parameters = new ArrayList<>(values);
		parameters.addAll(table.getPrimaryKey());

		return new SqlStatement("UPDATE " + identifier(table.getName()) + " SET " + join(values, ", ", column -> identifier(column) + " = ?")
				+ " WHERE " + keyCondition(table), parameters);
	}

	@Override
	public @NotNull SqlStatement update(
			@NotNull TableDefinition table,
			@NotNull List<String> columns,
			@NotNull List<Condition> conditions
	) {
		requireKey(table);
		if (columns.isEmpty())
			throw new IllegalArgumentException("An update of " + table.getName() + " needs a column to set");
		requireColumns(table, columns);
		for (String column : columns)
			if (table.getPrimaryKey().contains(column))
				throw new IllegalArgumentException("Column " + column + " of " + table.getName() + " belongs to the primary key and cannot be set");
		requireConditions(table, conditions);

		List<String> parameters = new ArrayList<>(columns);
		parameters.addAll(table.getPrimaryKey());

		StringBuilder sql = new StringBuilder("UPDATE ").append(identifier(table.getName()))
				.append(" SET ").append(join(columns, ", ", column -> identifier(column) + " = ?"))
				.append(" WHERE ").append(keyCondition(table));
		for (Condition condition : conditions) {
			sql.append(" AND ").append(identifier(condition.getColumn())).append(' ').append(operator(condition.getComparison()));
			if (condition.getComparison().takesValue())
				parameters.add(condition.getColumn());
		}

		return new SqlStatement(sql.toString(), parameters);
	}

	@Override
	public @NotNull SqlStatement select(@NotNull TableDefinition table) {
		return new SqlStatement("SELECT " + join(columns(table), ", ", this::identifier) + " FROM " + identifier(table.getName())
				+ " WHERE " + keyCondition(table), table.getPrimaryKey());
	}

	@Override
	public @NotNull SqlStatement delete(@NotNull TableDefinition table) {
		return new SqlStatement("DELETE FROM " + identifier(table.getName()) + " WHERE " + keyCondition(table), table.getPrimaryKey());
	}

	/**
	 * Renders an upsert as {@code INSERT ... ON CONFLICT (key) DO UPDATE SET column = excluded.column},
	 * as PostgreSQL and SQLite write it. A table whose columns all belong to the key does nothing on
	 * conflict.
	 *
	 * @param table table to write to
	 * @return the statement, parameterized by every column in table order
	 */
	protected final @NotNull SqlStatement insertOnConflict(@NotNull TableDefinition table) {
		requireKey(table);
		List<String> values = valueColumns(table);
		String action = values.isEmpty()
				? "DO NOTHING"
				: "DO UPDATE SET " + join(values, ", ", column -> identifier(column) + " = excluded." + identifier(column));

		return new SqlStatement(insert(table).getSql() + " ON CONFLICT " + identifiers(table.getPrimaryKey()) + " " + action, columns(table));
	}

	/**
	 * Writes a name as the dialect does.
	 *
	 * @param name name as declared
	 * @return the name as written in SQL
	 */
	protected final @NotNull String identifier(@NotNull String name) {
		return dialect.identifier(name);
	}

	/**
	 * Writes names as a parenthesized, comma-separated list.
	 *
	 * @param names names as declared
	 * @return the list as written in SQL
	 */
	protected final @NotNull String identifiers(@NotNull List<String> names) {
		return "(" + join(names, ", ", this::identifier) + ")";
	}

	/**
	 * Returns the columns of a table in order.
	 *
	 * @param table table
	 * @return the column names
	 */
	protected static @NotNull List<String> columns(@NotNull TableDefinition table) {
		return table.getColumns().stream().map(ColumnDefinition::getName).toList();
	}

	/**
	 * Returns the columns outside the primary key, in order.
	 *
	 * @param table table
	 * @return the column names outside the key
	 */
	protected static @NotNull List<String> valueColumns(@NotNull TableDefinition table) {
		return columns(table).stream().filter(column -> !table.getPrimaryKey().contains(column)).toList();
	}

	/**
	 * Returns a parenthesized list of one {@code ?} per column.
	 *
	 * @param table table
	 * @return the parameter markers
	 */
	protected static @NotNull String markers(@NotNull TableDefinition table) {
		return "(" + join(columns(table), ", ", column -> "?") + ")";
	}

	/**
	 * Refuses a table without a primary key.
	 *
	 * @param table table
	 * @throws IllegalArgumentException when the table has no primary key
	 */
	protected static void requireKey(@NotNull TableDefinition table) {
		if (table.getPrimaryKey().isEmpty())
			throw new IllegalArgumentException("Table " + table.getName() + " has no primary key");
	}

	/**
	 * Joins names after writing each one.
	 *
	 * @param names     names to join
	 * @param separator separator between them
	 * @param writer    writes one name
	 * @return the joined text
	 */
	protected static @NotNull String join(@NotNull List<String> names, @NotNull String separator, @NotNull Function<String, String> writer) {
		StringJoiner joiner = new StringJoiner(separator);
		for (String name : names)
			joiner.add(writer.apply(name));

		return joiner.toString();
	}

	private static void requireColumns(TableDefinition table, List<String> names) {
		Set<String> seen = new HashSet<>();
		for (String name : names) {
			requireColumn(table, name);
			if (!seen.add(name))
				throw new IllegalArgumentException("Column " + name + " of " + table.getName() + " is set twice");
		}
	}

	private static void requireConditions(TableDefinition table, List<Condition> conditions) {
		Set<Condition> seen = new HashSet<>();
		for (Condition condition : conditions) {
			ColumnDefinition column = requireColumn(table, condition.getColumn());
			if (!seen.add(condition))
				throw new IllegalArgumentException("Condition " + condition + " on " + table.getName() + " is given twice");
			if (condition.getComparison().isOrdered() && !holdsNumbers(column.getType()))
				throw new IllegalArgumentException("Column " + column.getName() + " of " + table.getName() + " holds " + column.getType()
						+ ", which cannot be compared " + condition.getComparison() + "; ordered conditions compare number columns");
		}
	}

	private static ColumnDefinition requireColumn(TableDefinition table, String name) {
		ColumnDefinition column = table.column(name);
		if (column == null)
			throw new IllegalArgumentException("Table " + table.getName() + " has no column " + name);

		return column;
	}

	private static boolean holdsNumbers(ColumnType type) {
		return type instanceof ColumnType.BigintType || type instanceof ColumnType.IntegerType;
	}

	private static String operator(Comparison comparison) {
		return switch (comparison) {
			case EQUALS -> "= ?";
			case IS_NULL -> "IS NULL";
			case BELOW -> "< ?";
			case ABOVE -> "> ?";
			case AT_MOST -> "<= ?";
			case AT_LEAST -> ">= ?";
		};
	}

	private String columnList(TableDefinition table) {
		return identifiers(columns(table));
	}

	private String keyCondition(TableDefinition table) {
		requireKey(table);

		return join(table.getPrimaryKey(), " AND ", column -> identifier(column) + " = ?");
	}
}
