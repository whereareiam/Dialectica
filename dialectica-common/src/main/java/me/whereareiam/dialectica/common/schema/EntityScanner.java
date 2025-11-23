package me.whereareiam.dialectica.common.schema;

import me.whereareiam.dialectica.annotation.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for scanning packages to discover classes annotated with {@link Entity}.
 */
public final class EntityScanner {
	/**
	 * Scans the specified package for classes annotated with {@link Entity}.
	 *
	 * @param packageName the package name to scan (e.g., "me.whereareiam.intercept.entity")
	 * @return list of discovered entity classes
	 * @throws RuntimeException if scanning fails
	 */
	public static List<Class<?>> scanPackage(String packageName) {
		if (packageName == null || packageName.isEmpty())
			return Collections.emptyList();

		List<Class<?>> entities = new ArrayList<>();
		String packagePath = packageName.replace('.', '/');
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		try {
			Enumeration<URL> resources = classLoader.getResources(packagePath);
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				scanResource(packageName, resource, entities);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to scan package: " + packageName, e);
		}

		return entities;
	}

	private static void scanResource(String packageName, URL resource, List<Class<?>> entities) throws IOException {
		URI uri;
		try {
			uri = resource.toURI();
		} catch (java.net.URISyntaxException e) {
			throw new IOException("Invalid URI for resource: " + resource, e);
		}

		String packagePath = packageName.replace('.', '/');

		if (uri.getScheme().equals("jar")) {
			try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
				Path path = fileSystem.getPath(packagePath);
				if (Files.exists(path)) scanDirectory(packageName, path, entities);
			}
			return;
		}

		Path path = Paths.get(uri);
		if (Files.exists(path)) scanDirectory(packageName, path, entities);
	}

	private static void scanDirectory(String packageName, Path directory, List<Class<?>> entities) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<>() {
			@Override
			public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
				String fileName = file.getFileName().toString();
				if (fileName.endsWith(".class")) {
					String relativePath = directory.relativize(file).toString()
							.replace(File.separatorChar, '.')
							.replace('/', '.');
					String className = packageName + "." + relativePath.substring(0, relativePath.length() - 6);

					try {
						Class<?> clazz = Class.forName(className);
						if (clazz.isAnnotationPresent(Entity.class) && !clazz.isInterface())
							entities.add(clazz);
					} catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
						// Skip classes that can't be loaded
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
}

