package me.whereareiam.dialectica.schema;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A foreign key of a {@link TableDefinition}: columns of the table that reference columns of another table,
 * or of the same one. The referenced table is named, so it may belong to the same {@link Schema} or
 * to one created by another owner beforehand.
 * <pre>{@code
 * ForeignKeyDefinition account = ForeignKeyDefinition.builder("unique_id")
 *         .references("identica_accounts", "unique_id")
 *         .onDelete(ReferentialAction.CASCADE)
 *         .build();
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ForeignKeyDefinition {
	/**
	 * Referencing columns of the owning table, in order.
	 */
	private final @NotNull List<String> columns;

	/**
	 * Name of the referenced table.
	 */
	private final @NotNull String referencedTable;

	/**
	 * Referenced columns, in the order of {@link #columns}. They have to form the referenced
	 * table's primary key or a unique key.
	 */
	private final @NotNull List<String> referencedColumns;

	/**
	 * What happens to referencing rows when the referenced row is deleted; {@link ReferentialAction#NO_ACTION}
	 * unless set.
	 */
	private final @NotNull ReferentialAction onDelete;

	private ForeignKeyDefinition(@NotNull Builder builder) {
		this.columns = builder.columns;
		this.referencedTable = requireName(builder.referencedTable, "referenced table");
		this.referencedColumns = builder.referencedColumns;
		this.onDelete = builder.onDelete;
		if (columns.size() != referencedColumns.size())
			throw new IllegalArgumentException("A foreign key on " + columns + " must reference as many columns, not " + referencedColumns);
	}

	/**
	 * Starts a foreign key over the given columns of the owning table.
	 *
	 * @param columns referencing columns, in order
	 * @return a builder for the foreign key
	 * @throws IllegalArgumentException when no column is given or a name is blank
	 */
	public static @NotNull Builder builder(@NotNull String... columns) {
		return new Builder(requireNames(columns, "column"));
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
	 * Builds a {@link ForeignKeyDefinition}.
	 */
	public static final class Builder {
		private final List<String> columns;
		private String referencedTable;
		private List<String> referencedColumns;
		private ReferentialAction onDelete = ReferentialAction.NO_ACTION;

		private Builder(List<String> columns) {
			this.columns = columns;
		}

		/**
		 * Names the referenced table and its columns.
		 *
		 * @param table   referenced table
		 * @param columns referenced columns, in the order of the referencing ones
		 * @return this builder
		 * @throws IllegalArgumentException when no column is given or a name is blank
		 */
		public @NotNull Builder references(@NotNull String table, @NotNull String... columns) {
			this.referencedTable = requireName(table, "referenced table");
			this.referencedColumns = requireNames(columns, "referenced column");
			return this;
		}

		/**
		 * Sets what happens to referencing rows when the referenced row is deleted.
		 *
		 * @param action action on delete
		 * @return this builder
		 */
		public @NotNull Builder onDelete(@NotNull ReferentialAction action) {
			this.onDelete = Objects.requireNonNull(action, "action");
			return this;
		}

		/**
		 * Builds the foreign key.
		 *
		 * @return the foreign key
		 * @throws NullPointerException     when {@link #references(String, String...)} was not called
		 * @throws IllegalArgumentException when the referencing and referenced columns differ in number
		 */
		public @NotNull ForeignKeyDefinition build() {
			Objects.requireNonNull(referencedColumns, "referenced columns");

			return new ForeignKeyDefinition(this);
		}
	}
}
