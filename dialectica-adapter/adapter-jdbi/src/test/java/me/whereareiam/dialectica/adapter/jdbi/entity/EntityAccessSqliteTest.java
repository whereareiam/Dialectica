package me.whereareiam.dialectica.adapter.jdbi.entity;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Runs the {@link EntityAccessContract} on a SQLite database file.
 */
class EntityAccessSqliteTest extends EntityAccessContract {
	@TempDir
	Path directory;

	@Override
	protected Jdbi createJdbi() {
		return Jdbi.create("jdbc:sqlite:" + directory.resolve("dialectica.db"));
	}
}
