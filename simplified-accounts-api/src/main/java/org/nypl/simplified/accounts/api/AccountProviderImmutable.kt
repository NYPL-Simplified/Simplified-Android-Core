package org.nypl.simplified.accounts.api

import org.joda.time.DateTime

import java.net.URI
import javax.annotation.concurrent.ThreadSafe

/**
 * An immutable account provider.
 */

@ThreadSafe
data class AccountProviderImmutable(
  override val addAutomatically: Boolean,
  override val annotationsURI: URI?,
  override val authentication: AccountProviderAuthenticationDescription?,
  override val authenticationDocumentURI: URI?,
  override val cardCreatorURI: URI?,
  override val catalogURI: URI,
  override val displayName: String,
  override val eula: URI?,
  override val id: URI,
  override val idNumeric: Int = -1,
  override val isProduction: Boolean,
  override val license: URI?,
  override val loansURI: URI?,
  override val logo: URI?,
  override val mainColor: String,
  override val patronSettingsURI: URI?,
  override val privacyPolicy: URI?,
  override val subtitle: String?,
  override val supportEmail: String?,
  override val supportsReservations: Boolean,
  override val supportsSimplyESynchronization: Boolean,
  override val updated: DateTime
) : AccountProviderType {

  override fun toDescription(): AccountProviderDescriptionType {
    val imageLinks = mutableListOf<AccountProviderDescriptionMetadata.Link>()
    this.logo?.let { uri ->
      imageLinks.add(AccountProviderDescriptionMetadata.Link(
        href = uri,
        type = null,
        templated = false,
        relation = "http://opds-spec.org/image/thumbnail"))
    }

    // XXX: TODO: Reconstruct links from above fields
    val links =
      mutableListOf<AccountProviderDescriptionMetadata.Link>()

    val meta =
      AccountProviderDescriptionMetadata(
        id = this@AccountProviderImmutable.id,
        title = this@AccountProviderImmutable.displayName,
        updated = this@AccountProviderImmutable.updated,
        links = links.toList(),
        images = imageLinks.toList(),
        isAutomatic = this@AccountProviderImmutable.addAutomatically,
        isProduction = this@AccountProviderImmutable.isProduction)

    return object : AccountProviderDescriptionType {
      override val metadata: AccountProviderDescriptionMetadata = meta

      override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
        onProgress.invoke(this.metadata.id, "")
        return AccountProviderResolutionResult(this@AccountProviderImmutable, listOf())
      }
    }
  }
}
