package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.DialectQuery;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link DialectQuery} annotation.
 * Tests query execution for both PostgreSQL and MariaDB.
 */
public class DialectQueryIntegrationTest extends BaseTest {
	@BeforeEach
	void setUp() {
		// Create test table and insert data
		getJdbi(DatabaseType.POSTGRES).useHandle(handle -> {
			handle.execute("DROP TABLE IF EXISTS test_table");
			handle.execute("CREATE TABLE test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
			handle.execute("INSERT INTO test_table (name) VALUES ('Alice'), ('Bob'), ('Charlie')");
		});
		getJdbi(DatabaseType.MARIADB).useHandle(handle -> {
			handle.execute("DROP TABLE IF EXISTS test_table");
			handle.execute("CREATE TABLE test_table (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100))");
			handle.execute("INSERT INTO test_table (name) VALUES ('Alice'), ('Bob'), ('Charlie')");
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testFindById(String type) {
		Jdbi jdbi = getJdbi(type);
		TestDao dao = jdbi.onDemand(TestDao.class);

		Optional<TestRecord> record = dao.findById(1L);
		assertTrue(record.isPresent());
		assertEquals(1L, record.get().id);
		assertEquals("Alice", record.get().name);
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testFindAll(String type) {
		Jdbi jdbi = getJdbi(type);
		TestDao dao = jdbi.onDemand(TestDao.class);

		List<TestRecord> records = dao.findAll();
		assertEquals(3, records.size());
		assertEquals("Alice", records.get(0).name);
		assertEquals("Bob", records.get(1).name);
		assertEquals("Charlie", records.get(2).name);
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testFindByName(String type) {
		Jdbi jdbi = getJdbi(type);
		TestDao dao = jdbi.onDemand(TestDao.class);

		Optional<TestRecord> record = dao.findByName("Bob");
		assertTrue(record.isPresent());
		assertEquals("Bob", record.get().name);
	}

	public interface TestDao extends SqlObject {
		@DialectQuery(provider = FindByIdProvider.class)
		@RegisterRowMapper(TestRecord.Mapper.class)
		Optional<TestRecord> findById(@Bind("id") long id);

		@DialectQuery(provider = FindAllProvider.class)
		@RegisterRowMapper(TestRecord.Mapper.class)
		List<TestRecord> findAll();

		@DialectQuery(provider = FindByNameProvider.class)
		@RegisterRowMapper(TestRecord.Mapper.class)
		Optional<TestRecord> findByName(@Bind("name") String name);
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

	public static class FindByIdProvider extends BaseStatementProvider {
		public FindByIdProvider() {
			super("SELECT id, name FROM test_table WHERE id = :id");
		}
	}

	public static class FindAllProvider extends BaseStatementProvider {
		public FindAllProvider() {
			super("SELECT id, name FROM test_table ORDER BY id");
		}
	}

	public static class FindByNameProvider extends BaseStatementProvider {
		public FindByNameProvider() {
			super("SELECT id, name FROM test_table WHERE name = :name");
		}
	}
}

