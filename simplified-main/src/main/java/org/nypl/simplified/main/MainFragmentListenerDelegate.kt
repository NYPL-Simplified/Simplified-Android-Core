package org.nypl.simplified.main

import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.ListenerRepository
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountDetailEvent
import org.nypl.simplified.ui.accounts.AccountDetailFragment
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListEvent
import org.nypl.simplified.ui.accounts.AccountListFragment
import org.nypl.simplified.ui.accounts.AccountListFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListRegistryEvent
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountPickerEvent
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20Event
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.catalog.CatalogBookDetailEvent
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedEvent
import org.nypl.simplified.ui.catalog.CatalogBookDetailFragment
import org.nypl.simplified.ui.catalog.CatalogBookDetailFragmentParameters
import org.nypl.simplified.ui.catalog.CatalogFeedFragment
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Event
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.navigation.tabs.R
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigator
import org.nypl.simplified.ui.profiles.ProfileTabEvent
import org.nypl.simplified.ui.settings.SettingsCustomOPDSFragment
import org.nypl.simplified.ui.settings.SettingsDebugEvent
import org.nypl.simplified.ui.settings.SettingsDebugFragment
import org.nypl.simplified.ui.settings.SettingsMainEvent
import org.nypl.simplified.viewer.api.Viewers
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.slf4j.LoggerFactory

