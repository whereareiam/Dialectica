package me.whereareiam.dialectica.common.dialect;

import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import me.whereareiam.dialectica.schema.Column;
import me.whereareiam.dialectica.schema.Table;
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
	public static final Table RESERVATIONS = Table.builder("identica_account_reservations")
			.column(Column.builder("reservation_key", ColumnType.varchar(192)).notNull().build())
			.column(Column.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(Column.builder("created_at", ColumnType.bigint()).notNull().build())
			.column(Column.builder("expires_at", ColumnType.bigint()).notNull().build())
			.primaryKey("reservation_key")
			.build();

	public static final Table LINKS = Table.builder("identica_provider_links")
			.column(Column.builder("provider_id", ColumnType.varchar(64)).notNull().build())
			.column(Column.builder("provider_subject", ColumnType.varchar(128)).notNull().build())
			.column(Column.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(Column.builder("is_primary", ColumnType.bool()).notNull().build())
			.primaryKey("provider_id", "provider_subject")
			.build();

	public static final Table FLAGS = Table.builder("identica_flags")
			.column(Column.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(Column.builder("order", ColumnType.varchar(16)).notNull().build())
			.primaryKey("unique_id", "order")
			.build();

	public static final Table UNKEYED = Table.builder("identica_events")
			.column(Column.builder("unique_id", ColumnType.uuid()).build())
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
	 * Creates an expected statement.
	 */
	public static SqlStatement statement(String sql, String... parameters) {
		return new SqlStatement(sql, List.of(parameters));
	}

	private static List<String> names() {
		return List.of(
				"insert reservation", "upsert reservation", "update reservation", "select reservation", "delete reservation",
				"insert link", "upsert link", "update link", "select link", "delete link",
				"upsert key-only table", "select quoted column"
		);
	}

	private static List<Function<StatementRenderer, SqlStatement>> renders() {
		return List.of(
				renderer -> renderer.insert(RESERVATIONS),
				renderer -> renderer.upsert(RESERVATIONS),
				renderer -> renderer.update(RESERVATIONS),
				renderer -> renderer.select(RESERVATIONS),
				renderer -> renderer.delete(RESERVATIONS),
				renderer -> renderer.insert(LINKS),
				renderer -> renderer.upsert(LINKS),
				renderer -> renderer.update(LINKS),
				renderer -> renderer.select(LINKS),
				renderer -> renderer.delete(LINKS),
				renderer -> renderer.upsert(FLAGS),
				renderer -> renderer.select(FLAGS)
		);
	}
}
