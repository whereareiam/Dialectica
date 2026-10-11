package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.Dialect;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaConfig;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.entity.Row;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps rows to entities of an {@link EntityTable}. Result columns are matched to the table's
 * columns by name, ignoring case, so {@code SELECT *} needs no aliases. A column of the table that
 * the result lacks reads as {@code null}; result columns the table lacks are ignored.
 *
 * @param <T> entity type
 */
public final class EntityRowMapper<T> implements RowMapper<T> {
	private final EntityTable<T> table;

	/**
	 * Creates a mapper for an entity table.
	 *
	 * @param table entity table to map rows to
	 */
	public EntityRowMapper(@NotNull EntityTable<T> table) {
		this.table = Objects.requireNonNull(table, "table");
	}

	@Override
	public T map(ResultSet rows, StatementContext context) throws SQLException {
		return specialize(rows, context).map(rows, context);
	}

	@Override
	public RowMapper<T> specialize(ResultSet rows, StatementContext context) throws SQLException {
		Dialect dialect = context.getConfig(DialecticaConfig.class).dialect();
		List<EntityColumn<T, ?>> columns = table.columns();
		int[] positions = positions(rows.getMetaData(), columns);

		return (row, rowContext) -> {
			Map<EntityColumn<T, ?>, Object> values = new HashMap<>();
			for (int index = 0; index < positions.length; index++) {
				if (positions[index] == 0) continue;

				EntityColumn<T, ?> column = columns.get(index);
				values.put(column, dialect.read(row, positions[index], column.column().getType()));
			}

			return table.create(new Row<>() {
				@Override
				@SuppressWarnings("unchecked")
				public <V> V get(@NotNull EntityColumn<T, V> column) {
					return (V) values.get(column);
				}
			});
		};
	}

	private static <T> int[] positions(ResultSetMetaData metaData, List<EntityColumn<T, ?>> columns) throws SQLException {
		int[] positions = new int[columns.size()];
		for (int position = 1; position <= metaData.getColumnCount(); position++) {
			String label = metaData.getColumnLabel(position);
			for (int index = 0; index < columns.size(); index++)
				if (positions[index] == 0 && columns.get(index).name().equalsIgnoreCase(label)) positions[index] = position;
		}

		return positions;
	}
}
