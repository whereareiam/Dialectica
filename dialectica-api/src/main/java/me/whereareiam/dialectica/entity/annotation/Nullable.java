package me.whereareiam.dialectica.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets the column of a field hold {@code NULL}. Columns refuse it otherwise.
 * <p>
 * Any annotation named {@code Nullable} that is visible at runtime counts as well, such as
 * {@code jakarta.annotation.Nullable} or {@code javax.annotation.Nullable}. JetBrains'
 * {@code org.jetbrains.annotations.Nullable} is not visible at runtime and is not seen. A primitive
 * field cannot be nullable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE_USE})
public @interface Nullable {
}
