package me.whereareiam.dialectica.fixture;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.dialectica.entity.annotation.Column;
import me.whereareiam.dialectica.entity.annotation.Id;
import me.whereareiam.dialectica.entity.annotation.Length;
import me.whereareiam.dialectica.entity.annotation.Nullable;
import me.whereareiam.dialectica.entity.annotation.References;
import me.whereareiam.dialectica.entity.annotation.Table;
import me.whereareiam.dialectica.entity.annotation.Unique;
import me.whereareiam.dialectica.schema.type.ReferentialAction;

import java.util.UUID;

/**
 * Identica's link of an account to a provider subject, as a Lombok bean with a composite key.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table("identica_provider_links")
@Unique({"uniqueId", "providerId"})
public class ProviderLink {
	@Id
	@Length(64)
	private String providerId;

	@Id
	@Length(128)
	private String providerSubject;

	@References(entity = Account.class, onDelete = ReferentialAction.CASCADE)
	private UUID uniqueId;

	@Column("is_primary")
	private boolean primary;

	@Nullable
	private Long linkedAt;

	@Nullable
	private Long lastSeenAt;
}
