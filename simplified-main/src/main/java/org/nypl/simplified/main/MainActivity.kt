package org.nypl.simplified.main

import android.app.ActionBar
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.lifecycle.ViewModelProvider
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.nypl.simplified.ui.catalog.AgeGateDialog
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.splash.SplashListenerType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class MainActivity :
  AppCompatActivity(),
  OnBackStackChangedListener,
  SplashListenerType,
  ErrorPageListenerType,
  AgeGateDialog.BirthYearSelectedListener {

  companion object {
    private const val STATE_ACTION_BAR_IS_SHOWING = "ACTION_BAR_IS_SHOWING"
  }

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private lateinit var mainViewModel: MainActivityViewModel
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesNavigationController: ProfilesNavigationController
  private lateinit var startupNavigationController: StartupNavigationController
  private lateinit var onboardingNavigationController: OnboardingNavigationController
  private lateinit var configurationService: BuildConfigurationServiceType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var ageGateDialog: AgeGateDialog

  private val navigationController: NavigationControllerType?
    get() {
      val controllers = arrayListOf(
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          CatalogNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          AccountNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          SettingsNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          ProfilesNavigationControllerType::class.java
        )
      )
      return controllers.filterNotNull().firstOrNull()
    }

  private fun onStartupFinished() {
    this.logger.debug("onStartupFinished")

    val services =
      Services.serviceDirectory()
    val profilesController =
      services.requireService(ProfilesControllerType::class.java)
    val accountProviders =
      services.requireService(AccountProviderRegistryType::class.java)
    val splashService = getSplashService()

    this.navigationControllerDirectory.removeNavigationController(
      AccountNavigationControllerType::class.java
    )

    when (profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
        val profile = profilesController.profileCurrent()
        val defaultProvider = accountProviders.defaultProvider

        val hasNonDefaultAccount =
          profile.accounts().values.count { it.provider.id != defaultProvider.id } > 0
        this.logger.debug("hasNonDefaultAccount=$hasNonDefaultAccount")

        val shouldShowLibrarySelectionScreen =
          splashService.shouldShowLibrarySelectionScreen && !profile.preferences().hasSeenLibrarySelectionScreen
        this.logger.debug("shouldShowLibrarySelectionScreen=$shouldShowLibrarySelectionScreen")

        if (!hasNonDefaultAccount && shouldShowLibrarySelectionScreen) {
          this.openLibrarySelectionScreen()
        } else {
          this.openCatalog()
        }
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.startupNavigationController.openProfileSelection()
      }
    }
  }

  private fun openLibrarySelectionScreen() {
    this.onboardingNavigationController.openOnboardingStartScreen()
  }

  private fun openCatalog() {
    // Sanity check; we were seeing some crashes on startup when performing a
    // transaction after the fragment manager had been destroyed.
    if (this.isFinishing || this.supportFragmentManager.isDestroyed) return

    ViewModelProvider(this)
      .get(MainActivityViewModel::class.java)
      .clearHistory = true

    this.startupNavigationController.openMainFragment()

    val services = Services.serviceDirectory()
    this.configurationService =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    if (configurationService.showAgeGateUi &&
      this.profilesController.profileCurrent().preferences().dateOfBirth == null
    ) {
      this.showAgeGate()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate (recreating {})", savedInstanceState != null)
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

    this.navigationControllerDirectory = NavigationControllers.findDirectory(this)
    this.setContentView(R.layout.main_host)

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)
    this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    this.supportActionBar?.setDisplayShowHomeEnabled(true)
    this.supportActionBar?.hide() // Hide toolbar until requested

    this.supportFragmentManager
      .addOnBackStackChangedListener(this)

    this.mainViewModel =
      ViewModelProvider(this)
        .get(MainActivityViewModel::class.java)

    this.profilesNavigationController =
      ProfilesNavigationController(this.supportFragmentManager, this.mainViewModel)
    this.navigationControllerDirectory.updateNavigationController(
      ProfilesNavigationControllerType::class.java, this.profilesNavigationController
    )

    this.startupNavigationController =
      StartupNavigationController(this.supportFragmentManager)
    this.navigationControllerDirectory.updateNavigationController(
      StartupNavigationController::class.java, this.startupNavigationController
    )

    this.onboardingNavigationController =
      OnboardingNavigationController(this.supportFragmentManager, this.supportActionBar)
    this.navigationControllerDirectory.updateNavigationController(
      OnboardingNavigationController::class.java, this.onboardingNavigationController
    )

    if (savedInstanceState == null) {
      this.mainViewModel.clearHistory = true
      this.startupNavigationController.openSplashScreen()
    } else {
      if (savedInstanceState.getBoolean(STATE_ACTION_BAR_IS_SHOWING)) {
        this.supportActionBar?.show()
      } else {
        this.supportActionBar?.hide()
      }
    }

    supportFragmentManager.setFragmentResultListener("", this) { _, _->
      this.onStartupFinished()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_ACTION_BAR_IS_SHOWING, this.supportActionBar?.isShowing ?: false)
  }

  override fun getActionBar(): ActionBar? {
    this.logger.warn("Use 'getSupportActionBar' instead")
    return super.getActionBar()
  }

  override fun onBackPressed() {
    this.navigationController?.let { controller ->
      this.logger.debug("delivering back press to {}", controller::class.simpleName)
      if (!controller.popBackStack()) {
        super.onBackPressed()
      }
      return
    }

    this.logger.debug("delivering back press to activity")
    super.onBackPressed()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        this.navigationController?.let { controller ->
          this.logger.debug("delivering home press to {}", controller::class.simpleName)
          controller.popToRoot()
        } ?: false
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackStackChanged() {
    this.navigationController?.let { controller ->
      val isRoot = (1 == controller.backStackSize())
      this.logger.debug(
        "controller stack size changed [{}, isRoot={}]", controller.backStackSize(), isRoot
      )
      this.supportActionBar?.apply {
        setHomeAsUpIndicator(null)
        setHomeActionContentDescription(null)
        setDisplayHomeAsUpEnabled(!isRoot)
      }
    }
  }

  override fun onSplashLibrarySelectionWanted() {
    val fm = this.supportFragmentManager

    /*
     * Set up a custom navigation controller used by the settings library registry screen. It's
     * only capable of moving to the error page, or popping the back stack.
     */

    this.navigationControllerDirectory.updateNavigationController(
      AccountNavigationControllerType::class.java,
      object : AccountNavigationControllerType {
        override fun popBackStack(): Boolean {
          this@MainActivity.onStartupFinished()
          return true
        }

        override fun popToRoot(): Boolean {
          this@MainActivity.onStartupFinished()
          return true
        }

        override fun backStackSize(): Int {
          // Note: Little hack to get the Toolbar to display correctly.
          return fm.backStackEntryCount + 1
        }

        override fun openSettingsAccount(parameters: AccountFragmentParameters) {
          throw UnreachableCodeException()
        }

        override fun openErrorPage(parameters: ErrorPageParameters) {
          throw UnreachableCodeException()
        }

        override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
          throw UnreachableCodeException()
        }

        override fun openSettingsAccountRegistry() {
          throw UnreachableCodeException()
        }

        override fun openCatalogAfterAuthentication() {
          throw UnreachableCodeException()
        }
      }
    )

    this.onboardingNavigationController.openAccountListRegistry()
  }

  override fun onSplashLibrarySelectionNotWanted() {
    this.openCatalog()
  }

  private fun getSplashService(): BrandingSplashServiceType {
    return ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
      )
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters) {
    Reports.sendReportsDefault(
      context = this,
      address = parameters.emailAddress,
      subject = parameters.subject,
      body = parameters.report
    )
  }

  override fun onNewIntent(intent: Intent) {
    if (Services.isInitialized()) {
      if (this.tryToCompleteOAuthIntent(intent)) {
        return
      }
    }
    super.onNewIntent(intent)
  }

  private fun tryToCompleteOAuthIntent(
    intent: Intent
  ): Boolean {
    this.logger.debug("attempting to parse incoming intent as OAuth token")

    val buildConfiguration =
      Services.serviceDirectory()
        .requireService(BuildConfigurationServiceType::class.java)

    val result = OAuthCallbackIntentParsing.processIntent(
      intent = intent,
      requiredScheme = buildConfiguration.oauthCallbackScheme.scheme,
      parseUri = Uri::parse
    )

    if (result is OAuthParseResult.Failed) {
      this.logger.warn("failed to parse incoming intent: {}", result.message)
      return false
    }

    this.logger.debug("parsed OAuth token")
    val accountId = AccountID((result as OAuthParseResult.Success).accountId)
    val token = result.token

    val profilesController =
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    profilesController.profileAccountLogin(
      OAuthWithIntermediaryComplete(
        accountId = accountId,
        token = token
      )
    )
    return true
  }

  override fun onUserInteraction() {
    super.onUserInteraction()

    /*
     * Each time the user interacts with something onscreen, reset the timer.
     */

    if (Services.isInitialized()) {
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)
        .profileIdleTimer()
        .reset()
    }
  }

  /**
   * Shows age gate for verification
   */
  private fun showAgeGate() {
    ageGateDialog = AgeGateDialog()
    ageGateDialog.show(supportFragmentManager, AgeGateDialog.TAG)
  }

  /**
   * Handle birth year sent back from Age Gate dialog
   */
  override fun onBirthYearSelected(isOver13: Boolean) {
    if (isOver13) {
      this.profilesController.profileUpdate { description ->
        this.synthesizeDateOfBirthDescription(description, 14)
      }
    } else {
      this.profilesController.profileUpdate { description ->
        this.synthesizeDateOfBirthDescription(description, 0)
      }
    }
  }

  private fun synthesizeDateOfBirthDescription(
    description: ProfileDescription,
    years: Int
  ): ProfileDescription {
    val newPreferences =
      description.preferences.copy(dateOfBirth = this.synthesizeDateOfBirth(years))
    return description.copy(preferences = newPreferences)
  }

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth {
    return ProfileDateOfBirth(
      date = DateTime.now().minusYears(years),
      isSynthesized = true
    )
  }
}
