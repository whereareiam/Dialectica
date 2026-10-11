package me.whereareiam.dialectica.fixture;

import me.whereareiam.dialectica.entity.annotation.ForeignKey;
import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Table;
import me.whereareiam.dialectica.schema.type.ReferentialAction;

/**
 * Identica's profile of a provider subject, whose foreign key references the composite key of
 * {@link ProviderLink}.
 */
@Table("identica_provider_profiles")
@ForeignKey(fields = {"providerId", "providerSubject"}, entity = ProviderLink.class, onDelete = ReferentialAction.CASCADE)
public record ProviderProfile(
		@Id @Length(64) String providerId,
		@Id @Length(128) String providerSubject,
		@Length(64) String providerUsername
) {
}
