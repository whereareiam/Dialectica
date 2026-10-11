package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the column of a field. Without it, the column is the field's name in snake_case:
 * {@code lastSeenAt} is stored in {@code last_seen_at}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
	/**
	 * Returns the name of the column.
	 *
	 * @return the column name
	 */
	String value();
}
