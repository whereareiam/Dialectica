package me.whereareiam.dialectica.adapter.jdbi;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.adapter.jdbi.entity.EntityAccess;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the {@link Dialect} that {@link DialecticaPlugin} chose for a {@code Jdbi}, so that
 * mappers, arguments and {@link EntityAccess} speak it.
 */
public final class DialecticaConfig implements JdbiConfig<DialecticaConfig> {
	private Dialect dialect;

	/**
	 * Creates an empty configuration; Jdbi calls it.
	 */
	public DialecticaConfig() {
	}

	private DialecticaConfig(Dialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * Returns the dialect of the database.
	 *
	 * @return the dialect
	 * @throws IllegalStateException when {@link DialecticaPlugin} is not installed
	 */
	public @NotNull Dialect dialect() {
		if (dialect == null) throw new IllegalStateException("Install " + DialecticaPlugin.class.getSimpleName() + " on this Jdbi first");

		return dialect;
	}

	void dialect(@NotNull Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public DialecticaConfig createCopy() {
		return new DialecticaConfig(dialect);
	}
}
