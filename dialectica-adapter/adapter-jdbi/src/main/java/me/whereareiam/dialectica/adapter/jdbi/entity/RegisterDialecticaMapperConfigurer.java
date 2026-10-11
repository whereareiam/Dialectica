package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.common.entity.EntityReader;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionConfigurer;
import org.jdbi.v3.core.mapper.RowMappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Applies {@link RegisterDialecticaMapper}. Jdbi creates and calls it; it is public only because
 * Jdbi requires that.
 */
public final class RegisterDialecticaMapperConfigurer implements ExtensionConfigurer {
	/**
	 * Creates the configurer; Jdbi looks for a public constructor.
	 */
	public RegisterDialecticaMapperConfigurer() {
	}

	@Override
	public void configureForType(ConfigRegistry config, Annotation annotation, Class<?> extensionType) {
		register(config, annotation);
	}

	@Override
	public void configureForMethod(ConfigRegistry config, Annotation annotation, Class<?> extensionType, Method method) {
		register(config, annotation);
	}

	private static void register(ConfigRegistry config, Annotation annotation) {
		if (annotation instanceof RegisterDialecticaMapper.List list) {
			for (RegisterDialecticaMapper single : list.value())
				register(config, single);
			return;
		}

		register(config, ((RegisterDialecticaMapper) annotation).value());
	}

	private static <T> void register(ConfigRegistry config, Class<T> entity) {
		config.get(RowMappers.class).register(entity, new EntityRowMapper<>(EntityReader.read(entity)));
	}
}
