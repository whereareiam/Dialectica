package me.whereareiam.dialectica;

import me.whereareiam.dialectica.common.schema.JdbcInspection;
import me.whereareiam.dialectica.dialect.SchemaRenderer;
import me.whereareiam.dialectica.exception.UnsupportedChangeException;
import me.whereareiam.dialectica.schema.ForeignKeyDefinition;
import me.whereareiam.dialectica.schema.IndexDefinition;
import me.whereareiam.dialectica.schema.Schema;
import me.whereareiam.dialectica.schema.TableDefinition;
import me.whereareiam.dialectica.schema.change.AddColumn;
import me.whereareiam.dialectica.schema.change.CreateIndex;
import me.whereareiam.dialectica.schema.change.CreateTable;
import me.whereareiam.dialectica.schema.change.DropColumn;
import me.whereareiam.dialectica.schema.change.DropIndex;
import me.whereareiam.dialectica.schema.change.DropTable;
import me.whereareiam.dialectica.schema.change.RenameTable;
import me.whereareiam.dialectica.schema.change.SchemaChange;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes schema changes to a database, skipping what already exists, so a writer can run the
 * same changes on every start.
 * <p>
 * Each change is checked against the database's metadata before it runs and is skipped when it
 * has already been made. This keeps reruns safe on MySQL, MariaDB and H2, where every DDL statement
 * commits on its own, and on MySQL, which has no {@code IF NOT EXISTS} for indexes and columns:
 * when a run fails halfway, the next run carries on where it stopped.
 * <table>
 *     <caption>When a change is skipped</caption>
 *     <tr><th>Change</th><th>Skipped when</th></tr>
 *     <tr><td>{@link CreateTable}</td><td>the table exists; its missing indexes are still created</td></tr>
 *     <tr><td>{@link DropTable}</td><td>the table does not exist</td></tr>
 *     <tr><td>{@link RenameTable}</td><td>no table has the old name</td></tr>
 *     <tr><td>{@link AddColumn}</td><td>the column exists</td></tr>
 *     <tr><td>{@link DropColumn}</td><td>the column does not exist</td></tr>
 *     <tr><td>{@link CreateIndex}</td><td>an index of that name exists on the table</td></tr>
 *     <tr><td>{@link DropIndex}</td><td>no index of that name exists on the table</td></tr>
 * </table>
 * An existing table is not compared with its model: a column that a newer model adds is added by
 * an {@link AddColumn}, not by creating the table again.
 * <p>
 * The writer runs statements on the connection as it is and leaves transactions to the caller.
 * It takes no lock: instances that share a database and start together have to serialize their
 * schema writes themselves, for example by running them inside a migration that holds a lock.
 */
public final class SchemaWriter {
	private final Connection connection;
	private final Dialect dialect;
	private final SchemaRenderer renderer;
	private final JdbcInspection inspection;

	SchemaWriter(@NotNull Connection connection, @NotNull Dialect dialect, @NotNull JdbcInspection inspection) {
		this.connection = connection;
		this.dialect = dialect;
		this.renderer = dialect.schemaRenderer();
		this.inspection = inspection;
	}

	/**
	 * Creates the tables of a schema that do not exist yet, and the missing indexes of every table.
	 * A table is created after the tables of the same schema it references; a referenced table of
	 * another owner has to exist already.
	 *
	 * @param schema schema to create
	 * @throws SQLException             when a statement or the metadata fails
	 * @throws IllegalArgumentException when tables of the schema reference each other in a cycle
	 */
	public void create(@NotNull Schema schema) throws SQLException {
		for (TableDefinition table : order(Objects.requireNonNull(schema, "schema")))
			apply(new CreateTable(table));
	}

	/**
	 * Applies changes in order, skipping each one that has already been made.
	 *
	 * @param changes changes to apply
	 * @throws SQLException                when a statement or the metadata fails
	 * @throws UnsupportedChangeException when the dialect cannot make a change without rebuilding
	 *                                     the table; the changes before it stay applied
	 */
	public void apply(@NotNull SchemaChange... changes) throws SQLException {
		for (SchemaChange change : changes)
			applyOne(Objects.requireNonNull(change, "change"));
	}

	private void applyOne(SchemaChange change) throws SQLException {
		if (change instanceof CreateTable createTable) {
			createTable(createTable);
			return;
		}

		if (!pending(change)) return;

		execute(renderer.render(change));
	}

	private void createTable(CreateTable change) throws SQLException {
		TableDefinition table = change.getTable();
		if (!inspection.tableExists(table.getName())) {
			execute(renderer.render(change));
			return;
		}

		for (IndexDefinition index : table.getIndexes())
			applyOne(new CreateIndex(table.getName(), index));
	}

	private boolean pending(SchemaChange change) throws SQLException {
		if (change instanceof DropTable drop) return inspection.tableExists(drop.getTable());
		if (change instanceof RenameTable rename) return inspection.tableExists(rename.getFrom());
		if (change instanceof AddColumn add) return !inspection.columnExists(add.getTable(), add.getColumn().getName());
		if (change instanceof DropColumn drop) return dropPending(drop);
		if (change instanceof CreateIndex create) return !inspection.indexExists(create.getTable(), create.getIndex().getName());
		if (change instanceof DropIndex drop) return inspection.indexExists(drop.getTable(), drop.getIndex());

		throw new IllegalArgumentException("Unknown schema change " + change);
	}

	private boolean dropPending(DropColumn change) throws SQLException {
		if (!inspection.columnExists(change.getTable(), change.getColumn())) return false;
		if (!renderer.dropsCoveredColumns() && inspection.columnKeyed(change.getTable(), change.getColumn()))
			throw new UnsupportedChangeException(dialect, change, "it drops only a column that belongs to no key and no index");

		return true;
	}

	private void execute(List<String> statements) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements)
				statement.execute(sql);
		}
	}

	private static List<TableDefinition> order(Schema schema) {
		Map<String, TableDefinition> pending = new LinkedHashMap<>();
		for (TableDefinition table : schema.getTables())
			pending.put(table.getName(), table);

		List<TableDefinition> ordered = new ArrayList<>();
		while (!pending.isEmpty()) {
			TableDefinition next = pending.values().stream()
					.filter(table -> !referencesPending(table, pending))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Tables " + pending.keySet() + " reference each other in a cycle"));

			ordered.add(next);
			pending.remove(next.getName());
		}

		return ordered;
	}

	private static boolean referencesPending(TableDefinition table, Map<String, TableDefinition> pending) {
		for (ForeignKeyDefinition foreignKey : table.getForeignKeys()) {
			String referenced = foreignKey.getReferencedTable();
			if (!referenced.equals(table.getName()) && pending.containsKey(referenced)) return true;
		}

		return false;
	}
}
