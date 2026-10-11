package me.whereareiam.dialectica.fixture;

import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Nullable;
import me.whereareiam.dialectica.entity.annotation.References;
import me.whereareiam.dialectica.entity.annotation.Table;
import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.util.UUID;

/**
 * Identica's verification method selected for a provider, which a feature declares apart from
 * the core and therefore references the account table by name.
 */
@Table("identica_verification_selections")
public record VerificationSelection(
		@Id @References(table = "identica_accounts", column = "unique_id", onDelete = ReferentialAction.CASCADE) UUID uniqueId,
		@Id @Length(64) String providerId,
		@Length(64) String methodId,
		@Nullable Long selectedAt
) {
}
