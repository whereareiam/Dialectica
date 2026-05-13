package me.whereareiam.dialectica.common.schema;

import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.migration.MigrationScopeBuilder;
import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.common.DialectConfig;
import me.whereareiam.dialectica.common.migration.DefaultMigrationScopeBuilder;
import me.whereareiam.dialectica.common.migration.DefaultSchemaMigrationContext;
import me.whereareiam.dialectica.common.migration.MigrationScanner;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Default implementation of {@link SchemaManager}.
 * Manages database schema initialization for entities marked with {@link Entity}.
 */
public final class DefaultSchemaManager implements SchemaManager {
	private static final String DEFAULT_MIGRATION_TABLE = "dialectica_schema_migrations";

	private final Set<Class<?>> registeredEntities = new LinkedHashSet<>();
	private final Set<String> scannedPackages = new HashSet<>();
	private final Map<String, DefaultMigrationScopeBuilder> migrationScopes = new LinkedHashMap<>();
	private final Jdbi jdbi;

	private boolean lazyInitialization = false;
	private boolean failOnError = true;
	private boolean initialized = false;
	private boolean migrationsApplied = false;
	private String migrationTable = DEFAULT_MIGRATION_TABLE;

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
	 * @param packageName the package name to scan (e.g., "me.whereareiam.intercept.entity")
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

	public SchemaManager registerMigrationScope(
			@NotNull String scope,
			@NotNull Consumer<MigrationScopeBuilder> builder
	) {
		if (scope.isBlank()) throw new IllegalArgumentException("Migration scope must not be blank");

        DefaultMigrationScopeBuilder scopeBuilder = migrationScopes.computeIfAbsent(scope, key -> new DefaultMigrationScopeBuilder());
		builder.accept(scopeBuilder);
		return this;
	}

