package me.whereareiam.dialectica.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a database entity that should be managed by Dialectica's schema system.
 * <p>
 * Entities marked with this annotation can be automatically discovered and initialized.
 * The entity class should provide a nested class implementing {@link me.whereareiam.dialectica.EntitySchemaProvider}
 * or implement it directly.
 * <p>
 * Example:
 * <pre>{@code
 * @DialectEntity(tableName = "players", version = 1)
 * public class PlayerEntity {
 *     // fields...
 *
 *     public static class SchemaProvider implements EntitySchemaProvider {
 *         // DDL statements...
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
	/**
	 * The name of the database table for this entity.
	 * If not specified, the table name will be derived from the class name.
	 *
	 * @return the table name
	 */
	String tableName() default "";

	/**
	 * The schema version of this entity.
	 * Descriptive metadata only; Strata streams own executable migration versions. Defaults to 1.
	 *
	 * @return the schema version
	 */
	int version() default 1;

	/**
	 * Other entity classes that this entity depends on.
	 * Tables will be created in dependency order.
	 *
	 * @return array of dependent entity classes
	 */
	Class<?>[] dependsOn() default {};
}

