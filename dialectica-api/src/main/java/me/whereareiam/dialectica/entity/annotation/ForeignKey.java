package me.whereareiam.dialectica.entity.annotation;

import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes fields of the entity, named by field name, a foreign key to the whole primary key of
 * another entity. The fields reference the other entity's {@link Id} fields in their declaration
 * order, so a composite key is referenced as a whole:
 * <pre>{@code
 * @Table("identica_provider_profiles")
 * @ForeignKey(fields = {"providerId", "providerSubject"}, entity = ProviderLink.class,
 *         onDelete = ReferentialAction.CASCADE)
 * public record ProviderProfile(
 *         @Id @Length(64) String providerId,
 *         @Id @Length(128) String providerSubject,
 *         @Length(64) String providerUsername
 * ) {}
 * }</pre>
 * A key over a single field can also be declared with {@link References} on that field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ForeignKey.List.class)
public @interface ForeignKey {
	/**
	 * Returns the referencing fields, in the order of the referenced entity's {@link Id} fields.
	 *
	 * @return the field names
	 */
	String[] fields();

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

	/**
	 * Holds several {@link ForeignKey} annotations of one entity.
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface List {
		/**
		 * Returns the foreign keys.
		 *
		 * @return the foreign keys
		 */
		ForeignKey[] value();
	}
}
