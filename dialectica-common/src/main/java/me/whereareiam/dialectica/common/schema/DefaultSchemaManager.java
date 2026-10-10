package me.whereareiam.dialectica.common.schema;

import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.common.DialectConfig;
import me.whereareiam.dialectica.common.schema.lock.SchemaLock;
import me.whereareiam.dialectica.type.DatabaseType;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Default implementation of {@link SchemaManager}.
 * Manages database schema initialization for entities marked with {@link Entity}.
 */
public final class DefaultSchemaManager implements SchemaManager {

	private static final String STATEMENT_SAVEPOINT = "dialectica_schema_statement";

	private final Set<Class<?>> registeredEntities = new LinkedHashSet<>();
	private final Set<String> scannedPackages = new HashSet<>();
	private final Jdbi jdbi;

	private boolean lazyInitialization = false;
	private boolean failOnError = true;
	private boolean initialized = false;

	/**
	 * Creates a new DefaultSchemaManager for the given Jdbi instance.
	 *
	 * @param jdbi the Jdbi instance to use for schema operations
	 */
	public DefaultSchemaManager(Jdbi jdbi) {
		if (jdbi == null) throw new IllegalArgumentException("Jdbi instance must not be null");
		this.jdbi = jdbi;
	}

	/**
	 * Scans the specified package for classes annotated with {@link Entity}.
	 *
	 * @param packageNames the package names to scan (e.g., "me.whereareiam.intercept.entity")
	 * @return this SchemaManager instance for method chaining
	 */
	public SchemaManager scanPackages(String... packageNames) {
		for (String packageName : packageNames) {
			if (packageName == null || packageName.isEmpty()) continue;
			if (scannedPackages.contains(packageName)) continue;

			scannedPackages.add(packageName);
			List<Class<?>> discoveredEntities = EntityScanner.scanPackage(packageName);
			registeredEntities.addAll(discoveredEntities);
		}

		return this;
	}

	/**
	 * Scans the specified package using a specific classloader.
	 * Useful in plugin environments where the default classloader detection might not work.
	 *
	 * @param classLoader  the classloader to use for scanning
	 * @param packageNames the package names to scan
	 * @return this SchemaManager instance for method chaining
	 */
	public SchemaManager scanPackages(ClassLoader classLoader, String... packageNames) {
		if (classLoader == null) return scanPackages(packageNames);

		for (String packageName : packageNames) {
			if (packageName == null || packageName.isEmpty()) continue;
			if (scannedPackages.contains(packageName)) continue;

			scannedPackages.add(packageName);
			List<Class<?>> discoveredEntities = EntityScanner.scanPackage(packageName, classLoader);
			registeredEntities.addAll(discoveredEntities);
		}

		return this;
	}

	/**
	 * Registers an entity class manually.
	 *
	 * @param entityClass the entity class to register
	 * @return this SchemaManager instance for method chaining
	 */
	public SchemaManager registerEntity(Class<?> entityClass) {
		if (entityClass == null) throw new IllegalArgumentException("Entity class must not be null");
		if (!entityClass.isAnnotationPresent(Entity.class))
			throw new IllegalArgumentException("Class " + entityClass.getName() + " is not annotated with @Entity");

		registeredEntities.add(entityClass);
		return this;
	}



	/**
	 * Sets whether to use lazy initialization (create tables on first access).
	 *
	 * @param lazyInitialization true for lazy initialization, false for immediate
	 * @return this SchemaManager instance for method chaining
	 */
	public SchemaManager setLazyInitialization(boolean lazyInitialization) {
		this.lazyInitialization = lazyInitialization;
		return this;
	}

