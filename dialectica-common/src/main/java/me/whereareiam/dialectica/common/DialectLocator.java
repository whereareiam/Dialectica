package me.whereareiam.dialectica.common;

import me.whereareiam.dialectica.StatementProvider;
import me.whereareiam.dialectica.annotation.DialectQuery;
import me.whereareiam.dialectica.annotation.DialectUpdate;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.locator.AnnotationSqlLocator;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.jdbi.v3.core.internal.JdbiClassUtils.checkedCreateInstance;

/**
 * Custom SqlLocator that resolves SQL from {@link StatementProvider} for methods annotated with
 * {@link DialectQuery} or {@link DialectUpdate}, and delegates to the default
 * {@link AnnotationSqlLocator} for standard annotations.
 */
public class DialectLocator implements SqlLocator {
	private final SqlLocator defaultLocator;
	private final ConcurrentMap<Method, String> cache = new ConcurrentHashMap<>();

	public DialectLocator() {
		this.defaultLocator = new AnnotationSqlLocator();
	}

	@Override
	public String locate(Class<?> sqlObjectType, Method method, ConfigRegistry config) {
		DialectQuery queryAnnotation = method.getAnnotation(DialectQuery.class);
		DialectUpdate updateAnnotation = method.getAnnotation(DialectUpdate.class);

		if (queryAnnotation != null || updateAnnotation != null) {
			// Resolve from StatementProvider
			return cache.computeIfAbsent(method, m -> resolveFromProvider(
					queryAnnotation != null ? queryAnnotation.provider() : updateAnnotation.provider(),
					config));
		}

		// Delegate to default locator for standard @SqlQuery/@SqlUpdate
		return defaultLocator.locate(sqlObjectType, method, config);
	}

	private String resolveFromProvider(Class<? extends StatementProvider> providerClass, ConfigRegistry config) {
		String databaseType = DialectConfig.getDatabaseType(config);
		if (databaseType == null)
			throw new IllegalStateException(
					"Database type not configured. DialectPlugin must be installed with a database type.");

		StatementProvider provider = checkedCreateInstance(providerClass);
		return provider.getStatemenet(databaseType);
	}
}

