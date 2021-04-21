package org.nypl.simplified.ui.navigation.tabs

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import java.net.URI

sealed class TabbedNavigationCommand {

  sealed class SettingsCommand : TabbedNavigationCommand() {

    /**
     * The settings screen wants to open the "about" section.
     */

    object OpenSettingsAbout : SettingsCommand()

    /**
     * The settings screen wants to open the list of accounts.
     */

    object OpenSettingsAccounts : SettingsCommand()

    /**
     * The settings screen wants to open the "acknowledgements" section.
     */

    object OpenSettingsAcknowledgements : SettingsCommand()

    /**
     * The settings screen wants to open the "EULA" section.
     */

    object OpenSettingsEULA : SettingsCommand()

    /**
     * The settings screen wants to open the "FAQ" section.
     */

    object OpenSettingsFaq : SettingsCommand()

    /**
     * The settings screen wants to open the "license" section.
     */

    object OpenSettingsLicense : SettingsCommand()

    /**
     * The settings screen wants to open the version screen.
     */

    object OpenSettingsVersion : SettingsCommand()

    /**
     * The settings screen wants to open the custom OPDS creation form.
     */

    object OpenSettingsCustomOPDS : SettingsCommand()
  }

  sealed class CatalogCommand : TabbedNavigationCommand() {

    /**
     * The catalog wants to open a book detail page.
     */

    class OpenBookDetail(
      val feedArguments: CatalogFeedArguments,
      val entry: FeedEntry.FeedEntryOPDS
    ) : CatalogCommand()

    /**
     * The catalog wants to open an external login form for downloading a book.
     */

    class OpenBookDownloadLogin(
      val bookID: BookID,
      val downloadURI: URI
    ) : CatalogCommand()

    /**
     * A catalog screen wants to open a feed.
     */

    class OpenFeed(
      val feedArguments: CatalogFeedArguments
    ) : CatalogCommand()

    /**
     * A catalog screen wants to open a viewer for a book
     */

    class OpenViewer(
      val book: Book,
      val format: BookFormat
    ) : CatalogCommand()

    object OnSAML20LoginSucceeded : CatalogCommand()
  }

  sealed class AccountCommand : TabbedNavigationCommand() {

    /**
     * Open an account configuration screen for the given account.
     */

    class OpenSettingsAccount(val parameters: AccountFragmentParameters) : AccountCommand()

    /**
     * Open a SAML 2.0 login screen.
     */

    class OpenSAML20Login(val parameters: AccountSAML20FragmentParameters) : AccountCommand()

    /**
     * Open the error page.
     */

    class OpenErrorPage(val parameters: ErrorPageParameters) : AccountCommand()

    /**
     * Open the account registry.
     */

    object OpenSettingsAccountRegistry : AccountCommand()

    /**
     * Switch to whichever tab contains the catalog, forcing a reset of the tab.
     */

    object OpenCatalogAfterAuthentication : AccountCommand()

    object OnAccountCreated : AccountCommand()

    object OnSAMLEventAccessTokenObtained : AccountCommand()
  }
}
