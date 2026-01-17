package me.whereareiam.dialectica.common.handler;

import me.whereareiam.dialectica.StatementProvider;
import me.whereareiam.dialectica.annotation.DialectUpdate;
import me.whereareiam.dialectica.common.DialectConfig;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.AttachedExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.HandleSupplier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.jdbi.v3.core.internal.JdbiClassUtils.checkedCreateInstance;

/**
 * Implementation of the update handler with logic.
 * This is in dialectica-common because it contains business logic.
 */
public class DefaultUpdateHandler implements ExtensionHandler {
	private final ExtensionHandler delegateHandler;
	private final Class<? extends StatementProvider> providerClass;

	public DefaultUpdateHandler(Class<?> sqlObjectType, Method method) {
		DialectUpdate annotation = method.getAnnotation(DialectUpdate.class);
		if (annotation == null)
			throw new IllegalStateException("DefaultUpdateHandler can only be used with @DialectUpdate");

		this.providerClass = annotation.provider();
		this.delegateHandler = createDelegateHandler(sqlObjectType, method);
	}

	private ExtensionHandler createDelegateHandler(Class<?> sqlObjectType, Method method) {
		try {
			// Use reflection to access Jdbi's internal SqlUpdateHandler
			Class<?> sqlUpdateHandlerClass = Class.forName("org.jdbi.v3.sqlobject.statement.internal.SqlUpdateHandler");
			Constructor<?> constructor = sqlUpdateHandlerClass.getConstructor(Class.class, Method.class);
			return (ExtensionHandler) constructor.newInstance(sqlObjectType, method);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create delegate SqlUpdateHandler", e);
		}
	}

	@Override
	public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
		// Check if provider returns multiple statements
		String databaseType = DialectConfig.getDatabaseType(config);
		if (databaseType == null)
			throw new IllegalStateException(
					"Database type not configured. DialectPlugin must be installed with a database type.");

		StatementProvider provider = checkedCreateInstance(providerClass);
		List<String> statements = provider.getStatements(databaseType);

		// If multiple statements, create a custom handler
		if (statements.size() > 1) return new MultiStatementHandler(statements);

		// Otherwise, delegate to standard handler
		return delegateHandler.attachTo(config, target);
	}

	/**
	 * Handler that executes multiple SQL statements in sequence.
	 */
	private static class MultiStatementHandler implements AttachedExtensionHandler {
		private final List<String> statements;

		MultiStatementHandler(List<String> statements) {
			this.statements = statements;
		}

		@Override
		public Object invoke(HandleSupplier handleSupplier, Object... args) {
			return handleSupplier.getHandle().inTransaction(handle -> {
				int totalUpdates = 0;
				for (String sql : statements) {
					int updates = handle.execute(sql);
					totalUpdates += updates;
				}

				return totalUpdates;
			});
		}
	}
}

