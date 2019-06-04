package org.nypl.simplified.accounts.api

import java.net.URI

/**
 * A builder interface for account providers.
 */

interface AccountProviderBuilderType {

  /**
   * @see AccountProviderType.id
   */

  var id: URI?

  /**
   * @see AccountProviderType.displayName
   */

  var displayName: String?

  /**
   * @see AccountProviderType.subtitle
   */

  var subtitle: String?

  /**
   * @see AccountProviderType.logo
   */

  var logo: URI?

  /**
   * @see AccountProviderType.authentication
   */

  var authentication: AccountProviderAuthenticationDescription?

  /**
   * @see AccountProviderType.supportsSimplyESynchronization
   */

  var supportsSimplyESynchronization: Boolean

  /**
   * @see AccountProviderType.supportsBarcodeScanner
   */

  var supportsBarcodeScanner: Boolean

  /**
   * @see AccountProviderType.supportsBarcodeDisplay
   */

  var supportsBarcodeDisplay: Boolean

  /**
   * @see AccountProviderType.supportsReservations
   */

  var supportsReservations: Boolean

  /**
   * @see AccountProviderType.supportsCardCreator
   */

  var supportsCardCreator: Boolean

  /**
   * @see AccountProviderType.supportsHelpCenter
   */

  var supportsHelpCenter: Boolean

  /**
   * @see AccountProviderType.addAutomatically
   */

  var addAutomatically: Boolean

  /**
   * @see AccountProviderType.catalogURI
   */

  var catalogURI: URI?

  /**
   * @see AccountProviderType.catalogURIForOver13s
   */

  var catalogURIForOver13s: URI?

  /**
   * @see AccountProviderType.catalogURIForUnder13s
   */

  var catalogURIForUnder13s: URI?

  /**
   * @see AccountProviderType.supportEmail
   */

  var supportEmail: String?

  /**
   * @see AccountProviderType.eula
   */

  var eula: URI?

  /**
   * @see AccountProviderType.license
   */

  var license: URI?

  /**
   * @see AccountProviderType.privacyPolicy
   */

  var privacyPolicy: URI?

  /**
   * @see AccountProviderType.mainColor
   */

  var mainColor: String?

  /**
   * @see AccountProviderType.styleNameOverride
   */

  var styleNameOverride: String?

  /**
   * @see AccountProviderType.patronSettingsURI
   */

  var patronSettingsURI: URI?

  /**
   * @see AccountProviderType.annotationsURI
   */

  var annotationsURI: URI?

  /**
   * @return The constructed account provider
   */

  fun build(): AccountProviderType
}
