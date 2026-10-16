package me.whereareiam.dialectica.dialect.sqlite;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteKeyConflictTest {
	private final SqliteDialect dialect = new SqliteDialect();

	@Test
	void recognisesATakenUniqueKey() throws SQLException {
		assertTrue(dialect.isKeyConflict(refused("INSERT INTO accounts (id, name) VALUES (2, 'alice')")));
	}

	@Test
	void recognisesATakenPrimaryKey() throws SQLException {
		assertTrue(dialect.isKeyConflict(refused("INSERT INTO accounts (id, name) VALUES (1, 'bob')")));
	}

	@Test
	void recognisesAnUpdateThatTakesAUniqueKey() throws SQLException {
		assertTrue(dialect.isKeyConflict(refused("UPDATE accounts SET name = 'alice' WHERE id = 3")));
	}

	@Test
	void doesNotTakeAMissingValueForAKeyConflict() throws SQLException {
		assertFalse(dialect.isKeyConflict(refused("INSERT INTO accounts (id, name) VALUES (4, NULL)")));
	}

	/**
	 * Runs a statement on a table with a primary key and a unique, required name, and returns why
	 * SQLite refused it.
	 */
	private static SQLException refused(String sql) throws SQLException {
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY, name VARCHAR(16) NOT NULL UNIQUE)");
			statement.execute("INSERT INTO accounts (id, name) VALUES (1, 'alice'), (3, 'carol')");

			return assertThrows(SQLException.class, () -> statement.execute(sql));
		}
	}
}
