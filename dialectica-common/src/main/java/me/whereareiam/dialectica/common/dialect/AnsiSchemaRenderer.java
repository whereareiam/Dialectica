package me.whereareiam.dialectica.common.dialect;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.schema.ColumnDefinition;
import me.whereareiam.dialectica.schema.ForeignKeyDefinition;
import me.whereareiam.dialectica.schema.IndexDefinition;
import me.whereareiam.dialectica.schema.TableDefinition;
import me.whereareiam.dialectica.schema.change.AddColumn;
import me.whereareiam.dialectica.schema.change.CreateIndex;
import me.whereareiam.dialectica.schema.change.CreateTable;
import me.whereareiam.dialectica.schema.change.DropColumn;
import me.whereareiam.dialectica.schema.change.DropIndex;
import me.whereareiam.dialectica.schema.change.DropTable;
import me.whereareiam.dialectica.schema.change.RenameTable;
import me.whereareiam.dialectica.schema.change.SchemaChange;
import me.whereareiam.dialectica.schema.type.ColumnType;
import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Renders the schema SQL that every supported dialect shares. A dialect's renderer supplies its
 * column types and overrides only the statements it writes differently.
 */
public abstract class AnsiSchemaRenderer implements SchemaRenderer {
	private final @NotNull Dialect dialect;

