package me.whereareiam.dialectica.dialect;

/**
 * How a {@link Condition} compares a column's current value. Every comparison but {@link #IS_NULL}
 * compares with a parameter; a column that is {@code NULL} satisfies none of them.
 */
public enum Comparison {
	/**
	 * The column equals the parameter: {@code column = ?}.
	 */
	EQUALS,

	/**
	 * The column is {@code NULL}: {@code column IS NULL}. Takes no parameter.
	 */
	IS_NULL,

	/**
	 * The column is less than the parameter: {@code column < ?}.
	 */
	BELOW,

	/**
	 * The column is greater than the parameter: {@code column > ?}.
	 */
	ABOVE,

	/**
	 * The column is less than or equal to the parameter: {@code column <= ?}.
	 */
	AT_MOST,

	/**
	 * The column is greater than or equal to the parameter: {@code column >= ?}.
	 */
	AT_LEAST;

	/**
	 * Returns whether the comparison compares with a parameter.
	 *
	 * @return {@code false} for {@link #IS_NULL}, {@code true} otherwise
	 */
	public boolean takesValue() {
		return this != IS_NULL;
	}

	/**
	 * Returns whether the comparison orders values, which only number columns allow.
	 *
	 * @return {@code true} for {@link #BELOW}, {@link #ABOVE}, {@link #AT_MOST} and {@link #AT_LEAST}
	 */
	public boolean isOrdered() {
		return this != EQUALS && this != IS_NULL;
	}
}
