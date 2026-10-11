package me.whereareiam.dialectica.entity.annotation;

import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes fields of the entity, named by field name, a foreign key, either to the whole primary key
 * of another entity or to columns of a table named directly. The fields reference the other
 * entity's {@link Id} fields in their declaration order, so a composite key is referenced as a
 * whole:
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
 * Name the table when it belongs to another owner whose entity classes are out of reach; the
 * fields then reference {@link #columns()} in order, which have to form its primary key or a unique
 * key:
 * <pre>{@code
 * @ForeignKey(fields = {"providerId", "providerSubject"}, table = "identica_provider_links",
 *         columns = {"provider_id", "provider_subject"})
 * }</pre>
 * Exactly one of {@link #entity()} and {@link #table()} is given. A key over a single field can
 * also be declared with {@link References} on that field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ForeignKey.List.class)
public @interface ForeignKey {
	/**
	 * Returns the referencing fields, in the order of the referenced entity's {@link Id} fields or
	 * of {@link #columns()}.
	 *
	 * @return the field names
	 */
	String[] fields();

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
	 * Returns the referenced columns of {@link #table()}, one for each of {@link #fields()}.
	 *
	 * @return the referenced columns, empty when {@link #entity()} is given
	 */
	String[] columns() default {};

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
