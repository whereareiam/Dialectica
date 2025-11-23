package me.whereareiam.dialectica.common.handler;

import me.whereareiam.dialectica.annotation.DialectQuery;
import me.whereareiam.dialectica.annotation.DialectUpdate;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Factory that provides the actual handler implementations for Dialectica annotations.
 * <p>
 * This factory intercepts requests for marker handler classes (UpdateHandler, QueryHandler)
 * from dialectica-api and provides the real implementations from dialectica-common.
 */
public class DialectHandlerFactory implements ExtensionHandlerFactory {
	@Override
	public boolean accepts(Class<?> extensionType, Method method) {
		return method.isAnnotationPresent(DialectUpdate.class)
				|| method.isAnnotationPresent(DialectQuery.class);
	}

	@Override
	public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
		if (method.isAnnotationPresent(DialectUpdate.class))
			return Optional.of(new DefaultUpdateHandler(extensionType, method));

		if (method.isAnnotationPresent(DialectQuery.class))
			return Optional.of(new DefaultQueryHandler(extensionType, method));

		return Optional.empty();
	}
}

