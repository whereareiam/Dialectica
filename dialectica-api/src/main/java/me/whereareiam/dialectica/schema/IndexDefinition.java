package me.whereareiam.dialectica.schema;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An index over columns of a {@link TableDefinition}. Index names share one namespace per schema on
 * PostgreSQL, H2 and SQLite, so prefix them with their table's name.
 * <p>
 * A unique index accepts any number of rows whose indexed columns contain {@code NULL}, on every
 * dialect; only rows whose indexed columns are all set have to differ.
 * <pre>{@code
 * IndexDefinition byUsername = IndexDefinition.plain("identica_accounts_username", "username");
 * IndexDefinition byPrimary = IndexDefinition.unique("identica_provider_links_primary", "unique_id", "is_primary");
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class IndexDefinition {
	/**
	 * Name of the index.
	 */
	private final @NotNull String name;

	/**
	 * Indexed columns, in order.
	 */
	private final @NotNull List<String> columns;

	/**
	 * Whether two rows may not share the same values in the indexed columns.
	 */
	private final boolean unique;

	private IndexDefinition(@NotNull String name, @NotNull String[] columns, boolean unique) {
		this.name = requireName(name, "index");
		this.columns = requireNames(columns, "column");
		this.unique = unique;
	}

	/**
	 * Creates an index that only speeds lookups up.
	 *
	 * @param name    name of the index
	 * @param columns indexed columns, in order
	 * @return the index
	 * @throws IllegalArgumentException when no column is given or a name is blank
	 */
	public static @NotNull IndexDefinition plain(@NotNull String name, @NotNull String... columns) {
		return new IndexDefinition(name, columns, false);
	}

	/**
	 * Creates an index that also keeps two rows from sharing the same values.
	 *
	 * @param name    name of the index
	 * @param columns indexed columns, in order
	 * @return the index
	 * @throws IllegalArgumentException when no column is given or a name is blank
	 */
	public static @NotNull IndexDefinition unique(@NotNull String name, @NotNull String... columns) {
		return new IndexDefinition(name, columns, true);
	}

	private static String requireName(String name, String kind) {
		Objects.requireNonNull(name, kind + " name");
		if (name.isBlank()) throw new IllegalArgumentException("A " + kind + " name must not be blank");

		return name;
	}

	private static List<String> requireNames(String[] names, String kind) {
		Objects.requireNonNull(names, kind + " names");
		if (names.length == 0) throw new IllegalArgumentException("At least one " + kind + " name is required");
		for (String name : names) requireName(name, kind);

		return List.copyOf(Arrays.asList(names));
	}
}
