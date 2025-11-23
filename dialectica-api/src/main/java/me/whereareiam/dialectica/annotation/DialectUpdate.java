package me.whereareiam.dialectica.annotation;

import me.whereareiam.dialectica.StatementProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for database-specific SQL updates (INSERT, UPDATE, DELETE statements).
 * <p>
 * This annotation works similarly to {@link org.jdbi.v3.sqlobject.statement.SqlUpdate},
 * but allows specifying a {@link StatementProvider} class that provides database-specific SQL.
 * <p>
 * The SQL will be resolved at runtime based on the current database type.
 * <p>
 * Example:
 * <pre>{@code
 * @DialectUpdate(provider = MessageFileAdapter.TruncateAll.class)
 * void deleteAll();
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DialectUpdate {
	/**
	 * The {@link StatementProvider} class that provides the SQL update for each database type.
	 *
	 * @return the StatementProvider class
	 */
	Class<? extends StatementProvider> provider();
}

