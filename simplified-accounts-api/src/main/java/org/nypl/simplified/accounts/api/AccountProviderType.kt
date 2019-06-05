package org.nypl.simplified.accounts.api

import java.net.URI

/**
 * A provider of accounts.
 */

interface AccountProviderType : Comparable<AccountProviderType> {

  /**
   * @return The account provider URI
   */

  val id: URI

  /**
   * @return The display name
   */

  val displayName: String

  /**
   * @return The subtitle
   */

  val subtitle: String?

  /**
   * @return The logo image
   */

  val logo: URI?

  /**
   * @return An authentication description if authentication is required, or nothing if it isn't
   */

  val authentication: AccountProviderAuthenticationDescription?

  /**
   * @return `true` iff the SimplyE synchronization is supported
   * @see .annotationsURI
   * @see .patronSettingsURI
   */

  val supportsSimplyESynchronization: Boolean

  /**
   * @return `true` iff the barcode scanner is supported
   */

  val supportsBarcodeScanner: Boolean

  /**
   * @return `true` iff the barcode display is supported
   */

  val supportsBarcodeDisplay: Boolean

  /**
   * @return `true` iff reservations are supported
   */

  val supportsReservations: Boolean

  /**
   * XXX: There is an associated Card Creator URL; this should be an OptionType[URI]
   *
   * @return `true` iff the card creator is supported
   */

  val supportsCardCreator: Boolean

  /**
   * @return `true` iff the help center is supported
   */

  val supportsHelpCenter: Boolean

  /**
   * @return The address of the authentication document for the account provider
   */

  val authenticationDocumentURI: URI?

  /**
   * @return The base URI of the catalog
   */

  val catalogURI: URI

  /**
   * The Over-13s catalog URI.
   *
   * @return The URI of the catalog for readers over the age of 13
   */

  val catalogURIForOver13s: URI?

  /**
   * @return The URI of the catalog for readers under the age of 13
   */

  val catalogURIForUnder13s: URI?

  /**
   * @return The support email address
   */

  val supportEmail: String?

  /**
   * @return The URI of the EULA if one is required
   */

  val eula: URI?

  /**
   * @return The URI of the EULA if one is required
   */

  val license: URI?

  /**
   * @return The URI of the privacy policy if one is required
   */

  val privacyPolicy: URI?

  /**
   * @return The main color used to decorate the application when using this provider
   */

  val mainColor: String

  /**
   * @return The name of the Android theme to use instead of the standard theme
   */

  val styleNameOverride: String?

  /**
   * @return `true` iff the account should be added by default
   */

  val addAutomatically: Boolean

  /**
   * The patron settings URI. This is the URI used to get and set patron settings.
   *
   * @return The patron settings URI
   */

  val patronSettingsURI: URI?

  /**
   * The annotations URI. This is the URI used to get and set annotations for bookmark
   * syncing.
   *
   * @return The annotations URI
   * @see .supportsSimplyESynchronization
   */

  val annotationsURI: URI?

  /**
   * Determine the correct catalog URI to use for readers of a given age.
   *
   * @param age The age of the reader
   * @return The correct catalog URI for the given age
   */

  fun catalogURIForAge(age: Int): URI {
    return if (age >= 13) {
      when (val o13 = this.catalogURIForOver13s) {
        null -> this.catalogURI
        else -> o13
      }
    } else {
      when (val u13 = this.catalogURIForUnder13s) {
        null -> this.catalogURI
        else -> u13
      }
    }
  }

  /**
   * @return `true` if the account has an age gate
   */

  fun hasAgeGate(): Boolean =
    (this.catalogURIForOver13s != null) or (this.catalogURIForUnder13s != null)

  /**
   * @return The current value as a mutable builder
   */

  fun toBuilder(): AccountProviderBuilderType

  override fun compareTo(other: AccountProviderType): Int =
    this.id.compareTo(other.id)
}
