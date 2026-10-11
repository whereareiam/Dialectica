package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a unique constraint over fields of the entity, named by field name:
 * <pre>{@code
 * @Unique({"uniqueId", "providerId"})
 * }</pre>
 * Like every unique constraint it accepts any number of rows with {@code NULL} in its columns.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Unique.List.class)
public @interface Unique {
	/**
	 * Returns the fields whose combined values no two rows may share.
	 *
	 * @return the field names
	 */
	String[] value();

	/**
	 * Holds several {@link Unique} annotations of one entity.
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface List {
		/**
		 * Returns the unique constraints.
		 *
		 * @return the unique constraints
		 */
		Unique[] value();
	}
}
