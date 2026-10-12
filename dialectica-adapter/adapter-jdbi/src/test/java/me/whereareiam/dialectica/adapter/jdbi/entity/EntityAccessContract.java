package me.whereareiam.dialectica.adapter.jdbi.entity;

import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.adapter.jdbi.DialecticaPlugin;
import me.whereareiam.dialectica.fixture.Account;
import me.whereareiam.dialectica.fixture.AccountReservation;
import me.whereareiam.dialectica.fixture.ProviderLink;
import me.whereareiam.dialectica.entity.EntityColumn;
import me.whereareiam.dialectica.schema.Schema;
import me.whereareiam.dialectica.schema.change.DropTable;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	void replacesTheRowWithTheEntitysKey() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));

		assertTrue(accounts.replace(new Account(ALICE, "Alicia", 1L, 5L)));
		assertFalse(accounts.replace(new Account(BOB, "Bob", 1L, 5L)));

		assertEquals(Optional.of(new Account(ALICE, "Alicia", 1L, 5L)), accounts.find(ALICE));
		assertEquals(Optional.empty(), accounts.find(BOB));
	}

	@Test
	void updatesOnlyTheColumnsItSets() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));

		assertTrue(accounts.update(ALICE).set("last_seen_at", 5L).execute());
		assertTrue(accounts.update(ALICE).set("username", "Alicia").set("created_at", null).execute());
		assertFalse(accounts.update(BOB).set("last_seen_at", 5L).execute());

		assertEquals(Optional.of(new Account(ALICE, "Alicia", null, 5L)), accounts.find(ALICE));
		assertEquals(Optional.empty(), accounts.find(BOB));
	}

	@Test
	@SuppressWarnings("unchecked")
	void updatesByACompositeKeyThroughTypedColumns() {
		accounts.insert(new Account(ALICE, "Alice", null, null));
		links.insert(new ProviderLink("premium", "alice-subject", ALICE, true, 10L, null));
		links.insert(new ProviderLink("credential", "alice", ALICE, false, null, null));
		EntityColumn<ProviderLink, Boolean> primary = (EntityColumn<ProviderLink, Boolean>) links.table().column("is_primary");
		EntityColumn<ProviderLink, Long> lastSeenAt = (EntityColumn<ProviderLink, Long>) links.table().column("last_seen_at");

		assertTrue(links.update("credential", "alice").set(primary, true).set(lastSeenAt, 30L).when(primary, false).execute());
		assertTrue(links.update("premium", "alice-subject").set(primary, false).execute());

		assertEquals(Optional.of(new ProviderLink("credential", "alice", ALICE, true, null, 30L)), links.find("credential", "alice"));
		assertEquals(Optional.of(new ProviderLink("premium", "alice-subject", ALICE, false, 10L, null)), links.find("premium", "alice-subject"));
	}

	@Test
	void updatesOnlyWhileTheConditionsHold() {
		reservations.insert(new AccountReservation("recovery:alice", ALICE, 1L, 0L));

		assertTrue(reservations.update("recovery:alice").set("expires_at", 7L).when("expires_at", 0L).execute());
		assertFalse(reservations.update("recovery:alice").set("expires_at", 8L).when("expires_at", 0L).execute());
		assertFalse(reservations.update("recovery:alice").set("expires_at", 9L).when("expires_at", 7L).when("unique_id", BOB).execute());

		assertEquals(Optional.of(new AccountReservation("recovery:alice", ALICE, 1L, 7L)), reservations.find("recovery:alice"));
	}

	@Test
	void comparesWithNullAsIsNull() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));

		assertTrue(accounts.update(ALICE).set("last_seen_at", 5L).when("last_seen_at", null).execute());
		assertFalse(accounts.update(ALICE).set("last_seen_at", 6L).when("last_seen_at", null).execute());

		assertEquals(Optional.of(new Account(ALICE, "Alice", 1L, 5L)), accounts.find(ALICE));
	}

	@Test
	void letsExactlyOneOfTwoConcurrentConditionalUpdatesWin() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			for (int round = 0; round < 20; round++) {
				reservations.upsert(new AccountReservation("recovery:alice", ALICE, 1L, 0L));
				CyclicBarrier start = new CyclicBarrier(2);
				List<Future<Boolean>> updates = new ArrayList<>();
				for (long contender = 1; contender <= 2; contender++) {
					long usedAt = contender;
					updates.add(executor.submit(() -> {
						start.await(10, TimeUnit.SECONDS);
						return reservations.update("recovery:alice").set("expires_at", usedAt).when("expires_at", 0L).execute();
					}));
				}

				boolean first = updates.get(0).get(30, TimeUnit.SECONDS);
				boolean second = updates.get(1).get(30, TimeUnit.SECONDS);

				assertTrue(first ^ second, "round " + round + ": first " + first + ", second " + second);
				assertEquals(first ? 1L : 2L, reservations.find("recovery:alice").orElseThrow().expiresAt());
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void updatesOnlyWhileAnOrderedConditionHolds() {
		reservations.insert(new AccountReservation("totp:alice", ALICE, 1L, 0L));

		assertTrue(reservations.update("totp:alice").set("expires_at", 5L).whenBelow("expires_at", 5L).execute());
		assertFalse(reservations.update("totp:alice").set("expires_at", 5L).whenBelow("expires_at", 5L).execute());
		assertFalse(reservations.update("totp:alice").set("expires_at", 4L).whenBelow("expires_at", 4L).execute());
		assertTrue(reservations.update("totp:alice").set("expires_at", 6L).whenBelow("expires_at", 6L).execute());

		assertEquals(Optional.of(new AccountReservation("totp:alice", ALICE, 1L, 6L)), reservations.find("totp:alice"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void comparesAboveAtMostAndAtLeastThroughNamesAndTypedColumns() {
		reservations.insert(new AccountReservation("totp:alice", ALICE, 1L, 5L));
		EntityColumn<AccountReservation, Long> expiresAt = (EntityColumn<AccountReservation, Long>) reservations.table().column("expires_at");

		assertFalse(reservations.update("totp:alice").set("created_at", 2L).whenAbove("expires_at", 5L).execute());
		assertTrue(reservations.update("totp:alice").set("created_at", 2L).whenAbove(expiresAt, 4L).execute());
		assertFalse(reservations.update("totp:alice").set("created_at", 3L).whenAtMost("expires_at", 4L).execute());
		assertTrue(reservations.update("totp:alice").set("created_at", 3L).whenAtMost(expiresAt, 5L).execute());
		assertFalse(reservations.update("totp:alice").set("created_at", 4L).whenAtLeast("expires_at", 6L).execute());
		assertTrue(reservations.update("totp:alice").set("created_at", 4L).whenAtLeast(expiresAt, 5L).execute());
		assertFalse(reservations.update("totp:alice").set("created_at", 5L).whenAtLeast("expires_at", 1L).whenBelow("expires_at", 5L).execute());
		assertTrue(reservations.update("totp:alice").set("created_at", 5L).whenAtLeast("expires_at", 1L).whenBelow(expiresAt, 6L).execute());

		assertEquals(Optional.of(new AccountReservation("totp:alice", ALICE, 5L, 5L)), reservations.find("totp:alice"));
	}

	@Test
	void meetsNoOrderedConditionWhileTheColumnIsNull() {
		accounts.insert(new Account(ALICE, "Alice", 1L, null));

		assertFalse(accounts.update(ALICE).set("last_seen_at", 5L).whenBelow("last_seen_at", 5L).execute());
		assertFalse(accounts.update(ALICE).set("last_seen_at", 5L).whenAtLeast("last_seen_at", Long.MIN_VALUE).execute());

		assertEquals(Optional.of(new Account(ALICE, "Alice", 1L, null)), accounts.find(ALICE));
	}

	@Test
	void letsExactlyOneOfTwoConcurrentOrderedUpdatesWin() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			for (int round = 0; round < 20; round++) {
				reservations.upsert(new AccountReservation("totp:alice", ALICE, 0L, 4L));
				CyclicBarrier start = new CyclicBarrier(2);
				List<Future<Boolean>> updates = new ArrayList<>();
				for (long contender = 1; contender <= 2; contender++) {
					long winner = contender;
					updates.add(executor.submit(() -> {
						start.await(10, TimeUnit.SECONDS);
						return reservations.update("totp:alice").set("expires_at", 5L).set("created_at", winner).whenBelow("expires_at", 5L).execute();
					}));
				}

				boolean first = updates.get(0).get(30, TimeUnit.SECONDS);
				boolean second = updates.get(1).get(30, TimeUnit.SECONDS);

				assertTrue(first ^ second, "round " + round + ": first " + first + ", second " + second);
				assertEquals(new AccountReservation("totp:alice", ALICE, first ? 1L : 2L, 5L), reservations.find("totp:alice").orElseThrow());
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void refusesUpdatesThatDoNotFitTheTable() {
		@SuppressWarnings({"unchecked", "rawtypes"})
		EntityColumn<Account, Boolean> foreign = (EntityColumn) links.table().column("is_primary");

		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE, BOB));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).set("missing", 1L));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).set("unique_id", BOB));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).set("username", 5L));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).set("username", null));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).set("last_seen_at", 1L).set("last_seen_at", 2L));
		assertThrows(IllegalArgumentException.class, () -> reservations.update("recovery:alice").when("expires_at", 0));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).when("last_seen_at", 1L).when("last_seen_at", 2L));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).when(foreign, true));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).when("last_seen_at", 1L).when("last_seen_at", null));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).whenBelow("username", 1L));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).whenBelow("last_seen_at", 1));
		assertThrows(IllegalArgumentException.class, () -> accounts.update(ALICE).whenBelow("last_seen_at", 1L).whenBelow("last_seen_at", 2L));
		assertThrows(IllegalArgumentException.class, () -> reservations.update("recovery:alice").whenAtLeast("unique_id", 1L));
		assertThrows(IllegalStateException.class, () -> accounts.update(ALICE).when("last_seen_at", null).execute());
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
