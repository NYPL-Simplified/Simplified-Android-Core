package org.nypl.simplified.simplye

import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderFallbackType
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderType
import java.net.URI

/**
 * The fallback account for SimplyE: The classics collection.
 */

class SimplyEAccountFallback : AccountProviderFallbackType {

  override fun get(): AccountProviderType {
    return AccountProvider(
      addAutomatically = false,
      annotationsURI = null,
      authenticationDocumentURI = URI.create("https://circulation.librarysimplified.org/CLASSICS/authentication_document"),
      authentication = AccountProviderAuthenticationDescription.COPPAAgeGate(
        greaterEqual13 = URI.create("https://circulation.librarysimplified.org/CLASSICS/groups/1831"),
        under13 = URI.create("https://circulation.librarysimplified.org/CLASSICS/groups/1832")
      ),
      authenticationAlternatives = listOf(),
      cardCreatorURI = null,
      catalogURI = URI.create("https://circulation.librarysimplified.org/CLASSICS/groups/1832"),
      displayName = "The SimplyE Collection",
      eula = null,
      id = URI.create("urn:uuid:56906f26-2c9a-4ae9-bd02-552557720b99"),
      idNumeric = -1,
      isProduction = true,
      license = URI.create("http://www.librarysimplified.org/iclicenses.html"),
      loansURI = URI.create("https://circulation.librarysimplified.org/CLASSICS/loans/"),
      logo = null,
      mainColor = "teal",
      patronSettingsURI = URI.create("https://circulation.librarysimplified.org/CLASSICS/patrons/me/"),
      privacyPolicy = null,
      subtitle = "A selection of classics and modern material available to anyone, with no library card necessary.",
      supportEmail = "mailto:gethelp+simplye-collection@nypl.org",
      supportsReservations = false,
      updated = DateTime.parse("2019-07-08T16:32:52+00:00"),
      location = null
    )
  }
}
