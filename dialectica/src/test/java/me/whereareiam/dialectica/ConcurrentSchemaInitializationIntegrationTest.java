package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for schema initialization by several application instances that start at the
 * same moment against one database, each with a Jdbi instance and connections of its own.
 */
public class ConcurrentSchemaInitializationIntegrationTest extends BaseTest {
	private static final int INSTANCES = 8;
	private static final int ROUNDS = 5;
	private static final List<String> TABLES = List.of(
			"concurrent_test_members",
			"concurrent_test_sessions",
			"concurrent_test_accounts"
	);

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testConcurrentInitialization(String type) throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			dropTables(type);

			initializeConcurrently(type, false);

			assertTablesExist(type, TABLES);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testConcurrentLazyInitialization(String type) throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			dropTables(type);

			initializeConcurrently(type, true);

			// Only the requested entity and its dependency are created
			assertTablesExist(type, List.of("concurrent_test_sessions", "concurrent_test_accounts"));
		}
	}

	private void initializeConcurrently(String type, boolean lazy) throws Exception {
		CyclicBarrier start = new CyclicBarrier(INSTANCES);
		ExecutorService executor = Executors.newFixedThreadPool(INSTANCES);
		try {
			List<Future<?>> instances = new ArrayList<>();
			for (int instance = 0; instance < INSTANCES; instance++) {
				SchemaManager schemaManager = Dialectica.schema(createInstanceJdbi(type))
						.setLazyInitialization(lazy)
						.registerEntity(SessionEntity.class)
						.registerEntity(MemberEntity.class)
						.registerEntity(AccountEntity.class);

				instances.add(executor.submit(() -> {
					start.await(30, TimeUnit.SECONDS);
					if (lazy) schemaManager.ensureInitialized(SessionEntity.class);
					else schemaManager.initialize();

					return null;
				}));
			}

			for (Future<?> instance : instances)
				assertDoesNotThrow(() -> instance.get(2, TimeUnit.MINUTES));
		} finally {
			executor.shutdownNow();
		}
	}

	private Jdbi createInstanceJdbi(String type) {
		JdbcDatabaseContainer<?> container = DatabaseType.POSTGRES.equals(type) ? postgresContainer : mariaDbContainer;
		Jdbi jdbi = Jdbi.create(container.getJdbcUrl(), container.getUsername(), container.getPassword());
		jdbi.installPlugin(new DialectPlugin(type));

		return jdbi;
	}

	private void dropTables(String type) {
		getJdbi(type).useHandle(handle -> {
			for (String table : TABLES)
				handle.execute("DROP TABLE IF EXISTS " + table);
		});
	}

	private void assertTablesExist(String type, List<String> tables) {
		getJdbi(type).useHandle(handle -> {
			for (String table : tables)
				assertEquals(0, handle.createQuery("SELECT COUNT(*) FROM " + table).mapTo(Integer.class).one());
		});
	}

	@Entity(tableName = "concurrent_test_accounts")
	public static class AccountEntity implements EntitySchemaProvider {
		@Override
		public String statement(String databaseType) {
			return "CREATE TABLE IF NOT EXISTS concurrent_test_accounts (" +
					"id CHAR(36) PRIMARY KEY, " +
					"name VARCHAR(100) NOT NULL" +
					")";
		}
	}

	@Entity(tableName = "concurrent_test_sessions", dependsOn = {AccountEntity.class})
	public static class SessionEntity implements EntitySchemaProvider {
		@Override
		public String statement(String databaseType) {
			return "CREATE TABLE IF NOT EXISTS concurrent_test_sessions (" +
					"id CHAR(36) PRIMARY KEY, " +
					"account_id CHAR(36) NOT NULL REFERENCES concurrent_test_accounts(id)" +
					")";
		}
	}

	@Entity(tableName = "concurrent_test_members", dependsOn = {AccountEntity.class})
	public static class MemberEntity implements EntitySchemaProvider {
		@Override
		public String statement(String databaseType) {
			return "CREATE TABLE IF NOT EXISTS concurrent_test_members (" +
					"id CHAR(36) PRIMARY KEY, " +
					"account_id CHAR(36) NOT NULL REFERENCES concurrent_test_accounts(id)" +
					")";
		}
	}
}
