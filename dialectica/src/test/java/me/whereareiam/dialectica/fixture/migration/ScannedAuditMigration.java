package me.whereareiam.dialectica.fixture.migration;

import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import org.jetbrains.annotations.NotNull;

public class ScannedAuditMigration implements SchemaMigration {
	@Override
	public int version() {
		return 1;
	}

	@Override
	public @NotNull String name() {
		return "scanned_audit_table";
	}

	@Override
	public void migrate(@NotNull SchemaMigrationContext context) {
		context.execute("""
				CREATE TABLE IF NOT EXISTS schema_test_scanned_audit (
					id INT PRIMARY KEY
				)
				""");
	}
}
