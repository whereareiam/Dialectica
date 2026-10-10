package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.Entity;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SchemaInitializationTest {
	@Test void initializesRegisteredDependenciesWithoutCreatingMigrationHistory() {
		var source = new JdbcDataSource(); source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		var jdbi = Jdbi.create(source); jdbi.installPlugin(new DialectPlugin("h2"));
		var schema = Dialectica.schema(jdbi).registerEntity(Profile.class).registerEntity(Account.class);
		schema.initialize(); schema.initialize();
		jdbi.useHandle(handle -> {
			handle.execute("INSERT INTO accounts VALUES (1)");
			handle.execute("INSERT INTO profiles VALUES (1)");
			assertEquals(1, handle.createQuery("SELECT COUNT(*) FROM profiles").mapTo(Integer.class).one());
			assertEquals(0, handle.createQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'DIALECTICA_SCHEMA_MIGRATIONS'").mapTo(Integer.class).one());
		});
	}

	@Test
	void continuesPastFailingEntityWhenFailOnErrorIsDisabled() {
		Jdbi jdbi = createJdbi();

		Dialectica.schema(jdbi)
				.setFailOnError(false)
				.registerEntity(Account.class)
				.registerEntity(Broken.class)
				.registerEntity(Profile.class)
				.initialize();

		jdbi.useHandle(handle -> {
			handle.execute("INSERT INTO accounts VALUES (1)");
			handle.execute("INSERT INTO profiles VALUES (1)");
		});
	}

	@Test
	void failsOnFailingEntityByDefault() {
		Jdbi jdbi = createJdbi();
		SchemaManager schema = Dialectica.schema(jdbi).registerEntity(Account.class).registerEntity(Broken.class);

		RuntimeException failure = assertThrows(RuntimeException.class, schema::initialize);

		assertTrue(failure.getMessage().contains(Broken.class.getName()));
	}

	@Test
	void lazyInitializationCreatesDependenciesFirst() {
		Jdbi jdbi = createJdbi();
		SchemaManager schema = Dialectica.schema(jdbi).setLazyInitialization(true);

		schema.ensureInitialized(Profile.class);
		schema.ensureInitialized(Profile.class);

		jdbi.useHandle(handle -> {
			handle.execute("INSERT INTO accounts VALUES (1)");
			handle.execute("INSERT INTO profiles VALUES (1)");
		});
	}

	private static Jdbi createJdbi() {
		JdbcDataSource source = new JdbcDataSource();
		source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		Jdbi jdbi = Jdbi.create(source);
		jdbi.installPlugin(new DialectPlugin("h2"));

		return jdbi;
	}

	@Entity(tableName = "broken", dependsOn = Account.class)
	public static class Broken implements EntitySchemaProvider {
		@Override
		public String statement(String databaseType) {
			return "CREATE TABLE IF NOT EXISTS broken (id NO_SUCH_TYPE PRIMARY KEY)";
		}
	}

	@Entity(tableName = "accounts")
	public static class Account implements EntitySchemaProvider {
		@Override public String statement(String databaseType) { return "CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY)"; }
	}
	@Entity(tableName = "profiles", dependsOn = Account.class)
	public static class Profile implements EntitySchemaProvider {
		@Override public String statement(String databaseType) { return "CREATE TABLE IF NOT EXISTS profiles (id INT PRIMARY KEY REFERENCES accounts(id))"; }
	}
}
