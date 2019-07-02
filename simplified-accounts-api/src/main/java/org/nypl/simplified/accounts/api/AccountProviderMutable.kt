package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import java.net.URI
import javax.annotation.concurrent.ThreadSafe

/**
 * A mutable, thread-safe account provider.
 */

@ThreadSafe
class AccountProviderMutable(
  @Volatile private var source: AccountProviderImmutable) : AccountProviderType {

  /**
   * Apply `f` to this account provider.
   *
   * @return `this`
   */

  fun update(f: (AccountProviderImmutable) -> AccountProviderImmutable): AccountProviderMutable {
    this.source = f.invoke(this.source)
    return this
  }

  override val id: URI get() = this.source.id
  override val isProduction: Boolean get() = this.source.isProduction
  override val displayName: String get() = this.source.displayName
  override val subtitle: String? get() = this.source.subtitle
  override val logo: URI? get() = this.source.logo
  override val authentication: AccountProviderAuthenticationDescription? get() = this.source.authentication
  override val supportsSimplyESynchronization: Boolean get() = this.source.supportsSimplyESynchronization
  override val supportsBarcodeScanner: Boolean get() = this.source.supportsBarcodeScanner
  override val supportsBarcodeDisplay: Boolean get() = this.source.supportsBarcodeDisplay
  override val supportsReservations: Boolean get() = this.source.supportsReservations
  override val supportsCardCreator: Boolean get() = this.source.supportsCardCreator
  override val supportsHelpCenter: Boolean get() = this.source.supportsHelpCenter
  override val authenticationDocumentURI: URI? get() = this.source.authenticationDocumentURI
  override val catalogURI: URI get() = this.source.catalogURI
  override val catalogURIForOver13s: URI? get() = this.source.catalogURIForOver13s
  override val catalogURIForUnder13s: URI? get() = this.source.catalogURIForUnder13s
  override val supportEmail: String? get() = this.source.supportEmail
  override val eula: URI? get() = this.source.eula
  override val license: URI? get() = this.source.license
  override val privacyPolicy: URI? get() = this.source.privacyPolicy
  override val mainColor: String get() = this.source.mainColor
  override val styleNameOverride: String? get() = this.source.styleNameOverride
  override val addAutomatically: Boolean get() = this.source.addAutomatically
  override val patronSettingsURI: URI? get() = this.source.patronSettingsURI
  override val annotationsURI: URI? get() = this.source.annotationsURI
  override val updated: DateTime get() = this.source.updated
  override fun toDescription(): AccountProviderDescriptionType = this.source.toDescription()
}
