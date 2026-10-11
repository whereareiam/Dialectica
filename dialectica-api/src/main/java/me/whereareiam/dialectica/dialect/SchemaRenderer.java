package me.whereareiam.dialectica.dialect;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.exception.UnsupportedChangeException;
import me.whereareiam.dialectica.schema.change.SchemaChange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders schema changes as the SQL statements of one {@link Dialect}.
 * <p>
 * A renderer only produces text; it neither connects to a database nor checks what already
 * exists there. Statements are plain DDL without {@code IF [NOT] EXISTS}, so rendering a change
 * twice and running both results fails the second time.
 * <pre>{@code
 * SchemaRenderer renderer = Dialect.of(connection.getMetaData()).schemaRenderer();
 * try (Statement jdbc = connection.createStatement()) {
 *     for (String sql : renderer.render(new CreateTable(accounts)))
 *         jdbc.execute(sql);
 * }
 * }</pre>
 */
public interface SchemaRenderer {
	/**
	 * Renders one change as the statements that carry it out, in the order they have to run.
	 * Creating a table yields the table's statement followed by one statement per index.
	 *
	 * @param change change to render
	 * @return statements without a trailing semicolon
	 * @throws UnsupportedChangeException when the dialect can only carry the change out by
	 *                                    rebuilding the table
	 */
	@NotNull List<String> render(@NotNull SchemaChange change);

	/**
	 * Tells whether the dialect drops a column that belongs to a key or an index. SQLite does not:
	 * the table would have to be rebuilt.
	 *
	 * @return whether such a column can be dropped
	 */
	boolean dropsCoveredColumns();
}
