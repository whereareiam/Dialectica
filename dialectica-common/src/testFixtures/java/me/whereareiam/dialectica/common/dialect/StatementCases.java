package me.whereareiam.dialectica.common.dialect;

import me.whereareiam.dialectica.dialect.Comparison;
import me.whereareiam.dialectica.dialect.Condition;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.schema.ColumnDefinition;
import me.whereareiam.dialectica.schema.TableDefinition;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The entity statements every dialect's statement renderer test renders, each paired by name with
 * the statement the dialect expects.
 */
public final class StatementCases {
	public static final TableDefinition RESERVATIONS = TableDefinition.builder("identica_account_reservations")
			.column(ColumnDefinition.builder("reservation_key", ColumnType.varchar(192)).notNull().build())
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("created_at", ColumnType.bigint()).notNull().build())
			.column(ColumnDefinition.builder("expires_at", ColumnType.bigint()).notNull().build())
			.primaryKey("reservation_key")
			.build();

	public static final TableDefinition LINKS = TableDefinition.builder("identica_provider_links")
			.column(ColumnDefinition.builder("provider_id", ColumnType.varchar(64)).notNull().build())
			.column(ColumnDefinition.builder("provider_subject", ColumnType.varchar(128)).notNull().build())
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("is_primary", ColumnType.bool()).notNull().build())
			.primaryKey("provider_id", "provider_subject")
			.build();

	public static final TableDefinition FLAGS = TableDefinition.builder("identica_flags")
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("order", ColumnType.varchar(16)).notNull().build())
			.primaryKey("unique_id", "order")
			.build();

	public static final TableDefinition SETTINGS = TableDefinition.builder("identica_settings")
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("order", ColumnType.varchar(16)).build())
			.primaryKey("unique_id")
			.build();

	public static final TableDefinition UNKEYED = TableDefinition.builder("identica_events")
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).build())
			.build();

	private StatementCases() {
	}

	/**
	 * Pairs every case with its expected statement, given in the order of {@link #names()}.
	 */
	public static Stream<Arguments> expect(SqlStatement... statements) {
		List<String> names = names();
		List<Function<StatementRenderer, SqlStatement>> renders = renders();
		if (statements.length != renders.size())
			throw new IllegalArgumentException("Expected " + renders.size() + " cases, not " + statements.length);

		return IntStream.range(0, renders.size())
				.mapToObj(index -> Arguments.of(names.get(index), renders.get(index), statements[index]));
	}

	/**
	 * Creates a condition of an update.
	 */
	public static Condition condition(String column, Comparison comparison) {
		return new Condition(column, comparison);
	}

	/**
	 * Creates an expected statement.
	 */
	public static SqlStatement statement(String sql, String... parameters) {
		return new SqlStatement(sql, List.of(parameters));
	}

	private static List<String> names() {
		return List.of(
				"insert reservation", "upsert reservation", "replace reservation", "select reservation", "delete reservation",
				"insert link", "upsert link", "replace link", "select link", "delete link",
				"upsert key-only table", "select quoted column",
				"update reservation conditionally", "update link where a column is null", "update quoted column conditionally",
				"update reservation within ordered conditions"
		);
	}

	private static List<Function<StatementRenderer, SqlStatement>> renders() {
		return List.of(
				renderer -> renderer.insert(RESERVATIONS),
				renderer -> renderer.upsert(RESERVATIONS),
				renderer -> renderer.replace(RESERVATIONS),
				renderer -> renderer.select(RESERVATIONS),
				renderer -> renderer.delete(RESERVATIONS),
				renderer -> renderer.insert(LINKS),
				renderer -> renderer.upsert(LINKS),
				renderer -> renderer.replace(LINKS),
				renderer -> renderer.select(LINKS),
				renderer -> renderer.delete(LINKS),
				renderer -> renderer.upsert(FLAGS),
				renderer -> renderer.select(FLAGS),
				renderer -> renderer.update(RESERVATIONS, List.of("unique_id", "expires_at"), List.of(condition("expires_at", Comparison.EQUALS))),
				renderer -> renderer.update(LINKS, List.of("is_primary"), List.of(condition("unique_id", Comparison.IS_NULL))),
				renderer -> renderer.update(SETTINGS, List.of("order"), List.of(condition("order", Comparison.EQUALS))),
				renderer -> renderer.update(RESERVATIONS, List.of("expires_at"), List.of(
						condition("expires_at", Comparison.BELOW),
						condition("expires_at", Comparison.ABOVE),
						condition("created_at", Comparison.AT_MOST),
						condition("created_at", Comparison.AT_LEAST)
				))
		);
	}
}
