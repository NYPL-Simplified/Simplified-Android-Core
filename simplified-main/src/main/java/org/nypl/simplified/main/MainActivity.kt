package org.nypl.simplified.main

import android.app.ActionBar
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.nypl.simplified.ui.catalog.AgeGateDialog
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class MainActivity :
  AppCompatActivity(R.layout.main_host),
  FragmentResultListener,
  AgeGateDialog.BirthYearSelectedListener {

  companion object {
    private const val STATE_ACTION_BAR_IS_SHOWING = "ACTION_BAR_IS_SHOWING"
    private const val SPLASH_RESULT_KEY = "SPLASH_RESULT"
    private const val ONBOARDING_RESULT_KEY = "ONBOARDING_RESULT"
  }

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private lateinit var mainViewModel: MainActivityViewModel
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesNavigationController: ProfilesNavigationController
  private lateinit var startupNavigationController: StartupNavigationController
  private lateinit var configurationService: BuildConfigurationServiceType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var ageGateDialog: AgeGateDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate (recreating {})", savedInstanceState != null)
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

    this.navigationControllerDirectory = NavigationControllers.findDirectory(this)

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)
    this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    this.supportActionBar?.setDisplayShowHomeEnabled(true)
    this.supportActionBar?.hide() // Hide toolbar until requested

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

    if (savedInstanceState == null) {
      this.mainViewModel.clearHistory = true
      this.startupNavigationController.openSplashScreen(SPLASH_RESULT_KEY)
    } else {
      if (savedInstanceState.getBoolean(STATE_ACTION_BAR_IS_SHOWING)) {
        this.supportActionBar?.show()
      } else {
        this.supportActionBar?.hide()
      }
    }

    supportFragmentManager.setFragmentResultListener(SPLASH_RESULT_KEY, this, this)
    supportFragmentManager.setFragmentResultListener(ONBOARDING_RESULT_KEY, this, this)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_ACTION_BAR_IS_SHOWING, this.supportActionBar?.isShowing ?: false)
  }

  override fun getActionBar(): ActionBar? {
    this.logger.warn("Use 'getSupportActionBar' instead")
    return super.getActionBar()
  }

  override fun onFragmentResult(requestKey: String, result: Bundle) {
    when (requestKey) {
      SPLASH_RESULT_KEY -> onSplashFinished()
      ONBOARDING_RESULT_KEY -> onOnboardingFinished()
    }
  }

  private fun onSplashFinished() {
    this.logger.debug("onSplashFinished")

    val services =
      Services.serviceDirectory()
    val profilesController =
      services.requireService(ProfilesControllerType::class.java)
    val accountProviders =
      services.requireService(AccountProviderRegistryType::class.java)
    val splashService = getSplashService()

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
          this.startupNavigationController.openOnboarding(ONBOARDING_RESULT_KEY)
        } else {
          this.onOnboardingFinished()
        }
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.startupNavigationController.openProfileSelection()
      }
    }
  }

  private fun onOnboardingFinished() {
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

  private fun getSplashService(): BrandingSplashServiceType {
    return ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
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
