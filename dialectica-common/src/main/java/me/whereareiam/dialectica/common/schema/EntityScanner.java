package me.whereareiam.dialectica.common.schema;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.whereareiam.dialectica.annotation.Entity;

import java.util.Collections;
import java.util.List;

/**
 * Utility class for scanning packages to discover classes annotated with {@link Entity}.
 * Uses ClassGraph library for reliable classpath scanning.
 */
public final class EntityScanner {
	/**
	 * Scans the specified package for classes annotated with {@link Entity}.
	 * Uses ClassGraph to handle classloader detection automatically.
	 *
	 * @param packageName the package name to scan (e.g., "me.whereareiam.intercept.entity")
	 * @return list of discovered entity classes
	 */
	public static List<Class<?>> scanPackage(String packageName) {
		if (packageName == null || packageName.isEmpty())
			return Collections.emptyList();

		try (ScanResult scanResult = new ClassGraph()
				.acceptPackages(packageName)
				.enableClassInfo()
				.enableAnnotationInfo()
				.scan()) {

			return scanResult.getClassesWithAnnotation(Entity.class.getName())
					.loadClasses()
					.stream()
					.filter(clazz -> !clazz.isInterface())
					.collect(java.util.stream.Collectors.toList());
		} catch (Exception e) {
			// Return empty list on error
			return Collections.emptyList();
		}
	}

	/**
	 * Scans the specified package using a specific classloader.
	 * Uses ClassGraph with the provided classloader.
	 *
	 * @param packageName the package name to scan
	 * @param classLoader the classloader to use
	 * @return list of discovered entity classes
	 */
	public static List<Class<?>> scanPackage(String packageName, ClassLoader classLoader) {
		if (packageName == null || packageName.isEmpty() || classLoader == null)
			return Collections.emptyList();

		try (ScanResult scanResult = new ClassGraph()
				.overrideClassLoaders(classLoader)
				.acceptPackages(packageName)
				.enableClassInfo()
				.enableAnnotationInfo()
				.scan()) {

			return scanResult.getClassesWithAnnotation(Entity.class.getName())
					.loadClasses()
					.stream()
					.filter(clazz -> !clazz.isInterface())
					.collect(java.util.stream.Collectors.toList());
		} catch (Exception e) {
			// Return empty list on error
			return Collections.emptyList();
		}
	}
}
