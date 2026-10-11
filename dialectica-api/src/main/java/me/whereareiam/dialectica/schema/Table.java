package me.whereareiam.dialectica.schema;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A table: its columns, its keys and its indexes.
 * <pre>{@code
 * Table links = Table.builder("identica_provider_links")
 *         .column(Column.builder("provider_id", ColumnType.varchar(64)).notNull().build())
 *         .column(Column.builder("provider_subject", ColumnType.varchar(128)).notNull().build())
 *         .column(Column.builder("unique_id", ColumnType.uuid()).notNull().build())
 *         .column(Column.builder("linked_at", ColumnType.bigint()).build())
 *         .primaryKey("provider_id", "provider_subject")
 *         .unique("unique_id", "provider_id")
 *         .foreignKey(ForeignKey.builder("unique_id")
 *                 .references("identica_accounts", "unique_id")
 *                 .onDelete(ReferentialAction.CASCADE)
 *                 .build())
 *         .index(Index.plain("identica_provider_links_linked_at", "linked_at"))
 *         .build();
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Table {
	/**
	 * Name of the table, written as given. A name that is a lowercase snake_case word and not
	 * reserved by a dialect is rendered without quotes.
	 */
	private final @NotNull String name;

	/**
	 * Columns, in the order they are created.
	 */
	private final @NotNull List<Column> columns;

	/**
	 * Columns of the primary key, in order; empty for a table without one.
	 */
	private final @NotNull List<String> primaryKey;

	/**
	 * Unique constraints, each the columns whose combined values no two rows may share. Like a
	 * unique {@link Index}, they accept any number of rows with a {@code NULL} among the columns.
	 */
	private final @NotNull List<List<String>> uniqueKeys;

	/**
	 * Foreign keys of the table.
	 */
	private final @NotNull List<ForeignKey> foreignKeys;

	/**
	 * Indexes, created right after the table.
	 */
	private final @NotNull List<Index> indexes;

	private Table(@NotNull Builder builder) {
		this.name = builder.name;
		this.columns = List.copyOf(builder.columns);
		this.primaryKey = builder.primaryKey;
		this.uniqueKeys = List.copyOf(builder.uniqueKeys);
		this.foreignKeys = List.copyOf(builder.foreignKeys);
		this.indexes = List.copyOf(builder.indexes);

		validate();
	}

	/**
	 * Starts a table.
	 *
	 * @param name name of the table
	 * @return a builder for the table
	 * @throws IllegalArgumentException when the name is blank
	 */
	public static @NotNull Builder builder(@NotNull String name) {
		return new Builder(requireName(name, "table"));
	}

	/**
	 * Finds a column of this table.
	 *
	 * @param name name of the column
	 * @return the column, or {@code null} when the table has none of that name
	 */
	public @Nullable Column column(@NotNull String name) {
		for (Column column : columns)
			if (column.getName().equals(name)) return column;

		return null;
	}

	private void validate() {
		if (columns.isEmpty()) throw new IllegalArgumentException("Table " + name + " needs at least one column");

		Set<String> names = new HashSet<>();
		for (Column column : columns)
			if (!names.add(column.getName()))
				throw new IllegalArgumentException("Table " + name + " declares column " + column.getName() + " twice");

		for (String key : primaryKey) {
			Column column = requireColumn(key, "primary key");
			if (column.isNullable())
				throw new IllegalArgumentException("Primary key column " + name + "." + key + " must be declared not null");
		}

		for (List<String> uniqueKey : uniqueKeys)
			uniqueKey.forEach(column -> requireColumn(column, "unique key"));
		for (ForeignKey foreignKey : foreignKeys)
			foreignKey.getColumns().forEach(column -> requireColumn(column, "foreign key"));

		Set<String> indexNames = new HashSet<>();
		for (Index index : indexes) {
			if (!indexNames.add(index.getName()))
				throw new IllegalArgumentException("Table " + name + " declares index " + index.getName() + " twice");

			index.getColumns().forEach(column -> requireColumn(column, "index " + index.getName()));
		}
	}

	private Column requireColumn(String columnName, String use) {
		Column column = column(columnName);
		if (column == null)
			throw new IllegalArgumentException("The " + use + " of table " + name + " names the unknown column " + columnName);

		return column;
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

	/**
	 * Builds a {@link Table}.
	 */
	public static final class Builder {
		private final String name;
		private final List<Column> columns = new ArrayList<>();
		private List<String> primaryKey = List.of();
		private final List<List<String>> uniqueKeys = new ArrayList<>();
		private final List<ForeignKey> foreignKeys = new ArrayList<>();
		private final List<Index> indexes = new ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		/**
		 * Adds a column after the ones added before.
		 *
		 * @param column column to add
		 * @return this builder
		 */
		public @NotNull Builder column(@NotNull Column column) {
			columns.add(Objects.requireNonNull(column, "column"));
			return this;
		}

		/**
		 * Sets the primary key. Its columns have to be declared not null.
		 *
		 * @param columns columns of the key, in order
		 * @return this builder
		 * @throws IllegalArgumentException when no column is given or a name is blank
		 */
		public @NotNull Builder primaryKey(@NotNull String... columns) {
			this.primaryKey = requireNames(columns, "primary key column");
			return this;
		}

		/**
		 * Adds a unique constraint over the given columns.
		 *
		 * @param columns columns whose combined values no two rows may share
		 * @return this builder
		 * @throws IllegalArgumentException when no column is given or a name is blank
		 */
		public @NotNull Builder unique(@NotNull String... columns) {
			uniqueKeys.add(requireNames(columns, "unique key column"));
			return this;
		}

		/**
		 * Adds a foreign key.
		 *
		 * @param foreignKey foreign key to add
		 * @return this builder
		 */
		public @NotNull Builder foreignKey(@NotNull ForeignKey foreignKey) {
			foreignKeys.add(Objects.requireNonNull(foreignKey, "foreign key"));
			return this;
		}

		/**
		 * Adds an index.
		 *
		 * @param index index to add
		 * @return this builder
		 */
		public @NotNull Builder index(@NotNull Index index) {
			indexes.add(Objects.requireNonNull(index, "index"));
			return this;
		}

		/**
		 * Builds the table.
		 *
		 * @return the table
		 * @throws IllegalArgumentException when the table has no column, declares a column or an
		 *                                  index twice, names an unknown column in a key or an
		 *                                  index, or has a nullable primary key column
		 */
		public @NotNull Table build() {
			return new Table(this);
		}
	}
}
