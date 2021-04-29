package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import org.nypl.simplified.announcements.Announcement
import java.net.URI

data class AccountProvider(
  override val id: URI,
  override val idNumeric: Int,
  override val isProduction: Boolean,
  override val displayName: String,
  override val subtitle: String?,
  override val logo: URI?,
  override val authentication: AccountProviderAuthenticationDescription,
  override val authenticationAlternatives: List<AccountProviderAuthenticationDescription>,
  override val supportsReservations: Boolean,
  override val loansURI: URI?,
  override val cardCreatorURI: URI?,
  override val authenticationDocumentURI: URI?,
  override val catalogURI: URI,
  override val supportEmail: String?,
  override val eula: URI?,
  override val license: URI?,
  override val privacyPolicy: URI?,
  override val mainColor: String,
  override val addAutomatically: Boolean,
  override val patronSettingsURI: URI?,
  override val annotationsURI: URI?,
  override val updated: DateTime,
  override val announcements: List<Announcement> = listOf(),
  override val location: AccountLibraryLocation?
) : AccountProviderType {
  override fun compareTo(other: AccountProviderType): Int {
    return this.id.compareTo(other.id)
  }

  override val supportsSimplyESynchronization: Boolean =
    this.annotationsURI != null

  companion object {

    /**
     * Make an immutable copy of the given account provider. If the other account provider
     * is already immutable, it will be returned directly.
     */

    fun copy(other: AccountProviderType): AccountProvider {
      if (other is AccountProvider) {
        return other
      }

      return AccountProvider(
        addAutomatically = other.addAutomatically,
        annotationsURI = other.annotationsURI,
        authentication = other.authentication,
        authenticationAlternatives = other.authenticationAlternatives,
        authenticationDocumentURI = other.authenticationDocumentURI,
        cardCreatorURI = other.cardCreatorURI,
        catalogURI = other.catalogURI,
        displayName = other.displayName,
        eula = other.eula,
        id = other.id,
        idNumeric = other.idNumeric,
        isProduction = other.isProduction,
        license = other.license,
        loansURI = other.loansURI,
        logo = other.logo,
        mainColor = other.mainColor,
        patronSettingsURI = other.patronSettingsURI,
        privacyPolicy = other.privacyPolicy,
        subtitle = other.subtitle,
        supportEmail = other.supportEmail,
        supportsReservations = other.supportsReservations,
        updated = other.updated,
        announcements = other.announcements,
        location = other.location
      )
    }
  }
}
