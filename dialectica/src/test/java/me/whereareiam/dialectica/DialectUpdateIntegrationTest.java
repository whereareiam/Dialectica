package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.DialectUpdate;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link DialectUpdate} annotation.
 * Tests single and multiple statement execution for both PostgreSQL and MariaDB.
 */
public class DialectUpdateIntegrationTest extends BaseTest {
	@BeforeEach
	void setUp() {
		// Create test table for both databases
		getJdbi(DatabaseType.POSTGRES).useHandle(handle -> {
			handle.execute("DROP TABLE IF EXISTS test_table");
			handle.execute("CREATE TABLE test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
		});
		getJdbi(DatabaseType.MARIADB).useHandle(handle -> {
			handle.execute("DROP TABLE IF EXISTS test_table");
			handle.execute("CREATE TABLE test_table (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100))");
		});
	}

	@ParameterizedTest
	@EnumSource(DatabaseType.class)
	void testSingleStatementUpdate(DatabaseType type) {
		Jdbi jdbi = getJdbi(type);
		TestDao dao = jdbi.onDemand(TestDao.class);

		// Insert a record
		dao.insert("Test");

		// Verify it was inserted
		List<TestRecord> records = dao.findAll();
		assertEquals(1, records.size());
		assertEquals("Test", records.get(0).name);
	}

	@ParameterizedTest
	@EnumSource(DatabaseType.class)
	void testMultipleStatementsForMariaDB(DatabaseType type) {
		Jdbi jdbi = getJdbi(type);
		TestDao dao = jdbi.onDemand(TestDao.class);

		// Insert records
		dao.insert("First");
		dao.insert("Second");

		// Truncate (which uses multiple statements for MariaDB)
		dao.truncateAll();

		// Verify table is empty and AUTO_INCREMENT is reset
		List<TestRecord> records = dao.findAll();
		assertTrue(records.isEmpty());

		// Insert again - ID should start from 1
		dao.insert("New");
		records = dao.findAll();
		assertEquals(1, records.size());
		assertEquals(1L, records.get(0).id);
		assertEquals("New", records.get(0).name);
	}

	public interface TestDao extends SqlObject {
		@DialectUpdate(provider = InsertProvider.class)
		void insert(@Bind("name") String name);

		@DialectUpdate(provider = TruncateProvider.class)
		void truncateAll();

		@SqlQuery("SELECT id, name FROM test_table ORDER BY id")
		@RegisterRowMapper(TestRecord.Mapper.class)
		List<TestRecord> findAll();
	}

	public static class TestRecord {
		long id;
		String name;

		public static class Mapper implements RowMapper<TestRecord> {
			@Override
			public TestRecord map(ResultSet rs, StatementContext ctx) throws SQLException {
				TestRecord record = new TestRecord();
				record.id = rs.getLong("id");
				record.name = rs.getString("name");
				return record;
			}
		}
	}

	// Single statement provider
	public static class InsertProvider extends BaseStatementProvider {
		public InsertProvider() {
			super("INSERT INTO test_table (name) VALUES (:name)");
		}
	}

	// Multiple statement provider for MariaDB (DELETE + ALTER TABLE)
	public static class TruncateProvider extends BaseStatementProvider {
		public TruncateProvider() {
			super(
					"TRUNCATE TABLE test_table RESTART IDENTITY CASCADE",
					asList(
							"DELETE FROM test_table",
							"ALTER TABLE test_table AUTO_INCREMENT = 1"
					)
			);
		}
	}
}

