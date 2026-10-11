package me.whereareiam.dialectica.fixture;

import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Table;

import java.util.UUID;

/**
 * Identica's username reservation, which Identica writes with an upsert.
 */
@Table("identica_account_reservations")
public record AccountReservation(
		@Id @Length(192) String reservationKey,
		UUID uniqueId,
		long createdAt,
		long expiresAt
) {
}
