package org.nypl.simplified.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.profiles.ProfileDialogs
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * The main application fragment.
 *
 * Currently, this displays a tabbed view and also displays dialogs on various application
 * events.
 */

class MainFragment : Fragment() {

  private lateinit var bottomNavigator: TabbedNavigationController
  private lateinit var bottomView: BottomNavigationView
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var accountProviders: AccountProviderRegistryType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var viewModel: MainFragmentViewModel
  private val logger = LoggerFactory.getLogger(MainFragment::class.java)
  private var accountSubscription: Disposable? = null
  private var profileSubscription: Disposable? = null
  private var timeOutDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.navigationControllerDirectory =
      NavigationControllers.findDirectory(this.requireActivity())

    val services =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)

    this.accountProviders =
      services.requireService(AccountProviderRegistryType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
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

    this.viewModel =
      ViewModelProviders.of(this.requireActivity())
        .get(MainFragmentViewModel::class.java)

    /*
     * Hide various tabs based on build configuration and other settings.
     */

    val holdsItem = this.bottomView.menu.findItem(R.id.tabHolds)
    holdsItem.isVisible = this.buildConfig.showHoldsTab
    holdsItem.isEnabled = this.buildConfig.showHoldsTab

    val settingsItem = this.bottomView.menu.findItem(R.id.tabSettings)
    settingsItem.isVisible = this.buildConfig.showSettingsTab
    settingsItem.isEnabled = this.buildConfig.showSettingsTab

    val profilesVisible =
      this.profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_DISABLED

    val profilesItem = this.bottomView.menu.findItem(R.id.tabProfile)
    profilesItem.isVisible = profilesVisible
    profilesItem.isEnabled = profilesVisible
    return layout
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

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

    this.uiThread.runOnUIThread {
      this.bottomNavigator =
        TabbedNavigationController.create(
          activity = this.requireActivity(),
          accountProviders = this.accountProviders,
          profilesController = this.profilesController,
          settingsConfiguration = this.buildConfig,
          fragmentContainerId = R.id.tabbedFragmentHolder,
          navigationView = this.bottomView
        )

      if (this.viewModel.clearHistory) {
        this.bottomNavigator.clearHistory()
        this.viewModel.clearHistory = false
      }
    }
  }

  override fun onStart() {
    super.onStart()

    val toolbar = (this.requireActivity() as ToolbarHostType).findToolbar()
    toolbar.visibility = View.VISIBLE

    this.uiThread.runOnUIThread {
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

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    this.profileSubscription =
      this.profilesController.profileEvents()
        .subscribe(this::onProfileEvent)

    /*
     * If named profiles are enabled, subscribe to profile timer events so that users are
     * logged out after a period of inactivity.
     */

    when (this.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {}
      ANONYMOUS_PROFILE_DISABLED -> {
        this.profilesController.profileIdleTimer().start()
      }
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
        this.uiThread.runOnUIThread {
          try {
            this.bottomNavigator.clearHistory()
          } catch (e: Throwable) {
            this.logger.error("could not clear history: ", e)
          }
        }
      }

      else -> {
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileUpdated.Succeeded ->
        this.uiThread.runOnUIThread {
          this.onProfileUpdateSucceeded(event)
        }
      is ProfileIdleTimeOutSoon ->
        this.uiThread.runOnUIThread {
          this.showTimeOutSoonDialog()
        }
      is ProfileIdleTimedOut ->
        this.uiThread.runOnUIThread {
          this.onIdleTimedOut()
        }
      else -> {}
    }
  }

  @UiThread
  private fun onProfileUpdateSucceeded(event: ProfileUpdated.Succeeded) {
    val oldAccountId = event.oldDescription.preferences.mostRecentAccount
    val newAccountId = event.newDescription.preferences.mostRecentAccount
    this.logger.debug("oldAccountId={}, newAccountId={}", oldAccountId, newAccountId)

    // Reload the catalog feed, the patron's account preference has changed

    if (oldAccountId != newAccountId) {
      newAccountId?.let { id ->
        val profile = this.profilesController.profileCurrent()
        val account = profile.account(id)
        val age = profile.preferences().dateOfBirth?.yearsOld(DateTime.now()) ?: 1
        this.bottomNavigator.clearHistory()
        this.bottomNavigator.popBackStack()
        this.bottomNavigator.openFeed(
          CatalogFeedArguments.CatalogFeedArgumentsRemote(
            title = account.provider.displayName,
            ownership = CatalogFeedOwnership.OwnedByAccount(id),
            feedURI = account.provider.catalogURIForAge(age),
            isSearchResults = false
          )
        )
      }
    }
  }

  @UiThread
  private fun onIdleTimedOut() {
    this.uiThread.checkIsUIThread()

    this.timeOutDialog?.dismiss()
    NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
      .openProfileSelect()
  }

  @UiThread
  private fun showTimeOutSoonDialog() {
    this.uiThread.checkIsUIThread()

    val dialog = ProfileDialogs.createTimeOutDialog(this.requireContext())
    this.timeOutDialog = dialog
    dialog.setOnDismissListener {
      this.profilesController.profileIdleTimer().reset()
      this.timeOutDialog = null
    }
    dialog.show()
  }

  override fun onStop() {
    super.onStop()

    when (this.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.profilesController.profileIdleTimer().stop()
      }
    }

    this.profileSubscription?.dispose()
    this.accountSubscription?.dispose()

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
  }
}
