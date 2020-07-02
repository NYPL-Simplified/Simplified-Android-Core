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
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.catalog.CatalogConfigurationServiceType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.profiles.ProfileDialogs
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.settings.SettingsConfigurationServiceType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * The main application fragment.
 *
 * Currently, this displays a tabbed view and also displays dialogs on various application
 * events.
 */

class MainFragment : Fragment() {

  private lateinit var bottomNavigator: TabbedNavigationController
  private lateinit var bottomView: BottomNavigationView
  private lateinit var catalogConfig: CatalogConfigurationServiceType
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var settingsConfiguration: SettingsConfigurationServiceType
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

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.settingsConfiguration =
      services.requireService(SettingsConfigurationServiceType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.catalogConfig =
      services.requireService(CatalogConfigurationServiceType::class.java)
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

    this.viewModel =
      ViewModelProviders.of(this.requireActivity())
        .get(MainFragmentViewModel::class.java)

    this.uiThread.runOnUIThread {
      this.bottomNavigator =
        TabbedNavigationController.create(
          activity = this.requireActivity(),
          profilesController = this.profilesController,
          settingsConfiguration = this.settingsConfiguration,
          fragmentContainerId = R.id.tabbedFragmentHolder,
          navigationView = this.bottomView
        )

      if (this.viewModel.clearHistory) {
        this.bottomNavigator.clearHistory()
        this.viewModel.clearHistory = false
      }
    }

    /*
     * Hide various tabs based on build configuration and other settings.
     */

    val holdsItem = this.bottomView.menu.findItem(R.id.tabHolds)
    holdsItem.isVisible = this.catalogConfig.showHoldsTab
    holdsItem.isEnabled = this.catalogConfig.showHoldsTab

    val settingsItem = this.bottomView.menu.findItem(R.id.tabSettings)
    settingsItem.isVisible = this.catalogConfig.showSettingsTab
    settingsItem.isEnabled = this.catalogConfig.showSettingsTab

    val profilesVisible =
      this.profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_DISABLED

    val profilesItem = this.bottomView.menu.findItem(R.id.tabProfile)
    profilesItem.isVisible = profilesVisible
    profilesItem.isEnabled = profilesVisible
    return layout
  }

  override fun onStart() {
    super.onStart()

    val toolbar = (this.requireActivity() as ToolbarHostType).findToolbar()
    toolbar.visibility = View.VISIBLE

    this.uiThread.runOnUIThread {
      this.navigationControllerDirectory.updateNavigationController(
        CatalogNavigationControllerType::class.java, this.bottomNavigator)
      this.navigationControllerDirectory.updateNavigationController(
        AccountNavigationControllerType::class.java, this.bottomNavigator)
      this.navigationControllerDirectory.updateNavigationController(
        SettingsNavigationControllerType::class.java, this.bottomNavigator)
      this.navigationControllerDirectory.updateNavigationController(
        NavigationControllerType::class.java, this.bottomNavigator)
    }

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    /*
     * If named profiles are enabled, subscribe to profile timer events so that users are
     * logged out after a period of inactivity.
     */

    when (this.profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.profileSubscription =
          this.profilesController.profileEvents()
            .subscribe(this::onProfileEvent)

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
      is ProfileIdleTimeOutSoon ->
        this.uiThread.runOnUIThread {
          this.showTimeOutSoonDialog()
        }
      is ProfileIdleTimedOut ->
        this.uiThread.runOnUIThread {
          this.onIdleTimedOut()
        }
      else -> {
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
      CatalogNavigationControllerType::class.java)
    this.navigationControllerDirectory.removeNavigationController(
      AccountNavigationControllerType::class.java)
    this.navigationControllerDirectory.removeNavigationController(
      SettingsNavigationControllerType::class.java)
    this.navigationControllerDirectory.removeNavigationController(
      NavigationControllerType::class.java)
  }
}
