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
import org.nypl.simplified.ui.accounts.AccountsNavigationCommand
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationCommand
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.navigation.tabs.R
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigator
import org.nypl.simplified.ui.settings.SettingsFragmentCustomOPDS
import org.nypl.simplified.ui.settings.SettingsFragmentDebug
import org.nypl.simplified.ui.settings.SettingsNavigationCommand
import org.nypl.simplified.viewer.api.Viewers
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.slf4j.LoggerFactory
import java.net.URI

internal class MainFragmentNavigationDelegate(
  private val activity: AppCompatActivity,
  private val navigationViewModel: MainFragmentNavigationViewModel,
  private val navigator: TabbedNavigator,
  private val profilesController: ProfilesControllerType,
  private val settingsConfiguration: BuildConfigurationServiceType
): LifecycleObserver {

  init {
    this.navigationViewModel.subscribeInfoStream(navigator)
  }

  private val logger =
    LoggerFactory.getLogger(MainFragmentNavigationDelegate::class.java)

  private val subscriptions =
    CompositeDisposable()

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
      this.navigationViewModel.registerHandler(this::handleCommand)

    this.navigationViewModel.infoStream
      .subscribe { action ->
        this.logger.debug(action.toString())
        this.onFragmentTransactionCompleted()
      }
      .let { subscriptions.add(it) }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    this.navigationViewModel.unregisterHandler()
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

  private fun handleCommand(command: MainFragmentNavigationCommand) {
    return when (command) {
      is MainFragmentNavigationCommand.CatalogNavigationCommand ->
        this.handleCatalogCommand(command.command)
      is MainFragmentNavigationCommand.AccountsNavigationCommand ->
        this.handleAccountsCommand(command.command)
      is MainFragmentNavigationCommand.SettingsNavigationCommand ->
        this.handleSettingsCommand(command.command)
    }
  }

  private fun handleCatalogCommand(command: CatalogNavigationCommand) {
    return when(command) {
      CatalogNavigationCommand.OnSAML20LoginSucceeded ->
        this.popBackStack()
      is CatalogNavigationCommand.OpenBookDetail ->
        this.openBookDetail(command.feedArguments, command.entry)
      is CatalogNavigationCommand.OpenBookDownloadLogin ->
        this.openBookDownloadLogin(command.bookID, command.downloadURI)
      is CatalogNavigationCommand.OpenFeed ->
        this.openFeed(command.feedArguments)
      is CatalogNavigationCommand.OpenViewer ->
        this.openViewer(command.book, command.format)
    }
  }

  private fun handleAccountsCommand(command: AccountsNavigationCommand) {
    return when(command) {
      AccountsNavigationCommand.OnAccountCreated ->
        this.popBackStack()
      AccountsNavigationCommand.OnSAMLEventAccessTokenObtained ->
        this.popBackStack()
      AccountsNavigationCommand.OpenCatalogAfterAuthentication ->
        this.openCatalogAfterAuthentication()
      is AccountsNavigationCommand.OpenErrorPage ->
        this.openErrorPage(command.parameters)
      is AccountsNavigationCommand.OpenSAML20Login ->
        this.openSAML20Login(command.parameters)
      is AccountsNavigationCommand.OpenSettingsAccount ->
        this.openSettingsAccount(command.parameters)
      AccountsNavigationCommand.OpenSettingsAccountRegistry ->
        this.openSettingsAccountRegistry()
    }
  }

  private fun handleSettingsCommand(command: SettingsNavigationCommand) {
    return when(command) {
      SettingsNavigationCommand.OpenSettingsAbout ->
        this.openSettingsAbout()
      SettingsNavigationCommand.OpenSettingsAccounts ->
        this.openSettingsAccounts()
      SettingsNavigationCommand.OpenSettingsAcknowledgements ->
        this.openSettingsAcknowledgements()
      SettingsNavigationCommand.OpenSettingsCustomOPDS ->
        this.openSettingsCustomOPDS()
      SettingsNavigationCommand.OpenSettingsEULA ->
        this.openSettingsEULA()
      SettingsNavigationCommand.OpenSettingsFaq ->
        this.openSettingsFaq()
      SettingsNavigationCommand.OpenSettingsLicense ->
        this.openSettingsLicense()
      SettingsNavigationCommand.OpenSettingsVersion ->
        this.openSettingsVersion()
    }
  }

  private fun popBackStack() {
    this.navigator.popBackStack()
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
