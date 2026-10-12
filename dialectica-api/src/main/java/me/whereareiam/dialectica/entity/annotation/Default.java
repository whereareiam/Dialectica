package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gives the column of a field the default it takes when an insert leaves it out. The value is
 * written as text and read by the field's type:
 * <table>
 *     <caption>Defaults by field type</caption>
 *     <tr><th>Field type</th><th>Value</th><th>Example</th></tr>
 *     <tr><td>{@code boolean}, {@code Boolean}</td><td>{@code true} or {@code false}</td><td>{@code @Default("false")}</td></tr>
 *     <tr><td>{@code long}, {@code Long}, {@code int}, {@code Integer}</td><td>a decimal number in the type's range</td><td>{@code @Default("0")}</td></tr>
 *     <tr><td>{@code String}</td><td>any text, at most the field's {@link Length} when it has one</td><td>{@code @Default("en")}</td></tr>
 *     <tr><td>{@code UUID}</td><td>a UUID in its 36-character form</td><td>{@code @Default("00000000-0000-0000-0000-000000000000")}</td></tr>
 * </table>
 * A value that does not fit the field's type is refused when the entity is read.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Default {
	/**
	 * Returns the default, as text read by the field's type.
	 *
	 * @return the default
	 */
	String value();
}
