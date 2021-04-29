package org.nypl.simplified.main

import android.app.ActionBar
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationAwareViewModelFactory
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.navigation.api.navViewModels
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class MainActivity :
  AppCompatActivity(R.layout.main_host),
  FragmentResultListener {

  companion object {
    private const val STATE_ACTION_BAR_IS_SHOWING = "ACTION_BAR_IS_SHOWING"
    private const val SPLASH_RESULT_KEY = "SPLASH_RESULT"
    private const val ONBOARDING_RESULT_KEY = "ONBOARDING_RESULT"
  }

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)
  private val navViewModel: NavigationViewModel<MainActivityNavigationCommand> by navViewModels()
  private lateinit var mainViewModel: MainActivityViewModel
  private lateinit var navigationDelegate: MainActivityNavigationDelegate
  private lateinit var configurationService: BuildConfigurationServiceType
  private lateinit var profilesController: ProfilesControllerType

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate (recreating {})", savedInstanceState != null)
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)
    this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    this.supportActionBar?.setDisplayShowHomeEnabled(true)
    this.supportActionBar?.hide() // Hide toolbar until requested

    this.mainViewModel =
      ViewModelProvider(this)
        .get(MainActivityViewModel::class.java)

    this.navigationDelegate =
      MainActivityNavigationDelegate(navViewModel, supportFragmentManager, this.mainViewModel)

    if (savedInstanceState == null) {
      this.mainViewModel.clearHistory = true
      this.navigationDelegate.openSplashScreen(SPLASH_RESULT_KEY)
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

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return NavigationAwareViewModelFactory(
      MainActivityNavigationViewModel::class.java,
      super.getDefaultViewModelProviderFactory()
    )
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
          this.navigationDelegate.openOnboarding(ONBOARDING_RESULT_KEY)
        } else {
          this.onOnboardingFinished()
        }
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.navigationDelegate.openProfileSelection()
      }
    }
  }

  private fun onOnboardingFinished() {
    ViewModelProvider(this)
      .get(MainActivityViewModel::class.java)
      .clearHistory = true

    this.navigationDelegate.openMainFragment()

    val services = Services.serviceDirectory()
    this.configurationService =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
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
}
