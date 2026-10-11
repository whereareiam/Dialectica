package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.adapter.jdbi.DialecticaPlugin;
import org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an {@link EntityRowMapper} for an annotated entity on a SqlObject interface or method,
 * so hand-written queries return entities:
 * <pre>{@code
 * @RegisterDialecticaMapper(Account.class)
 * public interface AccountQueries {
 *     @SqlQuery("SELECT * FROM identica_accounts WHERE LOWER(username) = LOWER(:username)")
 *     List<Account> findByUsername(@Bind("username") String username);
 * }
 * }</pre>
 * Columns are matched by name, so the query needs no aliases. {@link DialecticaPlugin} has to be
 * installed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RegisterDialecticaMapper.List.class)
@UseExtensionConfigurer(RegisterDialecticaMapperConfigurer.class)
public @interface RegisterDialecticaMapper {
	/**
	 * Returns the entity whose rows are mapped.
	 *
	 * @return the entity type
	 */
	Class<?> value();

	/**
	 * Holds several {@link RegisterDialecticaMapper} annotations.
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	@UseExtensionConfigurer(RegisterDialecticaMapperConfigurer.class)
	@interface List {
		/**
		 * Returns the registrations.
		 *
		 * @return the registrations
		 */
		RegisterDialecticaMapper[] value();
	}
}
