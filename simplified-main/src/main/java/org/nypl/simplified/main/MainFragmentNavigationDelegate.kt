package org.nypl.simplified.main

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountFragment
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListFragment
import org.nypl.simplified.ui.accounts.AccountListFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.navigation.tabs.R
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationCommand
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigator
import org.nypl.simplified.ui.settings.SettingsFragmentCustomOPDS
import org.nypl.simplified.ui.settings.SettingsFragmentDebug
import org.nypl.simplified.viewer.api.Viewers
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.slf4j.LoggerFactory
import java.net.URI

internal class MainFragmentNavigationDelegate(
  private val activity: AppCompatActivity,
  private val navigationController: TabbedNavigationController,
  private val navigator: TabbedNavigator,
  private val profilesController: ProfilesControllerType,
  private val settingsConfiguration: BuildConfigurationServiceType
): LifecycleObserver {

  init {
    this.navigationController.subscribeInfoStream(navigator)
  }

  private val logger =
    LoggerFactory.getLogger(MainFragmentNavigationDelegate::class.java)

  private val subscriptions =
    CompositeDisposable()

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
      this.navigationController.commandQueue
        .subscribe(this::handleCommand)
        .let { subscriptions.add(it) }

    this.navigationController.infoStream
      .subscribe { action ->
        this.logger.debug(action.toString())
        this.onFragmentTransactionCompleted()
      }
      .let { subscriptions.add(it) }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    subscriptions.clear()
  }

  private fun onFragmentTransactionCompleted() {
    val isRoot = (0 == this.navigator.backStackSize())
    this.logger.debug(
      "controller stack size changed [{}, isRoot={}]", this.navigator.backStackSize(), isRoot
    )
    configureToolbar()
  }

  private fun configureToolbar() {
    val isRoot = (0 == this.navigator.backStackSize())
    this.activity.supportActionBar?.apply {
      setHomeAsUpIndicator(null)
      setHomeActionContentDescription(null)
      setDisplayHomeAsUpEnabled(!isRoot)
    }
  }

  fun handleCommand(command: TabbedNavigationCommand) {
    when(command) {
      TabbedNavigationCommand.AccountCommand.OnAccountCreated ->
        navigator.popBackStack()
      TabbedNavigationCommand.AccountCommand.OnSAMLEventAccessTokenObtained ->
        navigator.popBackStack()
      TabbedNavigationCommand.AccountCommand.OpenCatalogAfterAuthentication ->
        openCatalogAfterAuthentication()
      is TabbedNavigationCommand.AccountCommand.OpenErrorPage ->
        openErrorPage(command.parameters)
      is TabbedNavigationCommand.AccountCommand.OpenSAML20Login ->
        openSAML20Login(command.parameters)
      is TabbedNavigationCommand.AccountCommand.OpenSettingsAccount ->
        openSettingsAccount(command.parameters)
      TabbedNavigationCommand.AccountCommand.OpenSettingsAccountRegistry ->
        openSettingsAccountRegistry()

      TabbedNavigationCommand.CatalogCommand.OnSAML20LoginSucceeded ->
        navigator.popBackStack()
      is TabbedNavigationCommand.CatalogCommand.OpenBookDetail ->
        openBookDetail(command.feedArguments, command.entry)
      is TabbedNavigationCommand.CatalogCommand.OpenBookDownloadLogin ->
        openBookDownloadLogin(command.bookID, command.downloadURI)
      is TabbedNavigationCommand.CatalogCommand.OpenFeed ->
        openFeed(command.feedArguments)
      is TabbedNavigationCommand.CatalogCommand.OpenViewer ->
        openViewer(command.book, command.format)

      TabbedNavigationCommand.SettingsCommand.OpenSettingsAbout ->
        openSettingsAbout()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsAccounts ->
        openSettingsAccounts()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsAcknowledgements ->
        openSettingsAcknowledgements()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsCustomOPDS ->
        openSettingsCustomOPDS()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsEULA ->
        openSettingsEULA()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsFaq ->
        openSettingsFaq()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsLicense ->
        openSettingsLicense()
      TabbedNavigationCommand.SettingsCommand.OpenSettingsVersion ->
        openSettingsVersion()
    }
  }

  private fun openSettingsAbout() {
    throw NotImplementedError()
  }

  private fun openSettingsAccounts() {
    this.navigator.addFragment(
      fragment = AccountListFragment.create(
        AccountListFragmentParameters(
          shouldShowLibraryRegistryMenu = this.settingsConfiguration.allowAccountsRegistryAccess
        )
      ),
      tab = R.id.tabSettings
    )
  }

  private fun openSettingsAcknowledgements() {
    throw NotImplementedError()
  }

  private fun openSettingsEULA() {
    throw NotImplementedError()
  }

  private fun openSettingsFaq() {
    throw NotImplementedError()
  }

  private fun openSettingsLicense() {
    throw NotImplementedError()
  }

  private fun openSettingsVersion() {
    this.navigator.addFragment(
      fragment = SettingsFragmentDebug(),
      tab = R.id.tabSettings
    )
  }

 private fun openSettingsCustomOPDS() {
    this.navigator.addFragment(
      fragment = SettingsFragmentCustomOPDS(),
      tab = R.id.tabSettings
    )
 }

  private fun openErrorPage(parameters: ErrorPageParameters) {
    this.navigator.addFragment(
      fragment = ErrorPageFragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  private fun openSettingsAccount(parameters: AccountFragmentParameters) {
    this.navigator.addFragment(
      fragment = AccountFragment.create(parameters),
      tab = R.id.tabSettings
    )
  }

  private fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
    this.navigator.addFragment(
      fragment = AccountSAML20Fragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  private fun openBookDetail(
    feedArguments: CatalogFeedArguments,
    entry: FeedEntry.FeedEntryOPDS
  ) {
    this.navigator.addFragment(
      fragment = CatalogFragmentBookDetail.create(
        CatalogFragmentBookDetailParameters(
          feedEntry = entry,
          feedArguments = feedArguments
        )
      ),
      tab = this.navigator.currentTab()
    )
  }

  private fun openBookDownloadLogin(
    bookID: BookID,
    downloadURI: URI
  ) {
    this.navigator.addFragment(
      fragment = CatalogSAML20Fragment.create(
        CatalogSAML20FragmentParameters(
          bookID = bookID,
          downloadURI = downloadURI
        )
      ),
      tab = this.navigator.currentTab()
    )
  }

  private fun openFeed(feedArguments: CatalogFeedArguments) {
    this.navigator.addFragment(
      fragment = CatalogFragmentFeed.create(feedArguments),
      tab = this.navigator.currentTab()
    )
  }

  private fun openSettingsAccountRegistry() {
    this.navigator.addFragment(
      fragment = AccountListRegistryFragment(),
      tab = R.id.tabSettings
    )
  }

  private fun openCatalogAfterAuthentication() {
    this.navigator.reset(R.id.tabCatalog, false)
  }

  private fun openViewer(
    book: Book,
    format: BookFormat
  ) {
    /*
     * XXX: Enable or disable support for R2 based on the current profile's preferences. When R2
     * moves from being experimental, this code can be removed.
     */

    val profile =
      this.profilesController.profileCurrent()
    val viewerPreferences =
      ViewerPreferences(
        flags = mapOf(Pair("useExperimentalR2", profile.preferences().useExperimentalR2))
      )

    Viewers.openViewer(
      activity = activity,
      preferences = viewerPreferences,
      book = book,
      format = format
    )
  }
}