	/**
	 * Creates the renderer of a dialect, which names tables, columns and indexes.
	 *
	 * @param dialect dialect whose SQL is rendered
	 */
	protected AnsiSchemaRenderer(@NotNull Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean dropsCoveredColumns() {
		return true;
	}

	@Override
	public @NotNull List<String> render(@NotNull SchemaChange change) {
		if (change instanceof CreateTable createTable) return createTable(createTable);
		if (change instanceof DropTable dropTable) return List.of(dropTable(dropTable));
		if (change instanceof RenameTable renameTable) return List.of(renameTable(renameTable));
		if (change instanceof AddColumn addColumn) return List.of(addColumn(addColumn));
		if (change instanceof DropColumn dropColumn) return List.of(dropColumn(dropColumn));
		if (change instanceof CreateIndex createIndex) return List.of(createIndex(createIndex.getTable(), createIndex.getIndex()));
		if (change instanceof DropIndex dropIndex) return List.of(dropIndex(dropIndex));

		throw new IllegalArgumentException("Unknown schema change " + change);
	}

	/**
	 * Renders a table followed by its indexes.
	 *
	 * @param change change to render
	 * @return the table's statement, then one statement per index
	 */
	protected @NotNull List<String> createTable(@NotNull CreateTable change) {
		TableDefinition table = change.getTable();
		StringJoiner definitions = new StringJoiner(", ", "CREATE TABLE " + identifier(table.getName()) + " (", ")");
		for (ColumnDefinition column : table.getColumns())
			definitions.add(column(column));

		if (!table.getPrimaryKey().isEmpty())
			definitions.add("PRIMARY KEY " + identifiers(table.getPrimaryKey()));
		for (List<String> uniqueKey : table.getUniqueKeys())
			definitions.add("UNIQUE " + identifiers(uniqueKey));
		for (ForeignKeyDefinition foreignKey : table.getForeignKeys())
			definitions.add(foreignKey(foreignKey));

		List<String> statements = new ArrayList<>();
		statements.add(definitions.toString());
		for (IndexDefinition index : table.getIndexes())
			statements.add(createIndex(table.getName(), index));

		return statements;
	}

	/**
	 * Renders the drop of a table.
	 *
	 * @param change change to render
	 * @return the statement
	 */
	protected @NotNull String dropTable(@NotNull DropTable change) {
		return "DROP TABLE " + identifier(change.getTable());
	}

	/**
	 * Renders the rename of a table.
	 *
	 * @param change change to render
	 * @return the statement
	 */
	protected @NotNull String renameTable(@NotNull RenameTable change) {
		return "ALTER TABLE " + identifier(change.getFrom()) + " RENAME TO " + identifier(change.getTo());
	}

	/**
	 * Renders the addition of a column.
	 *
	 * @param change change to render
	 * @return the statement
	 */
	protected @NotNull String addColumn(@NotNull AddColumn change) {
		return "ALTER TABLE " + identifier(change.getTable()) + " ADD COLUMN " + column(change.getColumn());
	}

	/**
	 * Renders the drop of a column.
	 *
	 * @param change change to render
	 * @return the statement
	 */
	protected @NotNull String dropColumn(@NotNull DropColumn change) {
		return "ALTER TABLE " + identifier(change.getTable()) + " DROP COLUMN " + identifier(change.getColumn());
	}

	/**
	 * Renders the creation of an index.
	 *
	 * @param table name of the indexed table
	 * @param index index to create
	 * @return the statement
	 */
	protected @NotNull String createIndex(@NotNull String table, @NotNull IndexDefinition index) {
		return "CREATE " + (index.isUnique() ? "UNIQUE " : "") + "INDEX " + identifier(index.getName())
				+ " ON " + identifier(table) + " " + identifiers(index.getColumns());
	}

	/**
	 * Renders the drop of an index, whose name is unique within its schema.
	 *
	 * @param change change to render
	 * @return the statement
	 */
	protected @NotNull String dropIndex(@NotNull DropIndex change) {
		return "DROP INDEX " + identifier(change.getIndex());
	}

	/**
	 * Renders a column definition: name, type, default and nullability.
	 *
	 * @param column column to render
	 * @return the definition
	 */
	protected @NotNull String column(@NotNull ColumnDefinition column) {
		StringBuilder definition = new StringBuilder(identifier(column.getName())).append(' ').append(type(column.getType()));
		if (column.getDefaultValue() != null)
			definition.append(" DEFAULT ").append(defaultValue(column.getType(), column.getDefaultValue()));
		if (!column.isNullable())
			definition.append(" NOT NULL");

		return definition.toString();
	}

	/**
	 * Renders the dialect's type of a column.
	 *
	 * @param type type to render
	 * @return the dialect's type
	 */
	protected abstract @NotNull String type(@NotNull ColumnType type);

	/**
	 * Renders the default of a column.
	 *
	 * @param type  type of the column
	 * @param value literal of the default
	 * @return the default as written after {@code DEFAULT}
	 */
	protected @NotNull String defaultValue(@NotNull ColumnType type, @NotNull Object value) {
		if (value instanceof String text) return string(text);
		if (value instanceof Boolean truth) return bool(truth);
		if (value instanceof Long number) return number.toString();

		throw new IllegalArgumentException("Unsupported default literal " + value);
	}

	/**
	 * Renders a string literal.
	 *
	 * @param text text of the literal
	 * @return the quoted literal
	 */
	protected @NotNull String string(@NotNull String text) {
		return "'" + text.replace("'", "''") + "'";
	}

	/**
	 * Renders a boolean literal.
	 *
	 * @param truth value of the literal
	 * @return the literal
	 */
	protected @NotNull String bool(boolean truth) {
		return truth ? "TRUE" : "FALSE";
	}

	/**
	 * Returns the dialect this renderer renders for.
	 *
	 * @return the dialect
	 */
	protected final @NotNull Dialect dialect() {
		return dialect;
	}

	/**
	 * Writes a name as the dialect does.
	 *
	 * @param name name as declared
	 * @return the name as written in SQL
	 */
	protected final @NotNull String identifier(@NotNull String name) {
		return dialect.identifier(name);
	}

	private String identifiers(List<String> names) {
		StringJoiner joiner = new StringJoiner(", ", "(", ")");
		for (String name : names)
			joiner.add(identifier(name));

		return joiner.toString();
	}

	private String foreignKey(ForeignKeyDefinition foreignKey) {
		return "FOREIGN KEY " + identifiers(foreignKey.getColumns())
				+ " REFERENCES " + identifier(foreignKey.getReferencedTable()) + " " + identifiers(foreignKey.getReferencedColumns())
				+ " ON DELETE " + referentialAction(foreignKey.getOnDelete());
	}

	private static String referentialAction(ReferentialAction action) {
		return switch (action) {
			case CASCADE -> "CASCADE";
			case SET_NULL -> "SET NULL";
			case RESTRICT -> "RESTRICT";
			case NO_ACTION -> "NO ACTION";
		};
	}
}
