package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds an index over fields of the entity, named by field name:
 * <pre>{@code
 * @Index(name = "identica_accounts_username", columns = "username")
 * }</pre>
 * Index names share one namespace per schema on PostgreSQL, H2 and SQLite, so prefix them with the
 * table's name.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Index.List.class)
public @interface Index {
	/**
	 * Returns the name of the index.
	 *
	 * @return the index name
	 */
	String name();

	/**
	 * Returns the indexed fields, in order.
	 *
	 * @return the field names
	 */
	String[] columns();

	/**
	 * Returns whether two rows may not share the values of the indexed fields.
	 *
	 * @return whether the index is unique
	 */
	boolean unique() default false;

	/**
	 * Holds several {@link Index} annotations of one entity.
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface List {
		/**
		 * Returns the indexes.
		 *
		 * @return the indexes
		 */
		Index[] value();
	}
}
