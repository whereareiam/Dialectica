package me.whereareiam.dialectica.fixture;

import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Nullable;
import me.whereareiam.dialectica.entity.annotation.Table;

import java.util.UUID;

/**
 * Identica's account, as an annotated record.
 */
@Table("identica_accounts")
public record Account(
		@Id UUID uniqueId,
		@Length(64) String username,
		@Nullable Long createdAt,
		@Nullable Long lastSeenAt
) {
}