	public SchemaManager setMigrationTable(@NotNull String tableName) {
		if (tableName.isBlank()) throw new IllegalArgumentException("Migration table name must not be blank");
		this.migrationTable = tableName;
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
	 * Tables are created in dependency order.
	 *
	 * @throws IllegalStateException if Jdbi or database type is not configured
	 */
	public void initialize() {
		if (initialized && !lazyInitialization) return;

		String databaseType = DialectConfig.getDatabaseType(jdbi.getConfig());
		if (databaseType == null)
			throw new IllegalStateException("Database type not configured. DialectPlugin must be installed with a database type.");

		List<Class<?>> orderedEntities = resolveDependencies();
		Set<String> createdTables = new HashSet<>();
		Map<String, List<ResolvedMigration>> resolvedMigrations = resolveMigrations();

		jdbi.useHandle(handle -> handle.useTransaction(transactionHandle -> {
			applyMigrationsIfNeeded(transactionHandle, databaseType, resolvedMigrations);

			for (Class<?> entityClass : orderedEntities) {
				try {
					EntitySchemaProvider provider = getSchemaProvider(entityClass);
					if (provider == null) {
						String message = "Entity " + entityClass.getName() + " does not provide EntitySchemaProvider";
						if (failOnError) throw new IllegalStateException(message);

						System.err.println("WARNING: " + message);
						continue;
					}

					String tableName = getTableName(entityClass, provider);
					if (createdTables.contains(tableName)) continue;

					String ddl = provider.statement(databaseType);
					transactionHandle.execute(ddl);
					createdTables.add(tableName);
				} catch (Exception e) {
					String message = "Failed to initialize entity " + entityClass.getName() + ": " + e.getMessage();
					if (failOnError) throw new RuntimeException(message, e);

					System.err.println("ERROR: " + message);
					throw (RuntimeException) e;
				}
			}
		}));

		initialized = true;
	}

	/**
	 * Ensures a specific entity's table is initialized (for lazy initialization).
	 *
	 * @param entityClass the entity class to ensure is initialized
	 */
	public void ensureInitialized(Class<?> entityClass) {
		if (!lazyInitialization || initialized) return;

		if (!registeredEntities.contains(entityClass)) registerEntity(entityClass);

		String databaseType = DialectConfig.getDatabaseType(jdbi.getConfig());
		if (databaseType == null)
			throw new IllegalStateException("Database type not configured. DialectPlugin must be installed with a database type.");

		Map<String, List<ResolvedMigration>> resolvedMigrations = resolveMigrations();

		// Initialize dependencies first
		Entity annotation = entityClass.getAnnotation(Entity.class);
		if (annotation != null)
			for (Class<?> dep : annotation.dependsOn())
				ensureInitialized(dep);

		// Initialize this entity
		jdbi.useHandle(handle -> {
			try {
				applyMigrationsIfNeeded(handle, databaseType, resolvedMigrations);

				EntitySchemaProvider provider = getSchemaProvider(entityClass);
				if (provider == null)
					throw new IllegalStateException("Entity " + entityClass.getName() + " does not provide EntitySchemaProvider");

				String ddl = provider.statement(databaseType);
				handle.execute(ddl);
			} catch (Exception e) {
				throw new RuntimeException("Failed to initialize entity " + entityClass.getName(), e);
			}
		});
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

	private void applyMigrationsIfNeeded(
			Handle handle,
			String databaseType,
			Map<String, List<ResolvedMigration>> resolvedMigrations
	) {
		if (migrationsApplied) return;
		if (resolvedMigrations.isEmpty()) {
			migrationsApplied = true;
			return;
		}

		ensureMigrationTable(handle);
		Set<String> appliedKeys = loadAppliedMigrationKeys(handle);
		for (Map.Entry<String, List<ResolvedMigration>> entry : resolvedMigrations.entrySet()) {
			String scope = entry.getKey();
			for (ResolvedMigration migration : entry.getValue()) {
				String key = migrationKey(scope, migration.version());
				if (appliedKeys.contains(key))
					continue;

				try {
					migration.migration().migrate(new DefaultSchemaMigrationContext(jdbi, handle, databaseType, scope));
					recordMigration(handle, scope, migration);
				} catch (Exception exception) {
					throw new IllegalStateException(
							"Failed to apply migration %s:%d (%s)".formatted(scope, migration.version(), migration.name()),
							exception
					);
				}
			}
		}

		migrationsApplied = true;
	}

	private @NotNull Map<String, List<ResolvedMigration>> resolveMigrations() {
		Map<String, List<ResolvedMigration>> resolved = new LinkedHashMap<>();

		for (Map.Entry<String, DefaultMigrationScopeBuilder> entry : migrationScopes.entrySet()) {
			String scope = entry.getKey();
			DefaultMigrationScopeBuilder builder = entry.getValue();
			LinkedHashSet<Class<? extends SchemaMigration>> migrationClasses = new LinkedHashSet<>(builder.getMigrationClasses());

			for (String packageName : builder.getPackageNames()) {
				List<Class<? extends SchemaMigration>> discovered = builder.getClassLoader() == null
						? MigrationScanner.scanPackage(packageName)
						: MigrationScanner.scanPackage(packageName, builder.getClassLoader());
				migrationClasses.addAll(discovered);
			}

			if (migrationClasses.isEmpty())
				continue;

			List<ResolvedMigration> migrations = new ArrayList<>();
			Set<Integer> seenVersions = new HashSet<>();
			for (Class<? extends SchemaMigration> migrationClass : migrationClasses) {
				SchemaMigration migration = instantiateMigration(migrationClass);
				if (!seenVersions.add(migration.version())) {
					throw new IllegalStateException(
							"Duplicate migration version %d registered for scope %s".formatted(migration.version(), scope)
					);
				}
				migrations.add(new ResolvedMigration(migration.version(), migration.name(), migration));
			}

			migrations.sort(Comparator.comparingInt(ResolvedMigration::version));
			resolved.put(scope, migrations);
		}

		return resolved;
	}

	private SchemaMigration instantiateMigration(Class<? extends SchemaMigration> migrationClass) {
		try {
			return migrationClass.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
			throw new IllegalStateException("Failed to instantiate migration " + migrationClass.getName(), exception);
		}
	}

	private void ensureMigrationTable(Handle handle) {
		handle.execute("""
				CREATE TABLE IF NOT EXISTS %s (
					scope VARCHAR(128) NOT NULL,
					version INT NOT NULL,
					name VARCHAR(255) NOT NULL,
					applied_at BIGINT NOT NULL,
					PRIMARY KEY (scope, version)
				)
				""".formatted(migrationTable));
	}

	private Set<String> loadAppliedMigrationKeys(Handle handle) {
		List<Map<String, Object>> rows = handle.createQuery("SELECT scope, version FROM " + migrationTable)
				.mapToMap()
				.list();

		Set<String> appliedKeys = new HashSet<>();
		for (Map<String, Object> row : rows) {
			Object scope = row.get("scope");
			Object version = row.get("version");
			if (scope == null || version == null)
				continue;
			appliedKeys.add(migrationKey(String.valueOf(scope), ((Number) version).intValue()));
		}
		return appliedKeys;
	}

	private void recordMigration(
			Handle handle,
			String scope,
			ResolvedMigration migration
	) {
		handle.createUpdate("INSERT INTO " + migrationTable + " (scope, version, name, applied_at) VALUES (:scope, :version, :name, :appliedAt)")
				.bind("scope", scope)
				.bind("version", migration.version())
				.bind("name", migration.name())
				.bind("appliedAt", System.currentTimeMillis())
				.execute();
	}

	private String migrationKey(String scope, int version) {
		return scope + ":" + version;
	}

	private record ResolvedMigration(
			int version,
			@NotNull String name,
			@NotNull SchemaMigration migration
	) {
	}
}
