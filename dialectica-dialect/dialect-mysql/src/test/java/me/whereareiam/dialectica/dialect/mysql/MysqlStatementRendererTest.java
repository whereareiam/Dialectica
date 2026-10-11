package me.whereareiam.dialectica.dialect.mysql;

import me.whereareiam.dialectica.common.dialect.StatementCases;
import me.whereareiam.dialectica.dialect.SqlStatement;
import me.whereareiam.dialectica.dialect.StatementRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static me.whereareiam.dialectica.common.dialect.StatementCases.statement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MysqlStatementRendererTest {
	private final StatementRenderer renderer = new MysqlDialect().statementRenderer();

	static Stream<Arguments> cases() {
		return StatementCases.expect(
				statement(
						"INSERT INTO identica_account_reservations (reservation_key, unique_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
						"reservation_key", "unique_id", "created_at", "expires_at"
				),
				statement(
						"INSERT INTO identica_account_reservations (reservation_key, unique_id, created_at, expires_at) VALUES (?, ?, ?, ?) AS excluded ON DUPLICATE KEY UPDATE unique_id = excluded.unique_id, created_at = excluded.created_at, expires_at = excluded.expires_at",
						"reservation_key", "unique_id", "created_at", "expires_at"
				),
				statement(
						"UPDATE identica_account_reservations SET unique_id = ?, created_at = ?, expires_at = ? WHERE reservation_key = ?",
						"unique_id", "created_at", "expires_at", "reservation_key"
				),
				statement(
						"SELECT reservation_key, unique_id, created_at, expires_at FROM identica_account_reservations WHERE reservation_key = ?",
						"reservation_key"
				),
				statement(
						"DELETE FROM identica_account_reservations WHERE reservation_key = ?",
						"reservation_key"
				),
				statement(
						"INSERT INTO identica_provider_links (provider_id, provider_subject, unique_id, is_primary) VALUES (?, ?, ?, ?)",
						"provider_id", "provider_subject", "unique_id", "is_primary"
				),
				statement(
						"INSERT INTO identica_provider_links (provider_id, provider_subject, unique_id, is_primary) VALUES (?, ?, ?, ?) AS excluded ON DUPLICATE KEY UPDATE unique_id = excluded.unique_id, is_primary = excluded.is_primary",
						"provider_id", "provider_subject", "unique_id", "is_primary"
				),
				statement(
						"UPDATE identica_provider_links SET unique_id = ?, is_primary = ? WHERE provider_id = ? AND provider_subject = ?",
						"unique_id", "is_primary", "provider_id", "provider_subject"
				),
				statement(
						"SELECT provider_id, provider_subject, unique_id, is_primary FROM identica_provider_links WHERE provider_id = ? AND provider_subject = ?",
						"provider_id", "provider_subject"
				),
				statement(
						"DELETE FROM identica_provider_links WHERE provider_id = ? AND provider_subject = ?",
						"provider_id", "provider_subject"
				),
				statement(
						"INSERT INTO identica_flags (unique_id, `order`) VALUES (?, ?) AS excluded ON DUPLICATE KEY UPDATE unique_id = excluded.unique_id",
						"unique_id", "order"
				),
				statement(
						"SELECT unique_id, `order` FROM identica_flags WHERE unique_id = ? AND `order` = ?",
						"unique_id", "order"
				)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void renders(String name, Function<StatementRenderer, SqlStatement> render, SqlStatement expected) {
		assertEquals(expected, render.apply(renderer));
	}

	@Test
	void refusesToAddressARowWithoutAPrimaryKey() {
		assertThrows(IllegalArgumentException.class, () -> renderer.upsert(StatementCases.UNKEYED));
		assertThrows(IllegalArgumentException.class, () -> renderer.select(StatementCases.UNKEYED));
		assertThrows(IllegalArgumentException.class, () -> renderer.delete(StatementCases.UNKEYED));
	}

	@Test
	void refusesToUpdateATableWhoseColumnsAllBelongToTheKey() {
		assertThrows(IllegalArgumentException.class, () -> renderer.update(StatementCases.FLAGS));
	}
}