internal class MainFragmentListenerDelegate(
  private val fragment: Fragment,
  private val listenerRepository: ListenerRepository<MainFragmentListenedEvent, MainFragmentState>,
  private val navigator: TabbedNavigator,
  private val listener: FragmentListenerType<MainFragmentEvent>,
  private val profilesController: ProfilesControllerType,
  private val settingsConfiguration: BuildConfigurationServiceType
) : LifecycleObserver {
  private val logger =
    LoggerFactory.getLogger(MainFragmentListenerDelegate::class.java)

  private val subscriptions =
    CompositeDisposable()

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  fun onCreate() {
    this.configureToolbar()

    val activity = this.fragment.requireActivity() as AppCompatActivity

    activity.onBackPressedDispatcher.addCallback(this.fragment) {
      if (navigator.popBackStack()) {
        return@addCallback
      }

      try {
        isEnabled = false
        activity.onBackPressed()
      } finally {
        isEnabled = true
      }
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
    this.listenerRepository.registerHandler(this::handleEvent)

    this.navigator.infoStream
      .subscribe { action ->
        this.logger.debug(action.toString())
        this.onFragmentTransactionCompleted()
      }
      .let { subscriptions.add(it) }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    this.listenerRepository.unregisterHandler()
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
    val activity = this.fragment.requireActivity() as AppCompatActivity
    activity.supportActionBar?.apply {
      setHomeAsUpIndicator(null)
      setHomeActionContentDescription(null)
      setDisplayHomeAsUpEnabled(!isRoot)
    }
  }

  private fun handleEvent(
    event: MainFragmentListenedEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is MainFragmentListenedEvent.CatalogSAML20Event ->
        this.handleCatalogSAML20Event(event.event, state)
      is MainFragmentListenedEvent.CatalogFeedEvent ->
        this.handleCatalogFeedEvent(event.event, state)
      is MainFragmentListenedEvent.CatalogBookDetailEvent ->
        this.handleCatalogBookDetailEvent(event.event, state)
      is MainFragmentListenedEvent.SettingsMainEvent ->
        this.handleSettingsMainEvent(event.event, state)
      is MainFragmentListenedEvent.SettingsDebugEvent ->
        this.handleSettingsDebugEvent(event.event, state)
      is MainFragmentListenedEvent.AccountListRegistryEvent ->
        this.handleAccountListRegistryEvent(event.event, state)
      is MainFragmentListenedEvent.AccountListEvent ->
        this.handleAccountListEvent(event.event, state)
      is MainFragmentListenedEvent.AccountDetailEvent ->
        this.handleAccountEvent(event.event, state)
      is MainFragmentListenedEvent.AccountSAML20Event ->
        this.handleAccountSAML20Event(event.event, state)
      is MainFragmentListenedEvent.AccountPickerEvent ->
        this.handleAccountPickerEvent(event.event, state)
      is MainFragmentListenedEvent.ProfileTabEvent ->
        this.handleProfileTabEvent(event.event, state)
    }
  }

  private fun handleCatalogSAML20Event(
    event: CatalogSAML20Event,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is CatalogSAML20Event.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
      CatalogSAML20Event.LoginSucceeded -> {
        this.popBackStack()
        state
      }
    }
  }

  private fun handleCatalogFeedEvent(
    event: CatalogFeedEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is CatalogFeedEvent.LoginRequired -> {
        this.openSettingsAccount(event.account, showPleaseLogInTitle = true)
        MainFragmentState.CatalogWaitingForLogin
      }
      is CatalogFeedEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
      is CatalogFeedEvent.OpenViewer -> {
        this.openViewer(event.book, event.format)
        state
      }
      is CatalogFeedEvent.OpenBookDetail -> {
        this.openBookDetail(event.feedArguments, event.opdsEntry)
        state
      }
      is CatalogFeedEvent.OpenFeed -> {
        this.openFeed(event.feedArguments)
        state
      }
    }
  }

  private fun handleCatalogBookDetailEvent(
    event: CatalogBookDetailEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is CatalogBookDetailEvent.LoginRequired -> {
        this.openSettingsAccount(event.account, showPleaseLogInTitle = true)
        MainFragmentState.CatalogWaitingForLogin
      }
      is CatalogBookDetailEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
      is CatalogBookDetailEvent.OpenViewer -> {
        this.openViewer(event.book, event.format)
        state
      }
      is CatalogBookDetailEvent.OpenFeed -> {
        this.openFeed(event.feedArguments)
        state
      }
    }
  }

  private fun handleAccountListRegistryEvent(
    event: AccountListRegistryEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      AccountListRegistryEvent.AccountCreated -> {
        this.popBackStack()
        state
      }
      is AccountListRegistryEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
    }
  }

  private fun handleAccountListEvent(
    event: AccountListEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is AccountListEvent.AccountSelected -> {
        this.openSettingsAccount(event.account, showPleaseLogInTitle = false)
        state
      }
      AccountListEvent.AddAccount -> {
        this.openSettingsAccountRegistry()
        state
      }
      is AccountListEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
    }
  }

  private fun handleAccountEvent(
    event: AccountDetailEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      AccountDetailEvent.LoginSucceeded ->
        if (state is MainFragmentState.CatalogWaitingForLogin) {
          this.openCatalogAfterAuthentication()
          MainFragmentState.EmptyState
        } else {
          state
        }
      is AccountDetailEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
      is AccountDetailEvent.OpenSAML20Login -> {
        this.openSAML20Login(event.account, event.authenticationDescription)
        state
      }
    }
  }

  private fun handleAccountSAML20Event(
    event: AccountSAML20Event,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      AccountSAML20Event.AccessTokenObtained -> {
        this.popBackStack()
        state
      }
      is AccountSAML20Event.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
    }
  }

  private fun handleAccountPickerEvent(
    event: AccountPickerEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      is AccountPickerEvent.AccountSelected -> {
        // TODO: this should work without this for now
        state
      }
      AccountPickerEvent.AddAccount -> {
        this.openSettingsAccountRegistry()
        state
      }
    }
  }

  private fun handleSettingsMainEvent(
    event: SettingsMainEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      SettingsMainEvent.OpenAbout -> {
        this.openSettingsAbout()
        state
      }
      SettingsMainEvent.OpenAccountList -> {
        this.openSettingsAccounts()
        state
      }
      SettingsMainEvent.OpenAcknowledgments -> {
        this.openSettingsAcknowledgements()
        state
      }
      SettingsMainEvent.OpenDebugOptions -> {
        this.openSettingsVersion()
        state
      }
      SettingsMainEvent.OpenEULA -> {
        this.openSettingsEULA()
        state
      }
      SettingsMainEvent.OpenFAQ -> {
        this.openSettingsFaq()
        state
      }
      SettingsMainEvent.OpenLicense -> {
        this.openSettingsLicense()
        state
      }
    }
  }

  private fun handleSettingsDebugEvent(
    event: SettingsDebugEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      SettingsDebugEvent.OpenCustomOPDS -> {
        this.openSettingsCustomOPDS()
        state
      }
      is SettingsDebugEvent.OpenErrorPage -> {
        this.openErrorPage(event.parameters)
        state
      }
    }
  }

  private fun handleProfileTabEvent(
    event: ProfileTabEvent,
    state: MainFragmentState
  ): MainFragmentState {
    return when (event) {
      ProfileTabEvent.SwitchProfileSelected -> {
        this.listener.post(MainFragmentEvent.SwitchProfileSelected)
        state
      }
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
      fragment = SettingsDebugFragment(),
      tab = R.id.tabSettings
    )
  }

  private fun openSettingsCustomOPDS() {
    this.navigator.addFragment(
      fragment = SettingsCustomOPDSFragment(),
      tab = R.id.tabSettings
    )
  }

  private fun openErrorPage(parameters: ErrorPageParameters) {
    this.navigator.addFragment(
      fragment = ErrorPageFragment.create(parameters),
      tab = this.navigator.currentTab()
    )
  }

  private fun openSettingsAccount(account: AccountID, showPleaseLogInTitle: Boolean) {
    this.navigator.addFragment(
      fragment = AccountDetailFragment.create(
        AccountFragmentParameters(
          accountId = account,
          closeOnLoginSuccess = false,
          showPleaseLogInTitle = showPleaseLogInTitle
        )
      ),
      tab = R.id.tabSettings
    )
  }

  private fun openSAML20Login(
    account: AccountID,
    authenticationDescription: AccountProviderAuthenticationDescription.SAML2_0
  ) {
    this.navigator.addFragment(
      fragment = AccountSAML20Fragment.create(
        AccountSAML20FragmentParameters(
          accountID = account,
          authenticationDescription = authenticationDescription
        )
      ),
      tab = this.navigator.currentTab()
    )
  }

  private fun openBookDetail(
    feedArguments: CatalogFeedArguments,
    entry: FeedEntry.FeedEntryOPDS
  ) {
    this.navigator.addFragment(
      fragment = CatalogBookDetailFragment.create(
        CatalogBookDetailFragmentParameters(
          feedEntry = entry,
          feedArguments = feedArguments
        )
      ),
      tab = this.navigator.currentTab()
    )
  }

  private fun openFeed(feedArguments: CatalogFeedArguments) {
    this.navigator.addFragment(
      fragment = CatalogFeedFragment.create(feedArguments),
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
      activity = this.fragment.requireActivity(),
      preferences = viewerPreferences,
      book = book,
      format = format
    )
  }
}
