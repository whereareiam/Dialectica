package me.whereareiam.dialectica;

import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SchemaManager}.
 * Tests automatic schema initialization with entity discovery, dependency ordering, and both database types.
 */
public class SchemaManagerIntegrationTest extends BaseTest {

	@BeforeEach
	void setUp() {
		// Clean up any existing tables
		for (String type : List.of(DatabaseType.POSTGRES, DatabaseType.MARIADB)) {
			Jdbi jdbi = getJdbi(type);
			jdbi.useHandle(handle -> {
				handle.execute("DROP TABLE IF EXISTS schema_test_users");
				handle.execute("DROP TABLE IF EXISTS schema_test_posts");
				handle.execute("DROP TABLE IF EXISTS schema_test_comments");
			});
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testManualEntityRegistration(String type) {
		Jdbi jdbi = getJdbi(type);

		SchemaManager schemaManager = Dialectica.schema(jdbi)
				.registerEntity(UserEntity.class)
				.registerEntity(PostEntity.class)
				.registerEntity(CommentEntity.class);

		// Initialize schema
		assertDoesNotThrow(schemaManager::initialize);

		// Verify tables exist and can be used
		TestDao dao = jdbi.onDemand(TestDao.class);

		UUID userId = UUID.randomUUID();
		dao.insertUser(userId.toString(), "Alice");

		Optional<UserEntity> user = dao.findUserById(userId.toString());
		assertTrue(user.isPresent());
		assertEquals("Alice", user.get().name);
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testDependencyOrdering(String type) {
		Jdbi jdbi = getJdbi(type);

		// Register entities in wrong order (comments before posts, posts before users)
		SchemaManager schemaManager = Dialectica.schema(jdbi)
				.registerEntity(CommentEntity.class)  // Depends on PostEntity
				.registerEntity(PostEntity.class)      // Depends on UserEntity
				.registerEntity(UserEntity.class);     // No dependencies

		// Should still work due to dependency resolution
		assertDoesNotThrow(schemaManager::initialize);

		// Verify all tables exist
		TestDao dao = jdbi.onDemand(TestDao.class);
		assertDoesNotThrow(dao::findAllUsers);
		assertDoesNotThrow(dao::findAllPosts);
		assertDoesNotThrow(dao::findAllComments);
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testTableNameFromAnnotation(String type) {
		Jdbi jdbi = getJdbi(type);

		SchemaManager schemaManager = Dialectica.schema(jdbi)
				.registerEntity(UserEntity.class);

		schemaManager.initialize();

		// Verify table name from annotation is used
		TestDao dao = jdbi.onDemand(TestDao.class);
		assertDoesNotThrow(dao::findAllUsers);
	}

	@ParameterizedTest
	@ValueSource(strings = {DatabaseType.POSTGRES, DatabaseType.MARIADB})
	void testIdempotentInitialization(String type) {
		Jdbi jdbi = getJdbi(type);

		SchemaManager schemaManager = Dialectica.schema(jdbi)
				.registerEntity(UserEntity.class);

		// Initialize multiple times - should not fail
		schemaManager.initialize();
		schemaManager.initialize();
		schemaManager.initialize();

		// Verify table still works
		TestDao dao = jdbi.onDemand(TestDao.class);
		assertDoesNotThrow(dao::findAllUsers);
	}

	// Test Entities

	@Entity(tableName = "schema_test_users", version = 1)
	public static class UserEntity implements EntitySchemaProvider {
		public UUID id;
		public String name;

		@Override
		public String statement(String databaseType) {
			if (DatabaseType.POSTGRES.equals(databaseType)) {
				return "CREATE TABLE IF NOT EXISTS schema_test_users (" +
						"id CHAR(36) PRIMARY KEY, " +
						"name VARCHAR(100) NOT NULL" +
						")";
			}
			return "CREATE TABLE IF NOT EXISTS schema_test_users (" +
					"id CHAR(36) PRIMARY KEY, " +
					"name VARCHAR(100) NOT NULL" +
					")";
		}
	}

	@Entity(tableName = "schema_test_posts", version = 1, dependsOn = {UserEntity.class})
	public static class PostEntity implements EntitySchemaProvider {
		public Long id;
		public UUID userId;
		public String title;

		@Override
		public String statement(String databaseType) {
			if (DatabaseType.POSTGRES.equals(databaseType)) {
				return "CREATE TABLE IF NOT EXISTS schema_test_posts (" +
						"id SERIAL PRIMARY KEY, " +
						"user_id CHAR(36) NOT NULL, " +
						"title VARCHAR(200) NOT NULL" +
						")";
			}
			return "CREATE TABLE IF NOT EXISTS schema_test_posts (" +
					"id INT AUTO_INCREMENT PRIMARY KEY, " +
					"user_id CHAR(36) NOT NULL, " +
					"title VARCHAR(200) NOT NULL" +
					")";
		}
	}

	@Entity(tableName = "schema_test_comments", version = 1, dependsOn = {PostEntity.class})
	public static class CommentEntity implements EntitySchemaProvider {
		public Long id;
		public Long postId;
		public String content;

		@Override
		public String statement(String databaseType) {
			if (DatabaseType.POSTGRES.equals(databaseType)) {
				return "CREATE TABLE IF NOT EXISTS schema_test_comments (" +
						"id SERIAL PRIMARY KEY, " +
						"post_id BIGINT NOT NULL, " +
						"content TEXT NOT NULL" +
						")";
			}
			return "CREATE TABLE IF NOT EXISTS schema_test_comments (" +
					"id INT AUTO_INCREMENT PRIMARY KEY, " +
					"post_id BIGINT NOT NULL, " +
					"content TEXT NOT NULL" +
					")";
		}
	}

	// Test DAO
	public interface TestDao extends SqlObject {
		@SqlUpdate("INSERT INTO schema_test_users (id, name) VALUES (:id, :name)")
		void insertUser(@Bind("id") String id, @Bind("name") String name);

		@SqlQuery("SELECT id, name FROM schema_test_users WHERE id = :id")
		@RegisterRowMapper(UserMapper.class)
		Optional<UserEntity> findUserById(@Bind("id") String id);

		@SqlQuery("SELECT id, name FROM schema_test_users")
		@RegisterRowMapper(UserMapper.class)
		List<UserEntity> findAllUsers();

		@SqlQuery("SELECT id, user_id, title FROM schema_test_posts")
		@RegisterRowMapper(PostMapper.class)
		List<PostEntity> findAllPosts();

		@SqlQuery("SELECT id, post_id, content FROM schema_test_comments")
		@RegisterRowMapper(CommentMapper.class)
		List<CommentEntity> findAllComments();
	}

	public static class UserMapper implements RowMapper<UserEntity> {
		@Override
		public UserEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
			UserEntity user = new UserEntity();
			user.id = UUID.fromString(rs.getString("id"));
			user.name = rs.getString("name");
			return user;
		}
	}

	public static class PostMapper implements RowMapper<PostEntity> {
		@Override
		public PostEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
			PostEntity post = new PostEntity();
			post.id = rs.getLong("id");
			post.userId = UUID.fromString(rs.getString("user_id"));
			post.title = rs.getString("title");
			return post;
		}
	}

	public static class CommentMapper implements RowMapper<CommentEntity> {
		@Override
		public CommentEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
			CommentEntity comment = new CommentEntity();
			comment.id = rs.getLong("id");
			comment.postId = rs.getLong("post_id");
			comment.content = rs.getString("content");
			return comment;
		}
	}
}
