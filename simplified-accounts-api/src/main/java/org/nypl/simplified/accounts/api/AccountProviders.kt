package org.nypl.simplified.accounts.api

import java.lang.IllegalStateException
import java.net.URI

/**
 * Account provider constructors.
 */

object AccountProviders {

  private data class Provider(
    override val id: URI,
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
    override val annotationsURI: URI?
  ) : AccountProviderType {

    override fun toBuilder(): AccountProviderBuilderType {
      return Builder(
        id = this.id,
        displayName = this.displayName,
        subtitle = this.subtitle,
        logo = this.logo,
        supportsSimplyESynchronization = this.supportsSimplyESynchronization,
        supportsBarcodeScanner = this.supportsBarcodeScanner,
        supportsBarcodeDisplay = this.supportsBarcodeDisplay,
        supportsReservations = this.supportsReservations,
        supportsCardCreator = this.supportsCardCreator,
        supportsHelpCenter = this.supportsHelpCenter,
        addAutomatically = this.addAutomatically,
        catalogURI = this.catalogURI,
        catalogURIForOver13s = this.catalogURIForOver13s,
        catalogURIForUnder13s = this.catalogURIForUnder13s,
        supportEmail = this.supportEmail,
        eula = this.eula,
        license = this.license,
        privacyPolicy = this.privacyPolicy,
        mainColor = this.mainColor)
    }
  }

  private data class Builder(
    override var id: URI? = null,
    override var displayName: String? = null,
    override var subtitle: String? = null,
    override var logo: URI? = null,
    override var supportsSimplyESynchronization: Boolean = false,
    override var supportsBarcodeScanner: Boolean = false,
    override var supportsBarcodeDisplay: Boolean = false,
    override var supportsReservations: Boolean = false,
    override var supportsCardCreator: Boolean = false,
    override var supportsHelpCenter: Boolean = false,
    override var addAutomatically: Boolean = false,
    override var catalogURI: URI? = null,
    override var catalogURIForOver13s: URI? = null,
    override var catalogURIForUnder13s: URI? = null,
    override var supportEmail: String? = null,
    override var eula: URI? = null,
    override var license: URI? = null,
    override var privacyPolicy: URI? = null,
    override var mainColor: String? = null,
    override var styleNameOverride: String? = null,
    override var patronSettingsURI: URI? = null,
    override var annotationsURI: URI? = null,
    override var authentication: AccountProviderAuthenticationDescription? = null)
    : AccountProviderBuilderType {

    override fun build(): AccountProviderType {
      val id =
        this.check(this.id, "id")
      val displayName =
        this.check(this.displayName, "displayName")
      val catalogURI =
        this.check(this.catalogURI, "catalogURI")
      val mainColor =
        this.check(this.mainColor, "mainColor")

      return Provider(
        id = id,
        displayName = displayName,
        subtitle = this.subtitle,
        logo = this.logo,
        authentication = this.authentication,
        supportsSimplyESynchronization = this.supportsSimplyESynchronization,
        supportsBarcodeScanner = this.supportsBarcodeScanner,
        supportsBarcodeDisplay = this.supportsBarcodeDisplay,
        supportsReservations = this.supportsReservations,
        supportsCardCreator = this.supportsCardCreator,
        supportsHelpCenter = this.supportsHelpCenter,
        catalogURI = catalogURI,
        catalogURIForOver13s = this.catalogURIForOver13s,
        catalogURIForUnder13s = this.catalogURIForUnder13s,
        supportEmail = this.supportEmail,
        eula = this.eula,
        license = this.license,
        privacyPolicy = this.privacyPolicy,
        mainColor = mainColor,
        styleNameOverride = this.styleNameOverride,
        addAutomatically = this.addAutomatically,
        patronSettingsURI = this.patronSettingsURI,
        annotationsURI = this.annotationsURI)
    }

    private fun <T> check(value: T?, name: String): T {
      return value ?: throw IllegalStateException("${name} must be provided")
    }
  }

  /**
   * @return A new account provider builder
   */

  @JvmStatic
  fun builder(): AccountProviderBuilderType {
    return Builder()
  }

}