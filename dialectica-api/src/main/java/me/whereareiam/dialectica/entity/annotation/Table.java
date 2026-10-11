package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a record or class as an entity stored in the named table. Every non-static,
 * non-transient field is a column:
 * <pre>{@code
 * @Table("identica_account_reservations")
 * public record AccountReservation(
 *         @Id @Length(192) String reservationKey,
 *         UUID uniqueId,
 *         long createdAt,
 *         long expiresAt
 * ) {}
 * }</pre>
 * A column is named after its field in snake_case unless {@link Column} names it, and refuses
 * {@code NULL} unless its field carries a runtime-visible annotation named {@code Nullable}, such as
 * {@link Nullable}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
	/**
	 * Returns the name of the table.
	 *
	 * @return the table name
	 */
	String value();
}