	/**
	 * Sets whether to fail on error or continue with other entities.
	 * When continuing, a failed entity is reported and skipped, and the tables of the other
	 * entities are still created and committed.
	 *
	 * @param failOnError true to fail on first error, false to continue
	 * @return this SchemaManager instance for method chaining
	 */
	public SchemaManager setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
		return this;
	}

	/**
	 * Initializes all registered entities by creating their tables.
	 * Tables are created in dependency order, in one transaction that holds the database's
	 * {@link SchemaLock}, so instances that start together against one database wait for each other.
	 *
	 * @throws IllegalStateException if Jdbi or database type is not configured
	 */
	public void initialize() {
		if (initialized && !lazyInitialization) return;

		createTables(resolveDependencies(), requireDatabaseType());
		initialized = true;
	}

	/**
	 * Ensures a specific entity's table is initialized (for lazy initialization).
	 * The entity's dependencies are created first, under the same lock and transaction as
	 * {@link #initialize()}.
	 *
	 * @param entityClass the entity class to ensure is initialized
	 */
	public void ensureInitialized(Class<?> entityClass) {
		if (!lazyInitialization || initialized) return;

		String databaseType = requireDatabaseType();
		Set<Class<?>> orderedEntities = new LinkedHashSet<>();
		collectWithDependencies(entityClass, orderedEntities, new HashSet<>());

		createTables(orderedEntities, databaseType);
	}

	private String requireDatabaseType() {
		String databaseType = DialectConfig.getDatabaseType(jdbi.getConfig());
		if (databaseType == null)
			throw new IllegalStateException("Database type not configured. DialectPlugin must be installed with a database type.");

		return databaseType;
	}

	private void collectWithDependencies(Class<?> entityClass, Set<Class<?>> ordered, Set<Class<?>> visiting) {
		if (ordered.contains(entityClass)) return;
		if (!visiting.add(entityClass))
			throw new IllegalStateException("Circular dependency detected in entity definitions");
		if (!registeredEntities.contains(entityClass)) registerEntity(entityClass);

		for (Class<?> dependency : entityClass.getAnnotation(Entity.class).dependsOn())
			collectWithDependencies(dependency, ordered, visiting);

		ordered.add(entityClass);
	}

	private void createTables(Collection<Class<?>> orderedEntities, String databaseType) {
		// PostgreSQL aborts the whole transaction when one statement fails, so a statement that may
		// fail without ending the initialization runs under a savepoint of its own.
		boolean isolateStatements = !failOnError && DatabaseType.POSTGRES.equalsIgnoreCase(databaseType);
		Set<String> createdTables = new HashSet<>();

		jdbi.useTransaction(handle -> {
			try (SchemaLock lock = SchemaLock.acquire(handle, databaseType)) {
				for (Class<?> entityClass : orderedEntities)
					createTable(handle, entityClass, databaseType, createdTables, isolateStatements);
			}
		});
	}

	private void createTable(
			Handle handle,
			Class<?> entityClass,
			String databaseType,
			Set<String> createdTables,
			boolean isolateStatement
	) {
		try {
			EntitySchemaProvider provider = getSchemaProvider(entityClass);
			if (provider == null)
				throw new IllegalStateException("Entity " + entityClass.getName() + " does not provide EntitySchemaProvider");

			String tableName = getTableName(entityClass, provider);
			if (createdTables.contains(tableName)) return;

			String ddl = provider.statement(databaseType);
			if (isolateStatement) executeIsolated(handle, ddl);
			else handle.execute(ddl);

			createdTables.add(tableName);
		} catch (Exception e) {
			String message = "Failed to initialize entity " + entityClass.getName() + ": " + e.getMessage();
			if (failOnError) throw new RuntimeException(message, e);

			System.err.println("ERROR: " + message);
		}
	}

	private void executeIsolated(Handle handle, String ddl) {
		handle.savepoint(STATEMENT_SAVEPOINT);
		try {
			handle.execute(ddl);
		} catch (Exception e) {
			handle.rollbackToSavepoint(STATEMENT_SAVEPOINT);
			throw e;
		}

		handle.releaseSavepoint(STATEMENT_SAVEPOINT);
	}

	private List<Class<?>> resolveDependencies() {
		Map<Class<?>, Set<Class<?>>> dependencyGraph = new HashMap<>();
		Map<Class<?>, Integer> inDegree = new HashMap<>();

		// Build dependency graph
		for (Class<?> entity : registeredEntities) {
			dependencyGraph.putIfAbsent(entity, new HashSet<>());
			inDegree.putIfAbsent(entity, 0);

			Entity annotation = entity.getAnnotation(Entity.class);
			if (annotation != null) {
				for (Class<?> dep : annotation.dependsOn()) {
					if (registeredEntities.contains(dep)) {
						dependencyGraph.get(entity).add(dep);
						inDegree.put(entity, inDegree.get(entity) + 1);
					}
				}
			}
		}

		// Topological sort
		List<Class<?>> result = new ArrayList<>();
		Queue<Class<?>> queue = new ArrayDeque<>();

		for (Class<?> entity : registeredEntities)
			if (inDegree.get(entity) == 0)
				queue.offer(entity);

		while (!queue.isEmpty()) {
			Class<?> current = queue.poll();
			result.add(current);

			for (Class<?> entity : registeredEntities) {
				if (dependencyGraph.get(entity).contains(current)) {
					int newDegree = inDegree.get(entity) - 1;
					inDegree.put(entity, newDegree);

					if (newDegree == 0) queue.offer(entity);
				}
			}
		}

		// Check for circular dependencies
		if (result.size() != registeredEntities.size()) {
			throw new IllegalStateException("Circular dependency detected in entity definitions");
		}

		return result;
	}

	private EntitySchemaProvider getSchemaProvider(Class<?> entityClass) {
		// Check if entity implements EntitySchemaProvider directly
		if (EntitySchemaProvider.class.isAssignableFrom(entityClass)) {
			try {
				return (EntitySchemaProvider) entityClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				// Try nested class
			}
		}

		// Look for nested SchemaProvider class
		Class<?>[] nestedClasses = entityClass.getDeclaredClasses();
		for (Class<?> nestedClass : nestedClasses) {
			if (EntitySchemaProvider.class.isAssignableFrom(nestedClass)) {
				try {
					return (EntitySchemaProvider) nestedClass.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new RuntimeException("Failed to instantiate SchemaProvider for " + entityClass.getName(), e);
				}
			}
		}

		return null;
	}

	private String getTableName(Class<?> entityClass, EntitySchemaProvider provider) {
		Entity annotation = entityClass.getAnnotation(Entity.class);
		if (annotation != null && !annotation.tableName().isEmpty())
			return annotation.tableName();

		if (provider != null && !provider.getTableName().isEmpty())
			return provider.getTableName();

		// Derive from class name
		String className = entityClass.getSimpleName();
		if (className.endsWith("Entity"))
			className = className.substring(0, className.length() - 6);

		return toSnakeCase(className);
	}

	private String toSnakeCase(String camelCase) {
		return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}








}
