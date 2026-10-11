package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaPlugin;
import me.whereareiam.dialectica.fixture.Account;
import me.whereareiam.dialectica.fixture.AccountReservation;
import me.whereareiam.dialectica.fixture.ProviderLink;
import me.whereareiam.dialectica.schema.Schema;
import me.whereareiam.dialectica.schema.change.DropTable;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link EntityAccess}, {@link DialecticaPlugin} and {@link RegisterDialecticaMapper}
 * guarantee on every dialect, with Identica's tables. Each dialect runs it against a database of its
 * own.
 */
abstract class EntityAccessContract {
	private static final UUID ALICE = UUID.fromString("6f1c2a52-8e1b-4a8e-9a43-3b1f7f0c2d11");
	private static final UUID BOB = UUID.fromString("0b6a3f4e-2c5d-4e7f-8a9b-1c2d3e4f5a6b");

	protected Jdbi jdbi;
	private EntityAccess<Account> accounts;
	private EntityAccess<ProviderLink> links;
	private EntityAccess<AccountReservation> reservations;

	/**
	 * Creates a Jdbi for an empty or reusable database of the dialect under test.
	 */
	protected abstract Jdbi createJdbi();

	@BeforeEach
	void prepare() throws SQLException {
		jdbi = createJdbi();
		jdbi.installPlugin(new SqlObjectPlugin());
		jdbi.installPlugin(new DialecticaPlugin());

		jdbi.useHandle(handle -> {
			var writer = Dialectica.on(handle.getConnection());
			writer.apply(
					new DropTable("identica_provider_links"),
					new DropTable("identica_account_reservations"),
					new DropTable("identica_accounts")
			);
			writer.create(Schema.of(
					Dialectica.entity(Account.class).table(),
					Dialectica.entity(ProviderLink.class).table(),
					Dialectica.entity(AccountReservation.class).table()
			));
		});

		accounts = EntityAccess.of(jdbi, Dialectica.entity(Account.class));
		links = EntityAccess.of(jdbi, Dialectica.entity(ProviderLink.class));
		reservations = EntityAccess.of(jdbi, Dialectica.entity(AccountReservation.class));
	}

	@Test
	void insertsAndFindsByAUuidKey() {
		Account alice = new Account(ALICE, "Alice", 1_700_000_000_000L, null);

		accounts.insert(alice);

		assertEquals(Optional.of(alice), accounts.find(ALICE));
		assertEquals(Optional.empty(), accounts.find(BOB));
	}

	@Test
	void findsByACompositeKeyWithBooleans() {
		accounts.insert(new Account(ALICE, "Alice", null, null));
		ProviderLink primary = new ProviderLink("premium", "alice-subject", ALICE, true, 10L, null);
		ProviderLink secondary = new ProviderLink("credential", "alice", ALICE, false, null, 20L);

		links.insert(primary);
		links.insert(secondary);

		assertEquals(Optional.of(primary), links.find("premium", "alice-subject"));
		assertEquals(Optional.of(secondary), links.find("credential", "alice"));
	}

	@Test
	void upsertsTheSameKeyTwice() {
		reservations.upsert(new AccountReservation("name:alice", ALICE, 1L, 2L));
		reservations.upsert(new AccountReservation("name:alice", BOB, 3L, 4L));

		assertEquals(Optional.of(new AccountReservation("name:alice", BOB, 3L, 4L)), reservations.find("name:alice"));
		long rows = jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM identica_account_reservations")
				.mapTo(Long.class)
				.one());
		assertEquals(1, rows);
	}

	@Test
	void updatesTheRowWithTheEntitysKey() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));

		assertTrue(accounts.update(new Account(ALICE, "Alicia", 1L, 5L)));
		assertFalse(accounts.update(new Account(BOB, "Bob", 1L, 5L)));

		assertEquals(Optional.of(new Account(ALICE, "Alicia", 1L, 5L)), accounts.find(ALICE));
		assertEquals(Optional.empty(), accounts.find(BOB));
	}

	@Test
	void deletesTheRowWithAKey() {
		reservations.insert(new AccountReservation("name:alice", ALICE, 1L, 2L));

		assertTrue(reservations.delete("name:alice"));
		assertFalse(reservations.delete("name:alice"));

		assertEquals(Optional.empty(), reservations.find("name:alice"));
	}

	@Test
	void mapsHandWrittenQueriesByColumnName() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));
		accounts.insert(new Account(BOB, "Bob", 2L, 3L));
		links.insert(new ProviderLink("premium", "alice-subject", ALICE, true, 10L, null));
		links.insert(new ProviderLink("credential", "alice", ALICE, false, null, null));

		Queries queries = jdbi.onDemand(Queries.class);

		assertEquals(Optional.of(new Account(BOB, "Bob", 2L, 3L)), queries.account(BOB));
		assertEquals(List.of(new ProviderLink("premium", "alice-subject", ALICE, true, 10L, null)), queries.links(ALICE, true));
		assertEquals(List.of(ALICE, BOB), queries.uniqueIds());
		assertEquals(List.of(false, true), queries.primaries());
	}

	@RegisterDialecticaMapper(Account.class)
	@RegisterDialecticaMapper(ProviderLink.class)
	public interface Queries {
		@SqlQuery("SELECT * FROM identica_accounts WHERE unique_id = :uniqueId")
		Optional<Account> account(@Bind("uniqueId") UUID uniqueId);

		@SqlQuery("SELECT * FROM identica_provider_links WHERE unique_id = :uniqueId AND is_primary = :primary")
		List<ProviderLink> links(@Bind("uniqueId") UUID uniqueId, @Bind("primary") boolean primary);

		@SqlQuery("SELECT unique_id FROM identica_accounts ORDER BY username")
		List<UUID> uniqueIds();

		@SqlQuery("SELECT is_primary FROM identica_provider_links ORDER BY provider_id")
		List<Boolean> primaries();
	}
}
