package me.whereareiam.dialectica.schema.type;

/**
 * What the database does with referencing rows when the row they reference is deleted.
 */
public enum ReferentialAction {
	/**
	 * Deletes the referencing rows too.
	 */
	CASCADE,

	/**
	 * Sets the referencing columns to {@code NULL}; they have to be nullable.
	 */
	SET_NULL,

	/**
	 * Refuses the deletion.
	 */
	RESTRICT,

	/**
	 * Refuses the deletion; the default of every dialect. PostgreSQL and H2 check it when the
	 * statement ends instead of immediately.
	 */
	NO_ACTION
}
