package me.whereareiam.dialectica.common.migration;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.whereareiam.dialectica.migration.SchemaMigration;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MigrationScanner {
	public static List<Class<? extends SchemaMigration>> scanPackage(String packageName) {
		if (packageName == null || packageName.isBlank())
			return Collections.emptyList();

		try (ScanResult scanResult = new ClassGraph()
				.acceptPackages(packageName)
				.enableClassInfo()
				.scan()) {

			return scanResult.getClassesImplementing(SchemaMigration.class.getName())
					.loadClasses(SchemaMigration.class)
					.stream()
					.filter(MigrationScanner::isConcreteMigration)
					.collect(Collectors.toList());
		} catch (Exception exception) {
			return Collections.emptyList();
		}
	}

	public static List<Class<? extends SchemaMigration>> scanPackage(
			String packageName,
			ClassLoader classLoader
	) {
		if (packageName == null || packageName.isBlank() || classLoader == null)
			return Collections.emptyList();

		try (ScanResult scanResult = new ClassGraph()
				.overrideClassLoaders(classLoader)
				.acceptPackages(packageName)
				.enableClassInfo()
				.scan()) {

			return scanResult.getClassesImplementing(SchemaMigration.class.getName())
					.loadClasses(SchemaMigration.class)
					.stream()
					.filter(MigrationScanner::isConcreteMigration)
					.collect(Collectors.toList());
		} catch (Exception exception) {
			return Collections.emptyList();
		}
	}

	private static boolean isConcreteMigration(Class<? extends SchemaMigration> migrationClass) {
		int modifiers = migrationClass.getModifiers();
		return !migrationClass.isInterface() && !Modifier.isAbstract(modifiers);
	}
}
