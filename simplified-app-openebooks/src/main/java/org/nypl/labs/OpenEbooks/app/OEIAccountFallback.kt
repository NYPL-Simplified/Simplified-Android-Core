package org.nypl.labs.OpenEbooks.app

import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderFallbackType
import org.nypl.simplified.accounts.api.AccountProviderType
import java.net.URI

/**
 * The main account for Open eBooks.
 */

class OEIAccountFallback : AccountProviderFallbackType {

  private val basicAuth =
    AccountProviderAuthenticationDescription.Basic(
      description = "First Book - JWT",
      barcodeFormat = null,
      keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      passwordMaximumLength = -1,
      labels = mapOf(),
      logoURI = URI.create("https://circulation.openebooks.us/images/FirstBookLoginButton280.png")
    )

  private val cleverAuth =
    AccountProviderAuthenticationDescription.OAuthWithIntermediary(
      description = "Clever",
      authenticate = URI.create("https://circulation.openebooks.us/USOEI/oauth_authenticate?provider=Clever"),
      logoURI = URI.create("https://circulation.openebooks.us/images/CleverLoginButton280.png")
    )

  override fun get(): AccountProviderType {
    return AccountProvider(
      addAutomatically = true,
      annotationsURI = null,
      authenticationDocumentURI = URI.create("https://circulation.openebooks.us/USOEI/authentication_document"),
      authentication = this.basicAuth,
      authenticationAlternatives = listOf(this.cleverAuth),
      cardCreatorURI = null,
      catalogURI = URI.create("https://circulation.openebooks.us/USOEI/groups"),
      displayName = "Open eBooks",
      eula = null,
      id = URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"),
      idNumeric = -1,
      isProduction = true,
      license = URI.create("http://www.librarysimplified.org/iclicenses.html"),
      loansURI = URI.create("https://circulation.openebooks.us/USOEI/loans/"),
      logo = null,
      mainColor = "teal",
      patronSettingsURI = URI.create("https://circulation.openebooks.us/USOEI/patrons/me/"),
      privacyPolicy = URI.create("https://openebooks.net/app_privacy.html"),
      subtitle = "",
      supportEmail = null,
      supportsReservations = false,
      updated = DateTime.parse("2020-05-10T00:00:00Z"),
      location = null
    )
  }
}
