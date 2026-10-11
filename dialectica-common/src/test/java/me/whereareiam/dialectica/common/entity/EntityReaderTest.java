package me.whereareiam.dialectica.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.entity.EntityTable;
import me.whereareiam.dialectica.entity.Row;
import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Index;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Nullable;
import me.whereareiam.dialectica.entity.annotation.References;
import me.whereareiam.dialectica.entity.annotation.Table;
import me.whereareiam.dialectica.entity.annotation.Unique;
import me.whereareiam.dialectica.schema.ForeignKey;
import me.whereareiam.dialectica.schema.type.ColumnType;
import me.whereareiam.dialectica.schema.type.ReferentialAction;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityReaderTest {
	private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Table("identica_accounts")
	@Index(name = "identica_accounts_username", columns = "username")
	record Account(@Id UUID uniqueId, @Length(64) String username, @Nullable Long createdAt, @Nullable Long lastSeenAt) {
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Table("identica_provider_links")
	@Unique({"uniqueId", "providerId"})
	@Index(name = "identica_provider_links_primary", columns = {"uniqueId", "primary"}, unique = true)
	static class ProviderLink {
		@Id
		@Length(64)
		private String providerId;
		@Id
		@Length(128)
		private String providerSubject;
		@References(entity = Account.class, onDelete = ReferentialAction.CASCADE)
		private UUID uniqueId;
		@me.whereareiam.dialectica.entity.annotation.Column("is_primary")
		private boolean primary;
		@Nullable
		private Long linkedAt;
		private transient String cached;
		private static final String IGNORED = "ignored";
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@Table("identica_settings")
	static class Setting {
		@Id
		@Length(32)
		private String name;
		@Nullable
		private String value;
		private int revision;
	}

	@Value
	@Table("identica_sessions")
	static class Session {
		@Id
		long id;
		@References(entity = Session.class)
		@Jakarta.Nullable
		Long parentId;
		String token;
	}

	@Test
	void readsARecord() {
		EntityTable<Account> table = EntityReader.read(Account.class);

		assertEquals("identica_accounts", table.table().getName());
		assertEquals(List.of("unique_id", "username", "created_at", "last_seen_at"), names(table));
		assertEquals(List.of("unique_id"), table.table().getPrimaryKey());
		assertEquals(ColumnType.uuid(), table.column("unique_id").column().getType());
		assertEquals(ColumnType.varchar(64), table.column("username").column().getType());
		assertFalse(table.column("username").column().isNullable());
		assertTrue(table.column("created_at").column().isNullable());
		assertEquals("identica_accounts_username", table.table().getIndexes().get(0).getName());

		Account account = new Account(ACCOUNT, "Alice", 1L, null);
		assertEquals("Alice", table.column("username").get(account));
		assertEquals(account, table.create(row(table, Map.of("unique_id", ACCOUNT, "username", "Alice", "created_at", 1L))));
	}

	@Test
	void readsALombokClassWithACompositeKey() {
		EntityTable<ProviderLink> table = EntityReader.read(ProviderLink.class);

		assertEquals(List.of("provider_id", "provider_subject", "unique_id", "is_primary", "linked_at"), names(table));
		assertEquals(List.of("provider_id", "provider_subject"), table.table().getPrimaryKey());
		assertEquals(2, table.primaryKey().size());
		assertEquals(ColumnType.bool(), table.column("is_primary").column().getType());
		assertFalse(table.column("is_primary").column().isNullable());
		assertEquals(List.of(List.of("unique_id", "provider_id")), table.table().getUniqueKeys());
		assertEquals(List.of("unique_id", "is_primary"), table.table().getIndexes().get(0).getColumns());
		assertTrue(table.table().getIndexes().get(0).isUnique());

		ForeignKey foreignKey = table.table().getForeignKeys().get(0);
		assertEquals("identica_accounts", foreignKey.getReferencedTable());
		assertEquals(List.of("unique_id"), foreignKey.getReferencedColumns());
		assertEquals(ReferentialAction.CASCADE, foreignKey.getOnDelete());

		ProviderLink link = table.create(row(table, Map.of(
				"provider_id", "premium", "provider_subject", "subject", "unique_id", ACCOUNT, "is_primary", true)));
		assertEquals("premium", link.getProviderId());
		assertEquals(ACCOUNT, link.getUniqueId());
		assertTrue(link.isPrimary());
		assertEquals(true, table.column("is_primary").get(link));
	}

	@Test
	void buildsAClassWithoutAllColumnsConstructorThroughItsSetters() {
		EntityTable<Setting> table = EntityReader.read(Setting.class);

		Setting setting = table.create(row(table, Map.of("name", "motd", "revision", 3)));

		assertEquals("motd", setting.getName());
		assertEquals(3, setting.getRevision());
		assertEquals(ColumnType.text(), table.column("value").column().getType());
		assertEquals(ColumnType.integer(), table.column("revision").column().getType());
	}

	@Test
	void readsAValueClassThatReferencesItself() {
		EntityTable<Session> table = EntityReader.read(Session.class);

		assertTrue(table.column("parent_id").column().isNullable());
		assertEquals("identica_sessions", table.table().getForeignKeys().get(0).getReferencedTable());
		assertEquals(ReferentialAction.NO_ACTION, table.table().getForeignKeys().get(0).getOnDelete());
		assertEquals(new Session(7, null, "token"), table.create(row(table, Map.of("id", 7L, "token", "token"))));
	}

	@Table("broken")
	record UnsupportedType(@Id UUID id, double score) {
	}

	@Table("broken")
	record NullablePrimitive(@Id UUID id, @Nullable long score) {
	}

	@Table("broken")
	record LengthOnNumber(@Id UUID id, @Length(4) long score) {
	}

	@Table("broken")
	record NullableKey(@Id @Nullable UUID id) {
	}

	@Table("broken")
	@Unique("missing")
	record UnknownUniqueField(@Id UUID id) {
	}

	@Table("broken")
	record ReferenceToCompositeKey(@Id UUID id, @References(entity = ProviderLink.class) @Length(64) String link) {
	}

	record NotAnnotated(UUID id) {
	}

	@Getter
	@Table("broken")
	static class NoUsableConstructor {
		@Id
		private final UUID id;
		private final String name;

		NoUsableConstructor(String name) {
			this.id = UUID.randomUUID();
			this.name = name;
		}
	}

	@Test
	void refusesInvalidEntitiesWhenReadingThem() {
		assertMessage(UnsupportedType.class, "score", "has no column type");
		assertMessage(NullablePrimitive.class, "score", "primitive field cannot be nullable");
		assertMessage(LengthOnNumber.class, "score", "@Length applies to String fields only");
		assertMessage(NullableKey.class, "id", "key field cannot be nullable");
		assertMessage(UnknownUniqueField.class, "missing", "no column field");
		assertMessage(ReferenceToCompositeKey.class, "ProviderLink", "exactly one @Id field");
		assertMessage(NotAnnotated.class, "NotAnnotated", "not annotated with @Table");
		assertMessage(NoUsableConstructor.class, "NoUsableConstructor", "needs a constructor");
	}

	private static void assertMessage(Class<?> type, String subject, String reason) {
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> EntityReader.read(type));

		assertTrue(failure.getMessage().contains(subject) && failure.getMessage().contains(reason), failure.getMessage());
	}

	private static <T> List<String> names(EntityTable<T> table) {
		return table.columns().stream().map(EntityColumn::name).toList();
	}

	private static <T> Row<T> row(EntityTable<T> table, Map<String, Object> values) {
		return new Row<>() {
			@Override
			@SuppressWarnings("unchecked")
			public <V> V get(EntityColumn<T, V> column) {
				return (V) values.get(column.name());
			}
		};
	}

	/**
	 * Stands in for a runtime-visible {@code Nullable} of another library, such as Jakarta's.
	 */
	static final class Jakarta {
		@Retention(RetentionPolicy.RUNTIME)
		@interface Nullable {
		}
	}
}
