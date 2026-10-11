package me.whereareiam.dialectica.schema;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.dialectica.schema.type.ColumnType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A column of a {@link Table}.
 * <pre>{@code
 * Column username = Column.builder("username", ColumnType.varchar(64))
 *         .notNull()
 *         .build();
 * Column primary = Column.builder("is_primary", ColumnType.bool())
 *         .notNull()
 *         .defaultValue(false)
 *         .build();
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Column {
	/**
	 * Name of the column, written as given. A name that is a lowercase snake_case word and not
	 * reserved by a dialect is rendered without quotes.
	 */
	private final @NotNull String name;

	/**
	 * Type of the column.
	 */
	private final @NotNull ColumnType type;

	/**
	 * Whether the column accepts {@code NULL}. Columns accept it unless built with
	 * {@link Builder#notNull()}.
	 */
	private final boolean nullable;

	/**
	 * Literal the column takes when an insert leaves it out: a {@link String}, a {@link Long} or a
	 * {@link Boolean}, or {@code null} for no default.
	 */
	private final @Nullable Object defaultValue;

	private Column(@NotNull Builder builder) {
		this.name = requireName(builder.name, "column");
		this.type = Objects.requireNonNull(builder.type, "type");
		this.nullable = builder.nullable;
		this.defaultValue = builder.defaultValue;
	}

	/**
	 * Starts a nullable column without a default.
	 *
	 * @param name name of the column
	 * @param type type of the column
	 * @return a builder for the column
	 */
	public static @NotNull Builder builder(@NotNull String name, @NotNull ColumnType type) {
		return new Builder(name, type);
	}

	private static String requireName(String name, String kind) {
		Objects.requireNonNull(name, kind + " name");
		if (name.isBlank()) throw new IllegalArgumentException("A " + kind + " name must not be blank");

		return name;
	}

	/**
	 * Builds a {@link Column}.
	 */
	public static final class Builder {
		private final String name;
		private final ColumnType type;
		private boolean nullable = true;
		private Object defaultValue;

		private Builder(String name, ColumnType type) {
			this.name = name;
			this.type = type;
		}

		/**
		 * Makes the column refuse {@code NULL}.
		 *
		 * @return this builder
		 */
		public @NotNull Builder notNull() {
			this.nullable = false;
			return this;
		}

		/**
		 * Gives the column a text default, rendered as a quoted SQL string.
		 *
		 * @param value default text
		 * @return this builder
		 */
		public @NotNull Builder defaultValue(@NotNull String value) {
			this.defaultValue = Objects.requireNonNull(value, "value");
			return this;
		}

		/**
		 * Gives the column a numeric default.
		 *
		 * @param value default number
		 * @return this builder
		 */
		public @NotNull Builder defaultValue(long value) {
			this.defaultValue = value;
			return this;
		}

		/**
		 * Gives the column a boolean default, rendered as {@code 1} or {@code 0} on SQLite and as
		 * {@code TRUE} or {@code FALSE} elsewhere.
		 *
		 * @param value default truth value
		 * @return this builder
		 */
		public @NotNull Builder defaultValue(boolean value) {
			this.defaultValue = value;
			return this;
		}

		/**
		 * Builds the column.
		 *
		 * @return the column
		 * @throws IllegalArgumentException when the name is blank
		 */
		public @NotNull Column build() {
			return new Column(this);
		}
	}
}
