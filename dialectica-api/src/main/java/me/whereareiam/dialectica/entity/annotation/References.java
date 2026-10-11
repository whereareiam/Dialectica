package me.whereareiam.dialectica.entity.annotation;

import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes the column of a field a foreign key, either to the primary key of another entity, which
 * has to have a single {@link Id} field, or to a column of a table named directly:
 * <pre>{@code
 * @Id @References(entity = Account.class, onDelete = ReferentialAction.CASCADE)
 * UUID uniqueId;
 *
 * @References(table = "identica_accounts", column = "unique_id", onDelete = ReferentialAction.CASCADE)
 * UUID owner;
 * }</pre>
 * Name the table when it belongs to another owner whose entity classes are out of reach, such as
 * the core tables of an application that a plug-in references. Exactly one of {@link #entity()} and
 * {@link #table()} is given; {@link #column()} goes with {@link #table()} and has to be its primary
 * key or a unique key. A key over several fields, such as one to a composite primary key, is
 * declared with {@link ForeignKey} on the entity.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface References {
	/**
	 * Returns the referenced entity, annotated with {@link Table}; it may be the declaring entity.
	 *
	 * @return the referenced entity type, or {@code void.class} when {@link #table()} is given
	 */
	Class<?> entity() default void.class;

	/**
	 * Returns the name of the referenced table, instead of an {@link #entity()}.
	 *
	 * @return the referenced table, or an empty string when {@link #entity()} is given
	 */
	String table() default "";

	/**
	 * Returns the referenced column of {@link #table()}.
	 *
	 * @return the referenced column, or an empty string when {@link #entity()} is given
	 */
	String column() default "";

	/**
	 * Returns what happens to this row when the referenced row is deleted.
	 *
	 * @return the action on delete
	 */
	ReferentialAction onDelete() default ReferentialAction.NO_ACTION;
}
