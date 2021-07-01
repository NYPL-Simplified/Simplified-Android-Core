package org.nypl.simplified.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.ListenerRepository
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.listeners.api.listenerRepositories
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut
import org.nypl.simplified.ui.announcements.AnnouncementsDialog
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment
import org.nypl.simplified.ui.catalog.saml20.CatalogSAML20FragmentParameters
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigator
import org.nypl.simplified.ui.profiles.ProfileDialogs
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * The main application fragment.
 *
 * Currently, this displays a tabbed view and also displays dialogs on various application
 * events.
 */

class MainFragment : Fragment(R.layout.main_tabbed_host) {

  companion object {

    private val ANNOUNCEMENT_DIALOG_TAG =
      AnnouncementsDialog::class.java.simpleName
  }

  private val logger = LoggerFactory.getLogger(MainFragment::class.java)
  private val subscriptions = CompositeDisposable()

  private val viewModel: MainFragmentViewModel by viewModels()

  private val listener: FragmentListenerType<MainFragmentEvent> by fragmentListeners()
  private val listenerRepo: ListenerRepository<MainFragmentListenedEvent, MainFragmentState> by listenerRepositories()

  private val defaultViewModelFactory: ViewModelProvider.Factory by lazy {
    MainFragmentDefaultViewModelFactory(super.getDefaultViewModelProviderFactory())
  }

  private lateinit var bottomView: BottomNavigationView
  private lateinit var navigator: TabbedNavigator

  private var timeOutDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /*
    * If named profiles are enabled, subscribe to profile timer events so that users are
    * logged out after a period of inactivity.
    */

    when (viewModel.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        viewModel.profilesController.profileIdleTimer().start()
      }
    }

    /*
    * Demand that onOptionsItemSelected be called.
    */

    setHasOptionsMenu(true)

    this.checkForAnnouncements()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.bottomView =
      view.findViewById(R.id.bottomNavigator)

    /*
     * Hide various tabs based on build configuration and other settings.
     */
    this.setShowHoldsVisibility()

    val settingsItem = this.bottomView.menu.findItem(R.id.tabSettings)
    settingsItem.isVisible = viewModel.buildConfig.showSettingsTab
    settingsItem.isEnabled = viewModel.buildConfig.showSettingsTab

    val profilesVisible =
      viewModel.profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_DISABLED

    val profilesItem = this.bottomView.menu.findItem(R.id.tabProfile)
    profilesItem.isVisible = profilesVisible
    profilesItem.isEnabled = profilesVisible

    this.navigator =
      TabbedNavigator.create(
        fragment = this,
        fragmentContainerId = R.id.tabbedFragmentHolder,
        navigationView = this.bottomView,
        accountProviders = viewModel.accountProviders,
        profilesController = viewModel.profilesController,
        settingsConfiguration = viewModel.buildConfig,
      )

    lifecycle.addObserver(
      MainFragmentListenerDelegate(
        fragment = this,
        listenerRepository = listenerRepo,
        listener = listener,
        profilesController = viewModel.profilesController,
        settingsConfiguration = viewModel.buildConfig,
        navigator = this.navigator
      )
    )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        this.navigator.popToRoot()
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    viewModel.accountEvents
      .subscribe(this::onAccountEvent)
      .let { subscriptions.add(it) }

    viewModel.profileEvents
      .subscribe(this::onProfileEvent)
      .let { subscriptions.add(it) }

    viewModel.registryEvents
      .subscribe(this::onBookStatusEvent)
      .let { subscriptions.add(it) }

    /*
     * Show the Toolbar
     */

    this.supportActionBar?.show()
  }

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventUpdated -> {
        this.checkForAnnouncements()
      }

      /*
       * We don't know which fragments on the backstack might refer to accounts that
       * have been deleted so we need to clear the history when an account is deleted.
       * It would be better if we could eliminate specific items from the history, but
       * this is Android...
       */

      is AccountEventDeletion.AccountEventDeletionSucceeded -> {
        try {
          this.navigator.clearHistory()
        } catch (e: Throwable) {
          this.logger.error("could not clear history: ", e)
        }
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileUpdated.Succeeded ->
        this.onProfileUpdateSucceeded(event)
      is ProfileIdleTimeOutSoon ->
        this.showTimeOutSoonDialog()
      is ProfileIdleTimedOut ->
        this.onIdleTimedOut()
    }
  }

  private fun setShowHoldsVisibility() {
    val showHolds = viewModel.showHoldsTab
    val holdsItem = this.bottomView.menu.findItem(R.id.tabHolds)
    holdsItem.isVisible = showHolds
    holdsItem.isEnabled = showHolds
  }

  private fun onProfileUpdateSucceeded(event: ProfileUpdated.Succeeded) {
    val oldAccountId = event.oldDescription.preferences.mostRecentAccount
    val newAccountId = event.newDescription.preferences.mostRecentAccount
    this.logger.debug("oldAccountId={}, newAccountId={}", oldAccountId, newAccountId)

    // Reload the catalog feed, the patron's account preference has changed

    if (oldAccountId != newAccountId) {
      this.navigator.clearHistory()
    }

    this.checkForAnnouncements()
    this.setShowHoldsVisibility()
  }

  private fun onIdleTimedOut() {
    this.timeOutDialog?.dismiss()
    this.listener.post(MainFragmentEvent.ProfileIdleTimedOut)
  }

  private fun showTimeOutSoonDialog() {
    val dialog = ProfileDialogs.createTimeOutDialog(this.requireContext())
    this.timeOutDialog = dialog
    dialog.setOnDismissListener {
      this.viewModel.profilesController.profileIdleTimer().reset()
      this.timeOutDialog = null
    }
    dialog.show()
  }

  private fun onBookStatusEvent(event: BookStatusEvent) {
    when (val status = event.statusNow) {
      is BookStatus.DownloadWaitingForExternalAuthentication -> {
        this.openBookDownloadLogin(status.id, status.downloadURI)
      }
    }
  }

  private fun checkForAnnouncements() {
    val currentProfile = this.viewModel.profilesController.profileCurrent()
    val mostRecentAccountId = currentProfile.preferences().mostRecentAccount
    val mostRecentAccount = currentProfile.account(mostRecentAccountId)
    val acknowledged =
      mostRecentAccount.preferences.announcementsAcknowledged.toSet()
    val notYetAcknowledged =
      mostRecentAccount.provider.announcements.filter { !acknowledged.contains(it.id) }

    if (notYetAcknowledged.isNotEmpty() &&
      this.childFragmentManager.findFragmentByTag(ANNOUNCEMENT_DIALOG_TAG) == null
    ) {
      this.showAnnouncementsDialog()
    }
  }

  private fun showAnnouncementsDialog() {
    AnnouncementsDialog().showNow(this.childFragmentManager, ANNOUNCEMENT_DIALOG_TAG)
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

  override fun onStop() {
    super.onStop()
    this.subscriptions.clear()
  }

  override fun onDestroy() {
    super.onDestroy()

    when (viewModel.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        viewModel.profilesController.profileIdleTimer().stop()
      }
    }
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return this.defaultViewModelFactory
  }
}
