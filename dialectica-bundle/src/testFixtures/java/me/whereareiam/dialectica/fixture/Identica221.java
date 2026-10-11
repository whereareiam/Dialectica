package me.whereareiam.dialectica.fixture;

import java.util.List;

/**
 * The DDL Identica 2.2.1 wrote by hand for the tables of {@link Account}, {@link ProviderLink},
 * {@link ProviderProfile}, {@link AccountReservation} and {@link VerificationSelection}, copied from
 * its entities.
 */
public final class Identica221 {
	private Identica221() {
	}

	/**
	 * Returns the statements, in creation order.
	 *
	 * @param databaseType Identica's database type: {@code SQLITE}, {@code H2}, {@code POSTGRES}, or
	 *                     {@code MYSQL} for MySQL and MariaDB
	 */
	public static List<String> statements(String databaseType) {
		String uuidType = switch (databaseType) {
			case "POSTGRES", "H2" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};
		String timeType = "SQLITE".equals(databaseType) ? "INTEGER" : "BIGINT";
		String booleanType = "SQLITE".equals(databaseType) ? "INTEGER" : "BOOLEAN";

		return List.of(
				"""
						CREATE TABLE IF NOT EXISTS identica_accounts (
							unique_id %s PRIMARY KEY,
							username VARCHAR(64) NOT NULL,
							created_at %s,
							last_seen_at %s
						)
						""".formatted(uuidType, timeType, timeType),
				"""
						CREATE TABLE IF NOT EXISTS identica_provider_links (
							provider_id VARCHAR(64) NOT NULL,
							provider_subject VARCHAR(128) NOT NULL,
							unique_id %s NOT NULL,
							is_primary %s NOT NULL,
							linked_at %s,
							last_seen_at %s,
							PRIMARY KEY (provider_id, provider_subject),
							UNIQUE (unique_id, provider_id),
							FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
						)
						""".formatted(uuidType, booleanType, timeType, timeType),
				"""
						CREATE TABLE IF NOT EXISTS identica_provider_profiles (
							provider_id VARCHAR(64) NOT NULL,
							provider_subject VARCHAR(128) NOT NULL,
							provider_username VARCHAR(64) NOT NULL,
							PRIMARY KEY (provider_id, provider_subject),
							FOREIGN KEY (provider_id, provider_subject) REFERENCES identica_provider_links(provider_id, provider_subject) ON DELETE CASCADE
						)
						""",
				"""
						CREATE TABLE IF NOT EXISTS identica_account_reservations (
							reservation_key VARCHAR(192) PRIMARY KEY,
							unique_id %s NOT NULL,
							created_at %s NOT NULL,
							expires_at %s NOT NULL
						)
						""".formatted(uuidType, timeType, timeType),
				"""
						CREATE TABLE IF NOT EXISTS identica_verification_selections (
							unique_id %s NOT NULL,
							provider_id VARCHAR(64) NOT NULL,
							method_id VARCHAR(64) NOT NULL,
							selected_at %s,
							PRIMARY KEY (unique_id, provider_id),
							FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
						)
						""".formatted(uuidType, timeType)
		);
	}
}
