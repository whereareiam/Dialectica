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
	@Entity(tableName = "accounts")
	public static class Account implements EntitySchemaProvider {
		@Override public String statement(String databaseType) { return "CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY)"; }
	}
	@Entity(tableName = "profiles", dependsOn = Account.class)
	public static class Profile implements EntitySchemaProvider {
		@Override public String statement(String databaseType) { return "CREATE TABLE IF NOT EXISTS profiles (id INT PRIMARY KEY REFERENCES accounts(id))"; }
	}
}
