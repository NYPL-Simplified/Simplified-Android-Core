package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType.Link
import java.net.URI
import javax.annotation.concurrent.ThreadSafe

/**
 * An immutable account provider.
 */

@ThreadSafe
data class AccountProviderImmutable(
  override val id: URI,
  override val isProduction: Boolean,
  override val displayName: String,
  override val subtitle: String?,
  override val logo: URI?,
  override val authentication: AccountProviderAuthenticationDescription?,
  override val supportsSimplyESynchronization: Boolean,
  override val supportsBarcodeScanner: Boolean,
  override val supportsBarcodeDisplay: Boolean,
  override val supportsReservations: Boolean,
  override val supportsCardCreator: Boolean,
  override val supportsHelpCenter: Boolean,
  override val authenticationDocumentURI: URI?,
  override val catalogURI: URI,
  override val catalogURIForOver13s: URI?,
  override val catalogURIForUnder13s: URI?,
  override val supportEmail: String?,
  override val eula: URI?,
  override val license: URI?,
  override val privacyPolicy: URI?,
  override val mainColor: String,
  override val styleNameOverride: String?,
  override val addAutomatically: Boolean,
  override val patronSettingsURI: URI?,
  override val annotationsURI: URI?,
  override val updated: DateTime
) : AccountProviderType {

  override fun toDescription(): AccountProviderDescriptionType {
    val imageLinks = mutableListOf<Link>()
    this.logo?.let { uri ->
      imageLinks.add(Link(
        href = uri,
        type = null,
        templated = false,
        relation = "http://opds-spec.org/image/thumbnail"))
    }

    // XXX: TODO: Reconstruct links from above fields
    val links = mutableListOf<Link>()

    return object: AccountProviderDescriptionType {
      override val id: URI
        get() = this@AccountProviderImmutable.id
      override val title: String
        get() = this@AccountProviderImmutable.displayName
      override val updated: DateTime
        get() = this@AccountProviderImmutable.updated
      override val links: List<Link>
        get() = links.toList()
      override val images: List<Link>
        get() = imageLinks.toList()
      override val isAutomatic: Boolean
        get() = this@AccountProviderImmutable.addAutomatically
      override val isProduction: Boolean
        get() = this@AccountProviderImmutable.isProduction
      override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
        onProgress.invoke(this.id, "")
        return AccountProviderResolutionResult(this@AccountProviderImmutable, listOf())
      }
    }
  }
}
