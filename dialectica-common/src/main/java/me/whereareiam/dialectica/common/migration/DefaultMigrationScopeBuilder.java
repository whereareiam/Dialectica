package me.whereareiam.dialectica.common.migration;

import lombok.Getter;
import me.whereareiam.dialectica.migration.MigrationScopeBuilder;
import me.whereareiam.dialectica.migration.SchemaMigration;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DefaultMigrationScopeBuilder implements MigrationScopeBuilder {
	private final Set<String> packageNames = new LinkedHashSet<>();
	private final Set<Class<? extends SchemaMigration>> migrationClasses = new LinkedHashSet<>();
	@Getter
    private ClassLoader classLoader;

	@Override
	public MigrationScopeBuilder scanPackages(@NotNull String... packageNames) {
		registerPackages(packageNames);
		return this;
	}

	@Override
	public MigrationScopeBuilder scanPackages(
			@NotNull ClassLoader classLoader,
			@NotNull String... packageNames
	) {
		this.classLoader = classLoader;
		registerPackages(packageNames);
		return this;
	}

	@Override
	public MigrationScopeBuilder registerMigration(@NotNull Class<? extends SchemaMigration> migrationClass) {
        migrationClasses.add(migrationClass);
		return this;
	}

	public @NotNull Set<String> getPackageNames() {
		return packageNames;
	}

	public @NotNull Set<Class<? extends SchemaMigration>> getMigrationClasses() {
		return migrationClasses;
	}

    private void registerPackages(String... packageNames) {
		if (packageNames == null) return;
		for (String packageName : packageNames) {
			if (packageName == null || packageName.isBlank())
				continue;

			this.packageNames.add(packageName);
		}
	}
}
