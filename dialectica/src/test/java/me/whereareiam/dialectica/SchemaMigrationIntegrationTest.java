package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Schema Migration Integration")
public class SchemaMigrationIntegrationTest extends BaseTest {
	@BeforeEach
	void cleanUp() {
		for (String type : new String[]{DatabaseType.POSTGRES, DatabaseType.MARIADB}) {
			Jdbi jdbi = getJdbi(type);
			jdbi.useHandle(handle -> {
				handle.execute("DROP TABLE IF EXISTS dialectica_schema_migrations");
				handle.execute("DROP TABLE IF EXISTS schema_test_manual_marker");
				handle.execute("DROP TABLE IF EXISTS schema_test_scanned_audit");
				handle.execute("DROP TABLE IF EXISTS schema_test_scope_one");
				handle.execute("DROP TABLE IF EXISTS schema_test_scope_two");
				handle.execute("DROP TABLE IF EXISTS schema_test_old_users");
				handle.execute("DROP TABLE IF EXISTS schema_test_users");
			});
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void noMigrationScopesDoNotCreateHistoryTable(String type) {
		Jdbi jdbi = getJdbi(type);

		Dialectica.schema(jdbi)
				.registerEntity(MigrationAwareUserEntity.class)
				.initialize();

		assertTrue(tableExists(jdbi, "schema_test_users"));
		assertFalse(tableExists(jdbi, "dialectica_schema_migrations"));
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void manualMigrationCreatesHistoryRowAndIsIdempotent(String type) {
		Jdbi jdbi = getJdbi(type);

		SchemaManager schemaManager = Dialectica.schema(jdbi)
				.registerMigrationScope("manual", scope -> scope.registerMigration(CreateManualMarkerMigration.class));

		schemaManager.initialize();
		schemaManager.initialize();

		assertTrue(tableExists(jdbi, "dialectica_schema_migrations"));
		assertTrue(tableExists(jdbi, "schema_test_manual_marker"));
		assertEquals(1, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'manual'"));
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void scannedAndManualScopesKeepIndependentVersions(String type) {
		Jdbi jdbi = getJdbi(type);

		Dialectica.schema(jdbi)
				.registerMigrationScope("scanned", scope -> scope.scanPackages("me.whereareiam.dialectica.fixture.migration"))
				.registerMigrationScope("manual", scope -> scope.registerMigration(CreateScopeOneMigration.class))
				.registerMigrationScope("manual-two", scope -> scope.registerMigration(CreateScopeTwoMigration.class))
				.initialize();

		assertTrue(tableExists(jdbi, "schema_test_scanned_audit"));
		assertTrue(tableExists(jdbi, "schema_test_scope_one"));
		assertTrue(tableExists(jdbi, "schema_test_scope_two"));
		assertEquals(1, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'scanned'"));
		assertEquals(1, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'manual'"));
		assertEquals(1, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'manual-two'"));
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void migrationsRunBeforeEntityInitialization(String type) {
		Jdbi jdbi = getJdbi(type);
		jdbi.useHandle(handle -> handle.execute("""
				CREATE TABLE schema_test_old_users (
					id CHAR(36) PRIMARY KEY,
					name VARCHAR(100) NOT NULL
				)
				"""));

		Dialectica.schema(jdbi)
				.registerEntity(MigrationAwareUserEntity.class)
				.registerMigrationScope("rename", scope -> scope.registerMigration(RenameUserTableMigration.class))
				.initialize();

		assertFalse(tableExists(jdbi, "schema_test_old_users"));
		assertTrue(tableExists(jdbi, "schema_test_users"));
		assertEquals(1, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'rename'"));
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void failedMigrationIsNotRecorded(String type) {
		Jdbi jdbi = getJdbi(type);

		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> Dialectica.schema(jdbi)
						.registerMigrationScope("broken", scope -> scope.registerMigration(FailingMigration.class))
						.initialize()
		);

		assertTrue(exception.getMessage().contains("Failed to apply migration broken:1"));
		assertTrue(tableExists(jdbi, "dialectica_schema_migrations"));
		assertEquals(0, countRows(jdbi, "SELECT COUNT(*) FROM dialectica_schema_migrations WHERE scope = 'broken'"));
	}

	private boolean tableExists(Jdbi jdbi, String tableName) {
		return jdbi.withHandle(handle -> {
			try {
				try (var resultSet = handle.getConnection().getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
					if (resultSet.next())
						return true;
				}
				try (var resultSet = handle.getConnection().getMetaData().getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
					return resultSet.next();
				}
			} catch (Exception exception) {
				throw new IllegalStateException("Failed to inspect table " + tableName, exception);
			}
		});
	}

	private long countRows(Jdbi jdbi, String sql) {
		return jdbi.withHandle(handle -> handle.createQuery(sql).mapTo(Long.class).one());
	}

	@Entity(tableName = "schema_test_users")
	public static class MigrationAwareUserEntity implements EntitySchemaProvider {
		@Override
		public String statement(String databaseType) {
			return """
					CREATE TABLE IF NOT EXISTS schema_test_users (
						id CHAR(36) PRIMARY KEY,
						name VARCHAR(100) NOT NULL
					)
					""";
		}
	}

	public static class CreateManualMarkerMigration implements SchemaMigration {
		@Override
		public int version() {
			return 1;
		}

		@Override
		public @NotNull String name() {
			return "create_manual_marker";
		}

		@Override
		public void migrate(@NotNull SchemaMigrationContext context) {
			context.execute("CREATE TABLE IF NOT EXISTS schema_test_manual_marker (id INT PRIMARY KEY)");
		}
	}

	public static class CreateScopeOneMigration implements SchemaMigration {
		@Override
		public int version() {
			return 1;
		}

		@Override
		public @NotNull String name() {
			return "create_scope_one";
		}

		@Override
		public void migrate(@NotNull SchemaMigrationContext context) {
			context.execute("CREATE TABLE IF NOT EXISTS schema_test_scope_one (id INT PRIMARY KEY)");
		}
	}

	public static class CreateScopeTwoMigration implements SchemaMigration {
		@Override
		public int version() {
			return 1;
		}

		@Override
		public @NotNull String name() {
			return "create_scope_two";
		}

		@Override
		public void migrate(@NotNull SchemaMigrationContext context) {
			context.execute("CREATE TABLE IF NOT EXISTS schema_test_scope_two (id INT PRIMARY KEY)");
		}
	}

	public static class RenameUserTableMigration implements SchemaMigration {
		@Override
		public int version() {
			return 1;
		}

		@Override
		public @NotNull String name() {
			return "rename_user_table";
		}

		@Override
		public void migrate(@NotNull SchemaMigrationContext context) {
			if (context.tableExists("schema_test_old_users") && !context.tableExists("schema_test_users"))
				context.renameTable("schema_test_old_users", "schema_test_users");
		}
	}

	public static class FailingMigration implements SchemaMigration {
		@Override
		public int version() {
			return 1;
		}

		@Override
		public @NotNull String name() {
			return "failing_migration";
		}

		@Override
		public void migrate(@NotNull SchemaMigrationContext context) {
			throw new IllegalStateException("boom");
		}
	}
}
