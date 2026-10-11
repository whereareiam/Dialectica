package me.whereareiam.dialectica.schema.change;

import me.whereareiam.dialectica.dialect.SchemaRenderer;

/**
 * One change to a database schema, independent of any dialect. A {@link SchemaRenderer} turns it
 * into the statements of one dialect.
 */
public sealed interface SchemaChange permits
		CreateTable,
		DropTable,
		RenameTable,
		AddColumn,
		DropColumn,
		CreateIndex,
		DropIndex {
}
