package me.whereareiam.dialectica;

import me.whereareiam.dialectica.fixture.Account;
import me.whereareiam.dialectica.fixture.AccountReservation;
import me.whereareiam.dialectica.fixture.Identica221;
import me.whereareiam.dialectica.fixture.ProviderLink;
import me.whereareiam.dialectica.fixture.ProviderProfile;
import me.whereareiam.dialectica.fixture.VerificationSelection;
import me.whereareiam.dialectica.schema.ColumnDefinition;
import me.whereareiam.dialectica.schema.ForeignKeyDefinition;
import me.whereareiam.dialectica.schema.IndexDefinition;
import me.whereareiam.dialectica.schema.Schema;
import me.whereareiam.dialectica.schema.TableDefinition;
import me.whereareiam.dialectica.schema.change.AddColumn;
import me.whereareiam.dialectica.schema.change.CreateIndex;
import me.whereareiam.dialectica.schema.change.DropColumn;
import me.whereareiam.dialectica.schema.change.DropIndex;
import me.whereareiam.dialectica.schema.change.DropTable;
import me.whereareiam.dialectica.schema.change.RenameTable;
import me.whereareiam.dialectica.schema.change.SchemaChange;
import me.whereareiam.dialectica.schema.type.ColumnType;
import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link SchemaWriter} guarantees on every dialect: a created schema matches its model in the
 * database's own metadata, and writing the same schema or changes again changes nothing. Each
 * dialect runs it against a database of its own.
 */
abstract class SchemaWriterContract {
	static final String ACCOUNT = "00000000-0000-0000-0000-000000000001";

