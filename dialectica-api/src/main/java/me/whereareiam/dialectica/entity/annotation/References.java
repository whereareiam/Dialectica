package me.whereareiam.dialectica.entity.annotation;

import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes the column of a field a foreign key to the primary key of another entity, which has to
 * have a single {@link Id} field:
 * <pre>{@code
 * @Id @References(entity = Account.class, onDelete = ReferentialAction.CASCADE)
 * UUID uniqueId;
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface References {
	/**
	 * Returns the referenced entity, annotated with {@link Table}; it may be the declaring entity.
	 *
	 * @return the referenced entity type
	 */
	Class<?> entity();

	/**
	 * Returns what happens to this row when the referenced row is deleted.
	 *
	 * @return the action on delete
	 */
	ReferentialAction onDelete() default ReferentialAction.NO_ACTION;
}
