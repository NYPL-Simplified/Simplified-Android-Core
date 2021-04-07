package org.nypl.simplified.main

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.profiles.ProfileDialogs
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.slf4j.LoggerFactory

/**
 * The main application fragment.
 *
 * Currently, this displays a tabbed view and also displays dialogs on various application
 * events.
 */

class MainFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(MainFragment::class.java)

  private lateinit var bottomNavigator: TabbedNavigationController
  private lateinit var bottomView: BottomNavigationView
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private var timeOutDialog: AlertDialog? = null

  private lateinit var activityViewModel: MainActivityViewModel
  private lateinit var viewModel: MainFragmentViewModel
  private val subscriptions = CompositeDisposable()

  private val handler: Handler = Handler(Looper.getMainLooper())
  private val delayedRunnables: MutableList<Runnable> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.navigationControllerDirectory =
      NavigationControllers.findDirectory(this.requireActivity())

    this.activityViewModel =
      ViewModelProvider(this.requireActivity())
        .get(MainActivityViewModel::class.java)

    this.viewModel =
      ViewModelProvider(this)
        .get(MainFragmentViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.main_tabbed_host, container, false)

    this.bottomView =
      layout.findViewById(R.id.bottomNavigator)

    /*
     * Hide various tabs based on build configuration and other settings.
     */

    val holdsItem = this.bottomView.menu.findItem(R.id.tabHolds)
    holdsItem.isVisible = viewModel.buildConfig.showHoldsTab
    holdsItem.isEnabled = viewModel.buildConfig.showHoldsTab

    val settingsItem = this.bottomView.menu.findItem(R.id.tabSettings)
    settingsItem.isVisible = viewModel.buildConfig.showSettingsTab
    settingsItem.isEnabled = viewModel.buildConfig.showSettingsTab

    val profilesVisible =
      viewModel.profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_DISABLED

    val profilesItem = this.bottomView.menu.findItem(R.id.tabProfile)
    profilesItem.isVisible = profilesVisible
    profilesItem.isEnabled = profilesVisible
    return layout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    /*
     * This extremely unfortunate workaround (delaying the creation of the navigator by scheduling
     * the creation on the UI thread) is necessary because the bottom navigator
     * eagerly instantiates fragments and there's nothing we can do to stop it doing so.
     * The actual issue this avoids is documented here:
     *
     * https://github.com/PandoraMedia/BottomNavigator/issues/13
     *
     * In other words, the current onStart method is currently executing in the middle of
     * a fragment transaction, and the bottom navigator will _immediately_ try to start
     * executing more transactions (leading to an exception). By deferring creation of
     * the navigator here, we avoid this issue, but this does mean that code executing
     * in the onStart() methods of fragments within the tabs will not have guaranteed
     * access to a navigation controller.
     *
     * Additionally, because the BottomNavigator stores references to fragments and reuses
     * them instead of instantiating them anew, it's necessary to aggressively clear the
     * "root fragments" in case any of them are holding references to stale data such as
     * profile and account identifiers. We use a view model to store a "clear history" flag
     * so that we can avoid clearing the history due to device orientation changes and app
     * foreground/background switches.
     */

    this.runOnUIThread {
      this.bottomNavigator =
        TabbedNavigationController.create(
          activity = this.requireActivity(),
          accountProviders = viewModel.accountProviders,
          profilesController = viewModel.profilesController,
          settingsConfiguration = viewModel.buildConfig,
          fragmentContainerId = R.id.tabbedFragmentHolder,
          navigationView = this.bottomView
        )

      if (this.activityViewModel.clearHistory) {
        this.bottomNavigator.clearHistory()
        this.activityViewModel.clearHistory = false
      }

      this.navigationControllerDirectory.updateNavigationController(
        CatalogNavigationControllerType::class.java, this.bottomNavigator
      )
      this.navigationControllerDirectory.updateNavigationController(
        AccountNavigationControllerType::class.java, this.bottomNavigator
      )
      this.navigationControllerDirectory.updateNavigationController(
        SettingsNavigationControllerType::class.java, this.bottomNavigator
      )
      this.navigationControllerDirectory.updateNavigationController(
        NavigationControllerType::class.java, this.bottomNavigator
      )
    }
  }

  override fun onResume() {
    super.onResume()

    this.runOnUIThread {
      viewModel.accountEvents
        .subscribe(this::onAccountEvent)
        .let { subscriptions.add(it) }

        viewModel.profileEvents
          .subscribe(this::onProfileEvent)
          .let { subscriptions.add(it) }

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
     * Show the Toolbar
     */
      this.supportActionBar?.show()
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      /*
       * We don't know which fragments on the backstack might refer to accounts that
       * have been deleted so we need to clear the history when an account is deleted.
       * It would be better if we could eliminate specific items from the history, but
       * this is Android...
       */

      is AccountEventDeletion.AccountEventDeletionSucceeded -> {
        try {
          this.bottomNavigator.clearHistory()
        } catch (e: Throwable) {
          this.logger.error("could not clear history: ", e)
        }
      }

      else -> {
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileUpdated.Succeeded ->
        this.onProfileUpdateSucceeded(event)
      is ProfileIdleTimeOutSoon ->
        this.showTimeOutSoonDialog()
      is ProfileIdleTimedOut ->
        this.onIdleTimedOut()
      else -> {}
    }
  }

  private fun onProfileUpdateSucceeded(event: ProfileUpdated.Succeeded) {
    val oldAccountId = event.oldDescription.preferences.mostRecentAccount
    val newAccountId = event.newDescription.preferences.mostRecentAccount
    this.logger.debug("oldAccountId={}, newAccountId={}", oldAccountId, newAccountId)

    // Reload the catalog feed, the patron's account preference has changed
    // Or if the user's age has changed

    if (oldAccountId != newAccountId ||
      event.oldDescription.preferences.dateOfBirth != event.newDescription.preferences.dateOfBirth
    ) {
      newAccountId?.let { id ->
        val profile = viewModel.profilesController.profileCurrent()
        val account = profile.account(id)
        val age = profile.preferences().dateOfBirth?.yearsOld(DateTime.now()) ?: 1
        this.bottomNavigator.clearHistory()
        this.bottomNavigator.popBackStack()
        this.bottomNavigator.openFeed(
          CatalogFeedArguments.CatalogFeedArgumentsRemote(
            title = getString(R.string.tabCatalog),
            ownership = CatalogFeedOwnership.OwnedByAccount(id),
            feedURI = account.catalogURIForAge(age),
            isSearchResults = false
          )
        )
      }
    }
  }

  private fun onIdleTimedOut() {
    this.timeOutDialog?.dismiss()
    NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
      .openProfileSelect()
  }

  private fun showTimeOutSoonDialog() {
    val dialog = ProfileDialogs.createTimeOutDialog(this.requireContext())
    this.timeOutDialog = dialog
    dialog.setOnDismissListener {
      viewModel.profilesController.profileIdleTimer().reset()
      this.timeOutDialog = null
    }
    dialog.show()
  }

  override fun onPause() {
    super.onPause()
    clearDelayedRunnables()

    when (viewModel.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        viewModel.profilesController.profileIdleTimer().stop()
      }
    }

    this.navigationControllerDirectory.removeNavigationController(
      CatalogNavigationControllerType::class.java
    )
    this.navigationControllerDirectory.removeNavigationController(
      AccountNavigationControllerType::class.java
    )
    this.navigationControllerDirectory.removeNavigationController(
      SettingsNavigationControllerType::class.java
    )
    this.navigationControllerDirectory.removeNavigationController(
      NavigationControllerType::class.java
    )

    this.subscriptions.clear()
  }

  private fun runOnUIThread(f: () -> Unit) {
    val runnable = Runnable {
      f.invoke()
    }
    handler.post(runnable)
    delayedRunnables.add(runnable)
  }

  private fun clearDelayedRunnables() {
    while (delayedRunnables.isNotEmpty()) {
      val r = delayedRunnables.removeFirst()
      handler.removeCallbacks(r)
    }
  }
}
