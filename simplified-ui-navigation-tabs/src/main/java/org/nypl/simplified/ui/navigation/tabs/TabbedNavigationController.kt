package org.nypl.simplified.ui.navigation.tabs

import android.app.Activity
import com.pandora.bottomnavigator.BottomNavigator
import com.pandora.bottomnavigator.NavigatorAction
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.disposables.Disposable
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import java.net.URI

class TabbedNavigationController
  : SettingsNavigationControllerType, CatalogNavigationControllerType {

  val commandQueue: UnicastWorkSubject<TabbedNavigationCommand> =
    UnicastWorkSubject.create()

  val infoStream: UnicastWorkSubject<NavigatorAction> =
    UnicastWorkSubject.create()

  fun subscribeInfoStream(bottomNavigator: BottomNavigator) {
    if (infoStreamSubscription != null) {
      return
    }

    infoStreamSubscription =
      bottomNavigator
        .infoStream()
        .subscribe { action -> infoStream.onNext(action) }
  }

  fun disposeInfoStream() {
    infoStreamSubscription?.dispose()
    infoStreamSubscription = null
  }

  private var infoStreamSubscription: Disposable? = null

  override fun openSettingsAbout() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsAbout)
  }

  override fun openSettingsAccounts() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsAccounts)
  }

  override fun openSettingsAcknowledgements() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsAcknowledgements)
  }

  override fun openSettingsEULA() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsEULA)
  }

  override fun openSettingsFaq() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsFaq)
  }

  override fun openSettingsLicense() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsLicense)
  }

  override fun openSettingsVersion() {
   commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsVersion)
  }

  override fun openSettingsCustomOPDS() {
    commandQueue.onNext(TabbedNavigationCommand.SettingsCommand.OpenSettingsCustomOPDS)
  }

  override fun openErrorPage(parameters: ErrorPageParameters) {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OpenErrorPage(parameters))
  }

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
   commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OpenSettingsAccount(parameters))
  }

  override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OpenSAML20Login(parameters))
  }

  override fun openBookDetail(
    feedArguments: CatalogFeedArguments,
    entry: FeedEntry.FeedEntryOPDS
  ) {
    commandQueue.onNext(TabbedNavigationCommand.CatalogCommand.OpenBookDetail(feedArguments, entry))
  }

  override fun openBookDownloadLogin(
    bookID: BookID,
    downloadURI: URI
  ) {
    commandQueue.onNext(TabbedNavigationCommand.CatalogCommand.OpenBookDownloadLogin(bookID, downloadURI))
  }

  override fun openFeed(feedArguments: CatalogFeedArguments) {
    commandQueue.onNext(TabbedNavigationCommand.CatalogCommand.OpenFeed(feedArguments))
  }

  override fun openSettingsAccountRegistry() {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OpenSettingsAccountRegistry)
  }

  override fun openCatalogAfterAuthentication() {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OpenCatalogAfterAuthentication)
  }

  override fun onAccountCreated() {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OnAccountCreated)
  }

  override fun onSAMLEventAccessTokenObtained() {
    commandQueue.onNext(TabbedNavigationCommand.AccountCommand.OnSAMLEventAccessTokenObtained)
  }

  override fun onSAML20LoginSucceeded() {
    commandQueue.onNext(TabbedNavigationCommand.CatalogCommand.OnSAML20LoginSucceeded)
  }

  override fun openViewer(
    activity: Activity,
    book: Book,
    format: BookFormat
  ) {
    commandQueue.onNext(TabbedNavigationCommand.CatalogCommand.OpenViewer(book, format))
  }
}