	static final TableDefinition ACCOUNTS = TableDefinition.builder("dialectica_accounts")
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("username", ColumnType.varchar(64)).notNull().build())
			.column(ColumnDefinition.builder("created_at", ColumnType.bigint()).build())
			.primaryKey("unique_id")
			.index(IndexDefinition.plain("dialectica_accounts_username", "username"))
			.build();

	static final TableDefinition LINKS = TableDefinition.builder("dialectica_links")
			.column(ColumnDefinition.builder("provider_id", ColumnType.varchar(64)).notNull().build())
			.column(ColumnDefinition.builder("provider_subject", ColumnType.varchar(128)).notNull().build())
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).notNull().build())
			.column(ColumnDefinition.builder("is_primary", ColumnType.bool()).notNull().defaultValue(false).build())
			.column(ColumnDefinition.builder("attempts", ColumnType.integer()).notNull().defaultValue(3).build())
			.column(ColumnDefinition.builder("note", ColumnType.text()).defaultValue("it's a\\b").build())
			.column(ColumnDefinition.builder("external_id", ColumnType.varchar(64)).build())
			.primaryKey("provider_id", "provider_subject")
			.unique("unique_id", "provider_id")
			.foreignKey(ForeignKeyDefinition.builder("unique_id")
					.references("dialectica_accounts", "unique_id")
					.onDelete(ReferentialAction.CASCADE)
					.build())
			.index(IndexDefinition.unique("dialectica_links_external_id", "external_id"))
			.build();

	static final TableDefinition SESSIONS = TableDefinition.builder("dialectica_sessions")
			.column(ColumnDefinition.builder("id", ColumnType.bigint()).notNull().build())
			.column(ColumnDefinition.builder("unique_id", ColumnType.uuid()).build())
			.column(ColumnDefinition.builder("parent_id", ColumnType.bigint()).build())
			.primaryKey("id")
			.foreignKey(ForeignKeyDefinition.builder("unique_id")
					.references("dialectica_accounts", "unique_id")
					.onDelete(ReferentialAction.SET_NULL)
					.build())
			.foreignKey(ForeignKeyDefinition.builder("parent_id")
					.references("dialectica_sessions", "id")
					.onDelete(ReferentialAction.RESTRICT)
					.build())
			.build();

	private static final List<String> TABLES = List.of(
			"dialectica_sessions", "dialectica_renamed_links", "dialectica_links", "dialectica_accounts",
			"identica_verification_selections", "identica_provider_profiles", "identica_provider_links",
			"identica_account_reservations", "identica_accounts"
	);

	private static final List<String> IDENTICA_TABLES = List.of(
			"identica_accounts", "identica_provider_links", "identica_provider_profiles", "identica_account_reservations",
			"identica_verification_selections"
	);

	protected Connection connection;

	/**
	 * Opens a connection to an empty or reusable database of the dialect under test.
	 */
	protected abstract Connection open() throws SQLException;

	/**
	 * Returns the database type Identica 2.2.1 chose its DDL by for this dialect.
	 */
	protected abstract String identicaDatabaseType();

	/**
	 * Returns the type name the dialect's metadata reports for a column of the given type.
	 */
	protected abstract String typeName(ColumnType type);

	@BeforeEach
	void connect() throws SQLException {
		connection = open();
		dropTables();
	}

	@AfterEach
	void disconnect() throws SQLException {
		try (Connection open = connection) {
			dropTables();
		}
	}

	@Test
	void createsWhatTheModelDeclares() throws SQLException {
		Dialectica.on(connection).create(Schema.of(SESSIONS, LINKS, ACCOUNTS));

		for (TableDefinition table : List.of(ACCOUNTS, LINKS, SESSIONS))
			assertMatches(table);
	}

	@Test
	void createsTheSameSchemaTwiceAndRestoresMissingIndexes() throws SQLException {
		SchemaWriter writer = Dialectica.on(connection);
		writer.create(Schema.of(ACCOUNTS, LINKS));
		writer.apply(new DropIndex("dialectica_accounts", "dialectica_accounts_username"));

		writer.create(Schema.of(ACCOUNTS, LINKS));

		assertMatches(ACCOUNTS);
		assertMatches(LINKS);
	}

	@Test
	void createsATableThatReferencesAnotherOwnersTable() throws SQLException {
		SchemaWriter writer = Dialectica.on(connection);
		writer.create(Schema.of(ACCOUNTS));

		writer.create(Schema.of(LINKS));

		assertMatches(LINKS);
	}

	@Test
	void appliesChangesTwiceWithTheSameResult() throws SQLException {
		SchemaWriter writer = Dialectica.on(connection);
		writer.create(Schema.of(ACCOUNTS, LINKS));
		SchemaChange[] changes = {
				new AddColumn("dialectica_accounts", ColumnDefinition.builder("last_seen_at", ColumnType.bigint()).build()),
				new AddColumn("dialectica_accounts", ColumnDefinition.builder("is_locked", ColumnType.bool()).notNull().defaultValue(true).build()),
				new CreateIndex("dialectica_accounts", IndexDefinition.unique("dialectica_accounts_last_seen_at", "last_seen_at")),
				new DropIndex("dialectica_accounts", "dialectica_accounts_username"),
				new DropColumn("dialectica_accounts", "created_at"),
				new RenameTable("dialectica_links", "dialectica_renamed_links"),
				new DropTable("dialectica_renamed_links")
		};

		writer.apply(changes);
		writer.apply(changes);

		assertEquals(List.of("unique_id", "username", "last_seen_at", "is_locked"), columns("dialectica_accounts"));
		Map<String, Boolean> indexes = indexes("dialectica_accounts");
		assertEquals(Boolean.TRUE, indexes.get("dialectica_accounts_last_seen_at"));
		assertFalse(indexes.containsKey("dialectica_accounts_username"));
		assertFalse(exists("dialectica_links"));
		assertFalse(exists("dialectica_renamed_links"));
	}

	@Test
	void letsAUniqueIndexHoldManyNulls() throws SQLException {
		Dialectica.on(connection).create(Schema.of(ACCOUNTS, LINKS));
		insertAccount();

		insertLink("first", null);
		insertLink("second", null);
		insertLink("third", "external");

		assertThrows(SQLException.class, () -> insertLink("fourth", "external"));
	}

	@Test
	void fillsOmittedColumnsWithTheirDefaults() throws SQLException {
		Dialectica.on(connection).create(Schema.of(ACCOUNTS, LINKS));
		insertAccount();
		insertLink("first", null);

		try (Statement statement = connection.createStatement();
		     ResultSet row = statement.executeQuery("SELECT is_primary, attempts, note FROM dialectica_links")) {
			assertTrue(row.next());
			assertFalse(row.getBoolean("is_primary"));
			assertEquals(3, row.getInt("attempts"));
			assertEquals("it's a\\b", row.getString("note"));
		}
	}

	/**
	 * Identica's entities create the tables that Identica 2.2.1's hand-written DDL created, as the
	 * database's metadata describes them. SQLite is the exception for primary key columns, which
	 * 2.2.1 left nullable there and the model declares not null, so their nullability is not
	 * compared.
	 */
	@Test
	void createsIdenticaTablesAsVersion221Did() throws SQLException {
		for (String statement : Identica221.statements(identicaDatabaseType()))
			execute(statement);
		Map<String, String> legacy = snapshot(IDENTICA_TABLES);
		assertTrue(legacy.get("identica_provider_links").contains("provider_subject"), legacy.toString());
		assertTrue(legacy.get("identica_provider_links").contains("->identica_accounts.unique_id:cascade"), legacy.toString());
		assertTrue(legacy.get("identica_provider_profiles").contains("provider_subject->identica_provider_links.provider_subject:cascade"), legacy.toString());
		assertTrue(legacy.get("identica_verification_selections").contains("unique_id->identica_accounts.unique_id:cascade"), legacy.toString());
		dropTables();

		SchemaWriter writer = Dialectica.on(connection);
		writer.create(Schema.of(
				Dialectica.entity(ProviderProfile.class).table(),
				Dialectica.entity(ProviderLink.class).table(),
				Dialectica.entity(AccountReservation.class).table(),
				Dialectica.entity(Account.class).table()
		));
		// A feature's schema of its own, referencing the core account table by name.
		writer.create(Schema.of(Dialectica.entity(VerificationSelection.class).table()));

		assertEquals(legacy, snapshot(IDENTICA_TABLES));
	}

	@Test
	void refusesTablesThatReferenceEachOtherInACycle() {
		TableDefinition first = TableDefinition.builder("dialectica_first")
				.column(ColumnDefinition.builder("id", ColumnType.bigint()).notNull().build())
				.primaryKey("id")
				.foreignKey(ForeignKeyDefinition.builder("id").references("dialectica_second", "id").build())
				.build();
		TableDefinition second = TableDefinition.builder("dialectica_second")
				.column(ColumnDefinition.builder("id", ColumnType.bigint()).notNull().build())
				.primaryKey("id")
				.foreignKey(ForeignKeyDefinition.builder("id").references("dialectica_first", "id").build())
				.build();

		assertThrows(IllegalArgumentException.class, () -> Dialectica.on(connection).create(Schema.of(first, second)));
	}

	protected void insertAccount() throws SQLException {
		execute("INSERT INTO dialectica_accounts (unique_id, username) VALUES ('" + ACCOUNT + "', 'Alice')");
	}

	protected void insertLink(String subject, String externalId) throws SQLException {
		String external = externalId == null ? "NULL" : "'" + externalId + "'";
		execute("INSERT INTO dialectica_links (provider_id, provider_subject, unique_id, external_id) VALUES ('"
				+ subject + "', 'subject', '" + ACCOUNT + "', " + external + ")");
	}

	protected void execute(String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private void dropTables() throws SQLException {
		for (String table : TABLES)
			if (exists(table)) execute("DROP TABLE " + table);
	}

	private void assertMatches(TableDefinition table) throws SQLException {
		String name = table.getName();
		DatabaseMetaData metaData = connection.getMetaData();
		String stored = stored(name);

		List<String> columnNames = new ArrayList<>();
		try (ResultSet columns = metaData.getColumns(connection.getCatalog(), connection.getSchema(), stored, "%")) {
			while (columns.next()) {
				String column = lower(columns.getString("COLUMN_NAME"));
				ColumnDefinition declared = table.column(column);
				columnNames.add(column);
				if (declared == null) continue;

				assertEquals(typeName(declared.getType()), columns.getString("TYPE_NAME").toUpperCase(Locale.ROOT), name + "." + column + " type");
				assertEquals(declared.isNullable(), columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable, name + "." + column + " nullability");
				if (declared.getType() instanceof ColumnType.VarcharType varchar)
					assertEquals(varchar.length(), columns.getInt("COLUMN_SIZE"), name + "." + column + " length");
			}
		}
		assertEquals(table.getColumns().stream().map(ColumnDefinition::getName).toList(), columnNames, name + " columns");

		Map<Integer, String> primaryKey = new TreeMap<>();
		try (ResultSet keys = metaData.getPrimaryKeys(connection.getCatalog(), connection.getSchema(), stored)) {
			while (keys.next())
				primaryKey.put(keys.getInt("KEY_SEQ"), lower(keys.getString("COLUMN_NAME")));
		}
		assertEquals(table.getPrimaryKey(), List.copyOf(primaryKey.values()), name + " primary key");

		Map<String, List<String>> indexColumns = indexColumns(stored);
		Map<String, Boolean> indexes = indexes(name);
		for (IndexDefinition index : table.getIndexes()) {
			assertEquals(index.isUnique(), indexes.get(index.getName()), name + " index " + index.getName());
			assertEquals(index.getColumns(), indexColumns.get(index.getName()), name + " index " + index.getName() + " columns");
		}
		for (List<String> uniqueKey : table.getUniqueKeys()) {
			boolean covered = indexColumns.entrySet().stream()
					.anyMatch(entry -> Boolean.TRUE.equals(indexes.get(entry.getKey())) && entry.getValue().equals(uniqueKey));
			assertTrue(covered, name + " unique key " + uniqueKey);
		}

		Set<String> foreignKeys = new HashSet<>();
		try (ResultSet imported = metaData.getImportedKeys(connection.getCatalog(), connection.getSchema(), stored)) {
			while (imported.next())
				foreignKeys.add(lower(imported.getString("FKCOLUMN_NAME")) + "->" + lower(imported.getString("PKTABLE_NAME"))
						+ "." + lower(imported.getString("PKCOLUMN_NAME")) + ":" + deleteRule(imported.getShort("DELETE_RULE")));
		}
		Set<String> expected = new HashSet<>();
		for (ForeignKeyDefinition foreignKey : table.getForeignKeys())
			for (int index = 0; index < foreignKey.getColumns().size(); index++)
				expected.add(foreignKey.getColumns().get(index) + "->" + foreignKey.getReferencedTable() + "."
						+ foreignKey.getReferencedColumns().get(index) + ":" + deleteRule(foreignKey.getOnDelete()));
		assertEquals(expected, foreignKeys, name + " foreign keys");
	}

	private Map<String, String> snapshot(List<String> tables) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		Map<String, String> snapshot = new TreeMap<>();
		for (String table : tables) {
			String stored = stored(table);
			List<String> key = new ArrayList<>();
			try (ResultSet keys = metaData.getPrimaryKeys(connection.getCatalog(), connection.getSchema(), stored)) {
				Map<Integer, String> ordered = new TreeMap<>();
				while (keys.next())
					ordered.put(keys.getInt("KEY_SEQ"), lower(keys.getString("COLUMN_NAME")));
				key.addAll(ordered.values());
			}

			List<String> columns = new ArrayList<>();
			try (ResultSet rows = metaData.getColumns(connection.getCatalog(), connection.getSchema(), stored, "%")) {
				while (rows.next()) {
					String column = lower(rows.getString("COLUMN_NAME"));
					String nullability = key.contains(column) ? "key" : String.valueOf(rows.getInt("NULLABLE"));
					columns.add(column + " " + rows.getString("TYPE_NAME").toUpperCase(Locale.ROOT) + "(" + rows.getInt("COLUMN_SIZE") + ") " + nullability);
				}
			}

			Map<String, Boolean> uniqueness = indexes(table);
			Set<String> uniqueIndexes = new TreeSet<>();
			indexColumns(stored).forEach((name, indexed) -> uniqueIndexes.add((Boolean.TRUE.equals(uniqueness.get(name)) ? "unique " : "") + indexed));

			Set<String> foreignKeys = new TreeSet<>();
			try (ResultSet imported = metaData.getImportedKeys(connection.getCatalog(), connection.getSchema(), stored)) {
				while (imported.next())
					foreignKeys.add(lower(imported.getString("FKCOLUMN_NAME")) + "->" + lower(imported.getString("PKTABLE_NAME"))
							+ "." + lower(imported.getString("PKCOLUMN_NAME")) + ":" + deleteRule(imported.getShort("DELETE_RULE")));
			}

			snapshot.put(table, "columns " + columns + "; key " + key + "; indexes " + uniqueIndexes + "; foreign keys " + foreignKeys);
		}

		return snapshot;
	}

	private static String deleteRule(short rule) {
		return switch (rule) {
			case DatabaseMetaData.importedKeyCascade -> "cascade";
			case DatabaseMetaData.importedKeySetNull -> "set null";
			case DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyNoAction -> "refuse";
			default -> "rule " + rule;
		};
	}

	private static String deleteRule(ReferentialAction action) {
		return switch (action) {
			case CASCADE -> "cascade";
			case SET_NULL -> "set null";
			case RESTRICT, NO_ACTION -> "refuse";
		};
	}

	private List<String> columns(String table) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), stored(table), "%")) {
			while (columns.next())
				names.add(lower(columns.getString("COLUMN_NAME")));
		}

		return names;
	}

	/**
	 * Returns whether each index of a table is unique, by lowercase name.
	 */
	private Map<String, Boolean> indexes(String table) throws SQLException {
		Map<String, Boolean> indexes = new HashMap<>();
		try (ResultSet rows = connection.getMetaData().getIndexInfo(connection.getCatalog(), connection.getSchema(), stored(table), false, false)) {
			while (rows.next())
				if (rows.getString("INDEX_NAME") != null)
					indexes.put(lower(rows.getString("INDEX_NAME")), !rows.getBoolean("NON_UNIQUE"));
		}

		return indexes;
	}

	private Map<String, List<String>> indexColumns(String stored) throws SQLException {
		Map<String, Map<Integer, String>> ordered = new HashMap<>();
		try (ResultSet rows = connection.getMetaData().getIndexInfo(connection.getCatalog(), connection.getSchema(), stored, false, false)) {
			while (rows.next())
				if (rows.getString("INDEX_NAME") != null && rows.getString("COLUMN_NAME") != null)
					ordered.computeIfAbsent(lower(rows.getString("INDEX_NAME")), name -> new TreeMap<>())
							.put((int) rows.getShort("ORDINAL_POSITION"), lower(rows.getString("COLUMN_NAME")));
		}

		Map<String, List<String>> columns = new HashMap<>();
		ordered.forEach((name, positions) -> columns.put(name, List.copyOf(positions.values())));

		return columns;
	}

	private boolean exists(String table) throws SQLException {
		try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), stored(table), null)) {
			return tables.next();
		}
	}

	private String stored(String name) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		if (metaData.storesUpperCaseIdentifiers()) return name.toUpperCase(Locale.ROOT);

		return name;
	}

	private static String lower(String name) {
		return name == null ? null : name.toLowerCase(Locale.ROOT);
	}
}
