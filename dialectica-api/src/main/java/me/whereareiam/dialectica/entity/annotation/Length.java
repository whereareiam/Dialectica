package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits a {@link String} field to a number of characters, stored as {@code varchar(n)}. A string
 * field without it is stored as unlimited {@code text}, which MySQL, MariaDB and H2 cannot use in a
 * key or an index.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Length {
	/**
	 * Returns the maximum number of characters.
	 *
	 * @return the length, at least 1
	 */
	int value();
}
